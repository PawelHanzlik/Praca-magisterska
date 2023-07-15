package pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import com.google.inject.Inject;

import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils.WeightedRandomBag;
//polaczenie ze Sparkiem
@Log4j2
public class ALSPoiRecommender implements IPoiRecommender {

    private final List<Rating> ratings = new LinkedList<>();

    private final AtomicInteger numberOfRatingsToBuildNewModel;

    private final Map<Long, IPoi> storedPois = new HashMap<>();

    private final Map<Long, IProfile> storedProfiles = new HashMap<>();

    private final Map<Long, WeightedRandomBag<Pair<Long, Double>>> latestRecommendations = new HashMap<>();

    private final SparkSession spark;

    private final JavaSparkContext sparkContext;

    private final AtomicLong modelBuildsCount = new AtomicLong(0);

    private final AtomicLong recommendedPoisCount = new AtomicLong(0);

    private final Config parameters;

    //właściwa klasa tworząca rekomendacje z wykorzystaniem algorytmu tworzenia rekomendacji \textit{Alternating Least Squares} (ALS).
    // W tej klasie został zaimplementowany algorytm
    @Inject
    public ALSPoiRecommender(Config parameters) {
        this.numberOfRatingsToBuildNewModel = new AtomicInteger(parameters.getInt(Parameters.BUILD_MODEL_EVERY_N_RATINGS));
        this.spark = SparkSession
            .builder()
            .config(new SparkConf().setAppName(parameters.getAppName()).setMaster("local[2]").set("spark.executor.memory", "2g"))
            .getOrCreate();
        this.sparkContext = new JavaSparkContext(spark.sparkContext());
        this.parameters = parameters;
    }

    //zatrzymywanie kontekstu
    public void stopContext() {
        this.sparkContext.stop();
        log.info("ALS poi recommendation model was build " + modelBuildsCount.get() + " times. Recommended " + recommendedPoisCount.get()
            + " pois.");
    }

    //implementacja algorytmu
    @Override
    public synchronized Collection<Pair<IPoi, Double>> recommend(IProfile profile) {
        final long profileId = numericId(profile.getId());
        final WeightedRandomBag<Pair<Long, Double>> recommendationsBag = latestRecommendations.get(profileId);
        if (recommendationsBag == null || recommendationsBag.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<Pair<IPoi, Double>> pois = new HashSet<>();
        for (int i = 0; i < parameters.getInt(Parameters.MAX_NUMBER_OF_RECOMMENDED_POIS_FOR_TRAJECTORY); i++) {
            final Pair<Long, Double> poi = recommendationsBag.getRandom();
            final IPoi randomRecommendation = storedPois.get(poi.getFirst());
            if (randomRecommendation != null) {
                pois.add(Pair.create(randomRecommendation, poi.getSecond()));
            }
        }
        recommendedPoisCount.addAndGet(pois.size());
        return pois;
    }

    @Override
    public synchronized void addRating(IProfile profile, IPoi poi, double rating) {
        final long profileId = numericId(profile.getId());
        storedProfiles.put(profileId, profile);
        final long poiId = numericId(poi.getId());

        final Rating r = new Rating(profileId, poiId, rating, System.currentTimeMillis());
        ratings.add(r);
        if (ratings.size() > numberOfRatingsToBuildNewModel.get()) {
            numberOfRatingsToBuildNewModel.addAndGet(parameters.getInt(Parameters.BUILD_MODEL_EVERY_N_RATINGS));
            buildModel();
            modelBuildsCount.getAndIncrement();
        }
    }

    private synchronized void buildModel() {
        Dataset<Row> sparkRecommendations = null;
        try {
            sparkContext.setLogLevel("ERROR");
            JavaRDD<Rating> ratingsRDD = sparkContext.parallelize(ratings, 1);

            Dataset<Row> ratings = spark.createDataFrame(ratingsRDD, Rating.class);
            Dataset<Row>[] splits = ratings.randomSplit(new double[] { 0.8, 0.2 });
            Dataset<Row> training = splits[0];
            Dataset<Row> test = splits[1];

            ALS als = new ALS()
                .setMaxIter(5)
                .setRegParam(0.01)
                .setUserCol("userId")
                .setItemCol("poiId")
                .setRatingCol("rating");
            ALSModel model = als.fit(training);

            model.setColdStartStrategy("drop");
            Dataset<Row> predictions = model.transform(test);

            RegressionEvaluator evaluator = new RegressionEvaluator()
                .setMetricName("rmse")
                .setLabelCol("rating")
                .setPredictionCol("prediction");
            Double rmse = evaluator.evaluate(predictions);
            log.info("Root-mean-square error = " + rmse);

            sparkRecommendations = model.recommendForAllUsers(20);
        } catch (Exception e) {
            log.error("Unable to build model for recommendations!" + e);
        }

        if (sparkRecommendations == null) {
            return;
        }

        latestRecommendations.clear();
        addSparkRecommendationsToLatestRecommendations(sparkRecommendations);
    }

    //poprawa literówki w nazwie funkcji
    private void addSparkRecommendationsToLatestRecommendations(Dataset<Row> sparkRecommendations) {
        sparkRecommendations.collectAsList().forEach(row -> {
            final Integer userId = row.getInt(0);
            final WeightedRandomBag<Pair<Long, Double>> recommendationsBag = latestRecommendations.computeIfAbsent(userId.longValue(),
                e -> new WeightedRandomBag<Pair<Long, Double>>());

            final List<Row> recommendations = row.getList(1);
            recommendations.forEach(recommendation -> {
                final Integer poiId = recommendation.getInt(0);
                final float rating = recommendation.getFloat(1);
                recommendationsBag.addEntry(Pair.create(poiId.longValue(), (double)rating), rating);
            });
        });
    }

    @Override
    public void storePoi(IPoi poi) {
        storedPois.put(numericId(poi.getId()), poi);
    }

    public static long numericId(String value) {
        final String digest = DigestUtils.md5Hex(value);
        return new BigInteger(digest, 16).intValue();
    }
}
