package pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

public interface IStatsProvider {

    Map<String, Object> provideStats(Collection<ProfileResult> results);

    default <T> boolean addStat(String name, Optional<T> maybeValue, Map<String, Object> targetMap) {
        maybeValue.ifPresent(v -> targetMap.put(name, v));
        return maybeValue.isPresent();
    }

    default <T> boolean addStat(String name, T value, Map<String, Object> targetMap) {
        return targetMap.put(name, value) == null;
    }

    default boolean addStat(Optional<? extends Map<?, ?>> stat, Map<String, Object> targetMap) {
        if (stat.isPresent()) {
            stat.get().forEach((k, v) -> targetMap.put(String.valueOf(k), v));
            return true;
        }
        return false;
    }

    default Optional<Double> toOptional(OptionalDouble optionalDouble) {
        return optionalDouble.isPresent() ? Optional.of(optionalDouble.getAsDouble()) : Optional.empty();
    }
}
