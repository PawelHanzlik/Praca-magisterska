package pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.Messages;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;

public class PoiStatsProvider implements IStatsProvider {

    @Override
    public Map<String, Object> provideStats(Collection<ProfileResult> results) {
        final Map<String, Object> poiStats = new HashMap<>();

        addStat(Messages.AverageVisitedPois, averageVisitedPois(results), poiStats);
        addStat(Messages.AverageVisitedPoisADay, averageVisitedPoisADay(results), poiStats);
        addStat(Messages.VisitsByCategories, visitsByCategory(results), poiStats);
        addStat(Messages.MostVisitedPois, mostVisitedPois(results), poiStats);
        addStat(Messages.AveragePoisPerHour, averageVisitedPoisAHour(results), poiStats);
        addStat(averageVisitsPerPoiDistinctPois(results), poiStats);

        return poiStats;
    }

    private Optional<Double> averageVisitedPois(Collection<ProfileResult> results) {
        return toOptional(results.stream()
            .map(ProfileResult::getPois)
            .mapToLong(pois -> {
                return pois.stream()
                    .filter(poi -> !IRoutePart.PLACE_OF_DEPARTURE.equals(poi.getPoi().getId()))
                    .count();
            })
            .average());
    }

    private Optional<Double> averageVisitedPoisADay(Collection<ProfileResult> results) {
        final Map<LocalDate, AtomicLong> poisByDay = new HashMap<>();
        results.stream().map(this::visitedPoisADay).forEach(poisByDayForSingleResult -> {
            poisByDayForSingleResult.forEach((day, numberOfPois) -> {
                poisByDay.computeIfAbsent(day, k -> new AtomicLong(0)).addAndGet(numberOfPois.get());
            });
        });
        return toOptional(poisByDay.values().stream().mapToLong(AtomicLong::get).average());
    }

    private Map<LocalDate, AtomicLong> visitedPoisADay(ProfileResult result) {
        final Map<LocalDate, AtomicLong> poisByDay = new HashMap<>();
        if (result.getTrajectory() == null) {
            return Collections.emptyMap();
        }
        final List<Pair<LocalDateTime, IPoi>> poisWithTimestamps = result.getTrajectory().getPoisWithTimestamps();
        poisWithTimestamps.stream()
            .filter(poi -> {
                return !IRoutePart.PLACE_OF_DEPARTURE.equals(poi.getSecond().getId());
            })
            .forEach(poiWithTimestamp -> {
                final LocalDate day = poiWithTimestamp.getFirst().toLocalDate();
                poisByDay.computeIfAbsent(day, k -> new AtomicLong(0)).incrementAndGet();
            });
        return poisByDay;
    }

    private Map<String, Pair<Long, Double>> visitsByCategory(Collection<ProfileResult> results) {
        final Map<String, AtomicLong> visits = new HashMap<>();
        results.forEach(result -> {
            result.getPois().forEach(poi -> {
                final String category = poi.getPoi().getCategory();
                visits.computeIfAbsent(category, k -> new AtomicLong(0)).incrementAndGet();
            });
        });
        final long totalVisits = visits.values().stream()
            .mapToLong(AtomicLong::longValue)
            .sum();

        final Map<String, Pair<Long, Double>> visitsByCategory = visits.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e2.getValue().get(), e1.getValue().get()))
            .limit(10)
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> postprocessVisits(entry.getValue(), totalVisits),
                    (v1, v2) -> {
                        throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                    },
                    TreeMap::new));
        visitsByCategory.put(Messages.AllVisits, Pair.create(totalVisits, 100d));

        return visitsByCategory;
    }

    private Pair<Long, Double> postprocessVisits(AtomicLong numberOfVisits, long totalVisits) {
        final long numberOfVisitsForCategory = numberOfVisits.get();
        return Pair.create(numberOfVisitsForCategory, ((double)numberOfVisitsForCategory) / totalVisits);
    }

    private Map<String, Pair<Long, Double>> mostVisitedPois(Collection<ProfileResult> results) {
        final List<IPoi> allPois = new ArrayList<>();
        final Map<String, AtomicLong> mostVisitedPois = new HashMap<>();
        final AtomicLong totalVisits = new AtomicLong(0);

        results.stream()
            .map(ProfileResult::getPois)
            .flatMap(Collection::stream)
            .map(PoiHolder::getPoi)
            .filter(poi -> !IRoutePart.PLACE_OF_DEPARTURE.equals(poi.getId()))
            .forEach(poi -> {
                allPois.add(poi);
                mostVisitedPois.computeIfAbsent(poi.getId(), k -> new AtomicLong(0)).incrementAndGet();
                totalVisits.incrementAndGet();
            });

        final Map<String, Pair<Long, Double>> top10 = mostVisitedPois.entrySet().stream()
            .sorted(this::compareEntriesByVisitsNumber)
            .limit(10)
            .collect(Collectors.toMap(entry -> resolvePoiName(entry.getKey(), allPois),
                entry -> Pair.create(entry.getValue().longValue(), (entry.getValue().doubleValue() / totalVisits.doubleValue())),
                (v1, v2) -> {
                    throw new RuntimeException(String.format("Duplicate key for values %s and %s", v1, v2));
                },
                TreeMap::new));
        return top10;
    }

    private String resolvePoiName(String id, List<IPoi> allPois) {
        return allPois.stream()
            .filter(poi -> Objects.equals(id, poi.getId()))
            .findFirst()
            .map(poi -> String.format("%s (%f,%f)", poi.getName(), poi.getLat(), poi.getLon()))
            .orElse("Unknown POI");
    }

    private int compareEntriesByVisitsNumber(Map.Entry<String, AtomicLong> e1, Map.Entry<String, AtomicLong> e2) {
        return Long.compare(e2.getValue().get(), e1.getValue().get());
    }

    private Optional<Double> averageVisitedPoisAHour(Collection<ProfileResult> results) {
        return toOptional(results.stream().mapToDouble(this::poisPerHour).average());
    }

    private Double poisPerHour(ProfileResult result) {
        if (result.getTrajectory() == null) {
            return 0d;
        }
        final List<Pair<LocalDateTime, Point>> pointsWithTimestamps = result.getTrajectory().getPointsWithTimestamps();
        if (pointsWithTimestamps.size() < 2) {
            return 0d;
        }

        final LocalDateTime startTime = pointsWithTimestamps.get(0).getFirst();
        final LocalDateTime endTime = pointsWithTimestamps.get(pointsWithTimestamps.size() - 1).getFirst();
        final double hoursInTrajectory = ((double)Duration.between(startTime, endTime).toSeconds()) / 3600;

        //sonarqube sugestia
        //return ((double)result.getPois().size()) / hoursInTrajectory;
        return (result.getPois().size()) / hoursInTrajectory;
    }

    private Optional<ImmutableMap<Object, Number>> averageVisitsPerPoiDistinctPois(Collection<ProfileResult> results) {
        final long allVisits = results.stream()
            .map(ProfileResult::getPois)
            .flatMap(Collection::stream)
            .count();

        final long allDistinctPois = results.stream()
            .map(ProfileResult::getPois)
            .flatMap(Collection::stream)
            .map(PoiHolder::getPoi)
            .filter(distinctByKey(poi -> Arrays.asList(poi.getLat(), poi.getLon(), poi.getName())))
            .count();

        return Optional.of(ImmutableMap.of(Messages.AverageVisitsPerPoi, ((double)allVisits) / allDistinctPois, Messages.AllDistinctPois,
            allDistinctPois));
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
