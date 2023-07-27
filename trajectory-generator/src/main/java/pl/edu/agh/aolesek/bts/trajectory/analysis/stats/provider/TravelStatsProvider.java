package pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider;

import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.Messages;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class TravelStatsProvider implements IStatsProvider {

    @Override
    public Map<String, Object> provideStats(Collection<ProfileResult> results) {
        final Map<String, Object> travelStats = new HashMap<>();
        addStat(Messages.AverageTrajectoryDistance, averageTrajectoryDistance(results), travelStats);

        final Pair<Set<String>, Optional<Double>> averageTransportMethods = averageTransportMethods(results);
        final Set<String> availableTransportMethods = averageTransportMethods.getFirst();
        addStat(Messages.AvailableTransportMethodsNumber, availableTransportMethods, travelStats);
        final Optional<Double> averagePerTrajectory = averageTransportMethods.getSecond();
        addStat(Messages.AverageTransportMethodsNumber, averagePerTrajectory, travelStats);

        return travelStats;
    }

    private Optional<Double> averageTrajectoryDistance(Collection<ProfileResult> results) {
        final OptionalDouble average = results.stream()
            .map(ProfileResult::getTrajectory)
            .filter(Objects::nonNull)
            .map(ITrajectory::getPointsWithTimestamps)
            .mapToDouble(this::resolveDistance)
            .average();
        return average.isPresent() ? Optional.of(average.getAsDouble()) : Optional.empty();
    }

    private Double resolveDistance(List<Pair<LocalDateTime, Point>> trajectory) {
        if (trajectory == null || trajectory.size() < 2) {
            return 0d;
        }

        double totalDistance = 0;
        Point currentPoint = trajectory.get(0).getSecond();
        for (int i = 1; i < trajectory.size(); i++) {
            final Point nextPoint = trajectory.get(i).getSecond();
            double subDistance = GeoUtils.distance(currentPoint.getLat(), currentPoint.getLon(), nextPoint.getLat(), nextPoint.getLon());
            totalDistance += subDistance;
            currentPoint = nextPoint;
        }
        return totalDistance/1000;
    }

    private Pair<Set<String>, Optional<Double>> averageTransportMethods(Collection<ProfileResult> results) {
        final Set<String> allTransportMethods = new HashSet<>();
        final OptionalDouble averageUsedTransportModes = results.stream()
            .filter(presult -> {
                if (presult.getRoutePlan() != null) {
                    return true;
                }

                log.warn("Omitted profile result due to empty plan! " + presult.getProfile().getFullName());
                return false;
            })
            .map(ProfileResult::getRoutePlan)
            .mapToLong(plan -> {
                final Set<String> methodsForPlan = usedTransportMethods(plan);
                allTransportMethods.addAll(methodsForPlan);
                return methodsForPlan.size();
            }).average();
        return Pair.create(allTransportMethods,
            averageUsedTransportModes.isPresent() ? Optional.of(averageUsedTransportModes.getAsDouble()) : Optional.empty());
    }

    private Set<String> usedTransportMethods(IRoutePlan routePlan) {
        return routePlan.getParts().stream()
            .map(IRoutePart::getTransportMode)
            .distinct()
            .collect(Collectors.toSet());
    }
}
