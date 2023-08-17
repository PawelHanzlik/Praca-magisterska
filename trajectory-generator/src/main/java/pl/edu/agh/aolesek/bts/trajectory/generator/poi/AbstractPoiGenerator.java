package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import avro.shaded.com.google.common.collect.Iterables;
import com.google.common.collect.ImmutableMap;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils.WeightedBagCollector;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils.WeightedRandomBag;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.StaticCateroriesEnum;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.ImmutableMap.of;
import static pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters.CATEGORIES_TO_AVOID_MULTIPLE_VISITS;

//klasa abstrakcyjna wykorzystująca interfejs IPoi
public abstract class AbstractPoiGenerator<T extends IPoi> implements IPoiGenerator<T> {

    public static final String ALL_POIS = "all_pois";

    public static final String ATTRACTIVENESS = "attractiveness";

    public static final String INTERESTING = "interesting";

    public static final String AFFORDABLE = "affordable";

    public static final String NEAR = "near";

    private final Config config;

    private final IPoiRecommender recommender;

    public AbstractPoiGenerator(Config generatorParameters, IPoiRecommender recommender) {
        this.config = generatorParameters;
        this.recommender = recommender;
    }

    @Override
    public Collection<PoiHolder<T>> generatePois(IProfile profile) {
        final ProfileLogger pLog = profile.getLogger();
        final int numberOfPois = resolveNumberOfPoisToGenerate(profile);
        final Collection<PoiHolder<T>> allAvailablePois = resolveAllAvailablePois(profile);
        ratePois(allAvailablePois);
        final Collection<PoiHolder<T>> recommendedPois = recommendPois(profile);

        final Collection<PoiHolder<T>> finalPois = resolveFinalPois(allAvailablePois, recommendedPois, numberOfPois, profile);

        pLog.commitOngoingPLog(
            String.format("Completed generating %d POIs for profile %s", finalPois.size(), profile.toString()));
        if (finalPois.size() < numberOfPois) {
            pLog.info("Unable to find sufficient number of pois for this profile!",
                of("generated_pois", finalPois, "desired_number", numberOfPois));
        }

        return finalPois;
    }

    // MARK: Resolving available POIs

    protected int resolveNumberOfPoisToGenerate(IProfile profile) {
        final int averageNumberOfPoisForProfile = profile.getPreferences().getAverageNumberOfPOIs();
        final int numberOfPois = RandomUtils.deviatedByPercentage(averageNumberOfPoisForProfile,
            config.getDouble(Parameters.AVERAGE_NUMBER_OF_VISITED_POIS_DEVIATION));

        profile.getLogger()
            .info(String.format("Decided to generate %d pois with base number %d and max deviation %f", numberOfPois,
                averageNumberOfPoisForProfile, config.getDouble(Parameters.AVERAGE_NUMBER_OF_VISITED_POIS_DEVIATION)),
                of("number_of_pois", numberOfPois));
        return numberOfPois;
    }

    private Collection<PoiHolder<T>> resolveAllAvailablePois(IProfile profile) {
        final Set<String> allCategoriesOfInterest = profile.getInterests().stream()
            .map(Pair::getFirst)
            .collect(Collectors.toSet());

        final List<PoiHolder<T>> allAvailablePois = allCategoriesOfInterest.parallelStream()
            .flatMap(category -> getCategoriesWithPois(category, profile))
            .collect(Collectors.toCollection(ArrayList::new));

        profile.getLogger().info(String.format("Resolved %d available POIs for this profile", allAvailablePois.size()),
            of(ALL_POIS, allAvailablePois.stream().collect(Collectors.toList())));
        profile.getLogger().logParameter(ALL_POIS, allAvailablePois);
        return allAvailablePois;
    }

    private Stream<PoiHolder<T>> getCategoriesWithPois(String category, IProfile profile) {
        final Set<PoiHolder<T>> pois = searchPois(category, profile).stream()
            .map(this::fillAverageSpentTime)
            .peek(poi -> recommender.storePoi(poi))
            .map(poi -> PoiHolder.create(profile, poi, new HashMap<String, Object>()))
            .collect(Collectors.toSet());
        if (pois.isEmpty()) {
            final ImmutableMap<String, Object> properties = ImmutableMap.of("departure", profile.getPlaceOfDeparture(), "range",
                profile.getMaxRange(), "profile", profile);
            profile.getLogger().debug("Unable to find any POIs for category " + category + ", profile " + profile.getFullName(),
                properties);
        }
        return pois.stream();
    }

    protected abstract Collection<T> searchPois(String category, IProfile profile);

    private T fillAverageSpentTime(T poi) {
        poi.setAverageSecondsSpent(resolveAverageSecondsSpent(poi));
        return poi;
    }

    protected abstract long resolveAverageSecondsSpent(T poi);

    // MARK: Rating POIs.

    private void ratePois(Collection<PoiHolder<T>> pois) {
        pois.forEach(poi -> computeAttractiveness(poi));
    }

    private double computeAttractiveness(PoiHolder<?> element) {
        if (element.getReason().isEmpty()) {
            final double interestingRating = resolveInterestingRating(element);
            final double priceRating = resolvePriceRating(element);
            final double distanceRating = resolveDistanceRating(element);

            final IProfile profile = element.getProfile();
            final double attractiveness = profile.rateAttractiveness(interestingRating, priceRating, distanceRating);

            element.getReason()
                .putAll(of("category", element.getPoi().getCategory(), ATTRACTIVENESS, attractiveness, INTERESTING,
                    interestingRating,
                    AFFORDABLE, priceRating, NEAR, distanceRating));

            profile.getLogger().appendOngoingLog(
                String.format(
                    "Rated attractiveness of %s as %f with %f interest rating, %f price rating and %f distance rating",
                    element.getPoi().toString(), attractiveness, interestingRating, priceRating,
                    distanceRating),
                of(ATTRACTIVENESS, attractiveness, INTERESTING, interestingRating, AFFORDABLE, priceRating, NEAR,
                    distanceRating));

            addRatingToRecommender(element, attractiveness, recommender);
        }
        return (double)element.getReason().get(ATTRACTIVENESS);
    }

    protected Double resolveInterestingRating(PoiHolder<?> element) {
        List<Double> interestRatings;
        if (element.getPoi().getCategory().equals("alcohol")) {
            interestRatings = element.getProfile().getInterests().stream()
                    .filter(interest -> Objects.equals(interest.getFirst(), "liquor_store"))
                    .map(Pair::getSecond)
                    .collect(Collectors.toList());
        } else{
            interestRatings = element.getProfile().getInterests().stream()
                    .filter(interest -> Objects.equals(interest.getFirst(), element.getPoi().getCategory()))
                    .map(Pair::getSecond)
                    .collect(Collectors.toList());
        }

        if (interestRatings.size() != 1) {
            throw new IllegalArgumentException("Profile in wrong state - one interest with two ratings!");
        }
        return interestRatings.iterator().next();
    }

    protected double resolvePriceRating(PoiHolder<?> element) {
        final int materialStatusAsInt = element.getProfile().getMaterialStatus().ordinal() + 1;
        final int priceLevelAsInt = resolvePriceLevel(element.getPoi()).ordinal() + 1;
        final double priceRating = ((double)materialStatusAsInt) / priceLevelAsInt;
        return priceRating;
    }

    protected abstract Level resolvePriceLevel(IPoi element);

    protected double resolveDistanceRating(PoiHolder<?> element) {
        final IProfile profile = element.getProfile();
        final double maxRange = profile.getMaxRange();
        final Point placeOfDeparture = profile.getPlaceOfDeparture();
        final double distanceToPoi = GeoUtils.distance(placeOfDeparture.getLat(), placeOfDeparture.getLon(),
            element.getPoi().getLat(), element.getPoi().getLon());

        double distance = distanceToPoi / maxRange;
        if (distance > 1.1) {
            profile.getLogger().warn("POI way too far from starting point, probably external provider ignored max distance.");
        }

        return 1 - distance > 1 ? 1 : distance;
    }

    protected void addRatingToRecommender(PoiHolder<?> element, double attractiveness, IPoiRecommender recommender) {
        recommender.addRating(element.getProfile(), element.getPoi(), attractiveness);
    }

    @SuppressWarnings("unchecked")
    private Collection<PoiHolder<T>> recommendPois(IProfile profile) {
        final Set<PoiHolder<T>> recommendedPois = recommender.recommend(profile).stream()
            .map(poiWithRating -> (PoiHolder<T>)PoiHolder.create(profile, (T)poiWithRating.getFirst(),
                of(ATTRACTIVENESS, poiWithRating.getSecond())))
            .collect(Collectors.toSet());
        profile.getLogger().info(String.format("Recommended %d POIs for this profile. Recommender is %s", recommendedPois.size(),
            recommender.getClass().getSimpleName()));
        return recommendedPois;
    }

    // MARK: Resolving final POIs

    private Collection<PoiHolder<T>> resolveFinalPois(Collection<PoiHolder<T>> availablePois, Collection<PoiHolder<T>> recommendedPois,
        int plannedNumberOfPois, IProfile profile) {
        final Map<String, WeightedRandomBag<PoiHolder<T>>> groupedPois = Stream.concat(availablePois.stream(), recommendedPois.stream())
            .collect(
                Collectors.groupingBy(holder -> holder.getPoi().getCategory(), WeightedBagCollector.create(this::accumulatePoiHolders)));

        final List<PoiHolder<T>> finalPois = new ArrayList<>();
        String category = "";
        String staticCategory = "";
        final List<String> alreadyUsedCategories = new ArrayList<>();
        while (finalPois.size() < plannedNumberOfPois && !areGroupedPoisEmpty(groupedPois)) {
            if (finalPois.isEmpty()){
                category = pickStaticCategory(profile);
                staticCategory = category;
            } else {
                category = pickCategory(groupedPois, profile, staticCategory, alreadyUsedCategories);
            }

            alreadyUsedCategories.add(category);
            final int maxCountOfCategoryElements = config.getStringCollection(CATEGORIES_TO_AVOID_MULTIPLE_VISITS).contains(category)
                ? config.getInt(Parameters.MAX_POIS_WITH_SAME_CATEGORY_WHEN_AVOIDING)
                : config.getInt(Parameters.MAX_POIS_WITH_SAME_CATEGORY);
            if (alreadyHas(finalPois, category) > maxCountOfCategoryElements) {
                groupedPois.get(category).clear(); // this category will not be used for current trajectory
                continue;
            }

            final WeightedRandomBag<PoiHolder<T>> bag = groupedPois.get(category);
            if (bag.isEmpty()) {
                continue;
            }

            final PoiHolder<T> randomPoi = bag.getRandom();
            bag.removeEntry(randomPoi);
            finalPois.add(randomPoi);
        }

        return finalPois;
    }

    private String pickCategory(Map<String, WeightedRandomBag<PoiHolder<T>>> groupedPois, IProfile profile, String staticCategory,
                                List<String> alreadyUsedCategories) {
        List<Pair<String, Double>> accumulatedInterests = new ArrayList<>();
        Double beforeValue = 0.0;
        for (Pair<String, Double> interest : profile.getInterests()){
            accumulatedInterests.add(Pair.create(interest.getFirst(), beforeValue + interest.getSecond()));
            beforeValue = beforeValue + interest.getSecond();
        }
        while (true) {
            double randomValue = new Random().nextDouble();
            for (Pair<String, Double> accumulatedInterest : accumulatedInterests) {
                if (randomValue <= accumulatedInterest.getSecond()) {
                    if (accumulatedInterest.getFirst().equals(staticCategory) || !groupedPois.containsKey(accumulatedInterest.getFirst()) ||
                            alreadyUsedCategories.contains(accumulatedInterest.getFirst())) {
                        break;
                    } else {
                        return accumulatedInterest.getFirst();
                    }
                }
            }
        }
    }

    private void accumulatePoiHolders(WeightedRandomBag<PoiHolder<T>> bag, PoiHolder<T> value) {
        final double attractiveness = (double)value.getReason().get(ATTRACTIVENESS);
        bag.addEntry(value, attractiveness);
    }

    private boolean areGroupedPoisEmpty(Map<String, WeightedRandomBag<PoiHolder<T>>> groupedPois) {
        return groupedPois.values().stream().allMatch(bag -> bag.isEmpty());

    }

    private long alreadyHas(List<PoiHolder<T>> poisForProfile, String category) {
        return poisForProfile.stream()
            .map(PoiHolder<T>::getPoi)
            .filter(poi -> Objects.equals(poi.getCategory(), category))
            .count();
    }

    // MARK: Utils

    protected Pair<Point, Point> resolveBbox(IProfile profile) {
        final Point placeOfDeparture = profile.getPlaceOfDeparture();
        final double maxRangeInMeters = profile.getMaxRange();

        // top left resolved by moving starting point by maxRangeInMeters meters and -45deg bearing
        final Point topLeft = GeoUtils.moveCoordinate(placeOfDeparture.getLat(), placeOfDeparture.getLon(),
            maxRangeInMeters,
            -45);

        // bottom right by moving starting point by maxRangeInMeters meters and 135deg bearing
        final Point bottomRight = GeoUtils.moveCoordinate(placeOfDeparture.getLat(), placeOfDeparture.getLon(),
            maxRangeInMeters,
            135);

        return Pair.create(topLeft, bottomRight);
    }

    private String  pickStaticCategory (IProfile profile){
        String profileName = profile.getFullName();
        if (profileName != null) {
            if (profileName.contains("AdultNight")){
                return StaticCateroriesEnum.SHOP.label;
            } else if (profileName.contains("Teenager") && !profileName.contains("TeenagerNight")) {
                return StaticCateroriesEnum.SCHOOL.label;
            } else if (profileName.contains("Student") && !profileName.contains("StudentNight")){
                return StaticCateroriesEnum.UNIVERSITY.label;
            } else if (profileName.contains("Adult") || profileName.equals("StudentNight2")){
                return StaticCateroriesEnum.COMPANY.label;
            } else {
                return StaticCateroriesEnum.HOUSE.label;
            }
        }
        return StaticCateroriesEnum.SHOP.label;
    }
}
