package pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.AbstractPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPricesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.ISpentTimeProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.model.OsmPoiModel;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//klasa tworząca zapytania do Overpassa.
// W jej ramach wywoływane są zapytania, ustalany jest format w jakim chcemy uzyskać odpowiedź na zapytanie, tworzone są informacje
// dla loggera, wyznaczane są ceny i średni czas jaki ma być spędzony w tym Poi
@Log4j2
public class OsmPoiGenerator extends AbstractPoiGenerator<OsmPoiAdapter> {

    private final OverpassApi api;

    private AtomicInteger requestsCount = new AtomicInteger(0);

    private final ISpentTimeProvider spentTimeProvider;

    private final IPricesProvider pricesProvider;

    //konstruktor
    @Inject
    public OsmPoiGenerator(Config parameters, ISpentTimeProvider spentTimeProvider, IPricesProvider pricesProvider,
        IPoiRecommender recommender) {
        super(parameters, recommender);
        this.spentTimeProvider = spentTimeProvider;
        this.pricesProvider = pricesProvider;
        api = RetrofitClientFactory.getRetrofitInstance(parameters.get(Parameters.OVERPASS_URL)).create(OverpassApi.class);
    }

    //informacje dla loggera
    @Override
    public void logStats() {
        log.info(String.format("Sent %d Overpass API requests.", requestsCount.get()));
    }

    //wyszukiwanie punktu i informacja dla loggera
    @Override
    protected Collection<OsmPoiAdapter> searchPois(String category, IProfile profile) {
        requestsCount.getAndIncrement();
        if (category.equals("liquor_store")){
            category = "alcohol";
        }
        String overpassQuery;
        if (category.equals("house")){
            overpassQuery = createOverpasQueryForSearchByTagForHouse(bBoxAsString(profile), category);
        } else {
            overpassQuery = createOverpasQueryForSearchByTag(bBoxAsString(profile), category);
        }
        profile.getLogger().debug(String.format("Overpass query for %s, category %s: %s", profile.getFullName(), category, overpassQuery),
            ImmutableMap.of("query", overpassQuery));
        String finalCategory = category;
        return api.searchPois(overpassQuery)
            .map(OsmPoiModel::getElements)
            .onErrorReturn(throwable -> {
                profile.getLogger().warn("An error occured while searching POIs.", throwable);
                return null;
            })
            .blockingGet()
            .stream()
            .map(osmModel -> new OsmPoiAdapter(osmModel, finalCategory))
            .collect(Collectors.toList());
    }

    //oczekiwany format odpowiedzi na zapytanie
    private String createOverpasQueryForSearchByTag(String bbox, String phrase) {
        return String.format("[out:json];node(%s)[~\".*\"~\"%s\"];out;", bbox, phrase);
    }

    private String createOverpasQueryForSearchByTagForHouse(String bbox, String phrase) {
        return String.format("[out:json];node(%s)[~\"addr:housenumber\"~\"1\"];out;", bbox, phrase);
    }

    private String bBoxAsString(IProfile profile) {
        final Pair<Point, Point> bbox = super.resolveBbox(profile);
        final Point topLeft = bbox.getFirst();
        final Point bottomRight = bbox.getSecond();

        // south, west, north, east
        return String.format(Locale.ROOT, "%f,%f,%f,%f", bottomRight.getLat(), topLeft.getLon(), topLeft.getLat(), bottomRight.getLon());
    }

    //wyznaczanie średniego czasu spędzonego w obiekcie
    @Override
    protected long resolveAverageSecondsSpent(OsmPoiAdapter poi) {
        return spentTimeProvider.resolveSecondsSpent(poi);
    }

    //wyznaczanie poziomu cenowego
    @Override
    protected Level resolvePriceLevel(IPoi element) {
        return pricesProvider.resolvePriceLevel(element);
    }
}
