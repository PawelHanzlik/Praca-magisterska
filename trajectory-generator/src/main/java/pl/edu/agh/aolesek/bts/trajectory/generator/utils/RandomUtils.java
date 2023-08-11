package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

//wyznaczanie losowych wartości
public class RandomUtils {

    private static final Random RND = new Random();

    private RandomUtils() {
    }

    public static int deviated(int baseNumber, int maxDeviation) {
        return (int)deviated((double)baseNumber, (double)maxDeviation);
    }

    public static long deviated(long baseNumber, long maxDeviation) {
        return (long)deviated((double)baseNumber, (double)maxDeviation);
    }

    public static double deviated(double baseNumber, double maxDeviation) {
        final double chr = RND.nextBoolean() ? 1 : -1;
        final double diff = chr * RND.nextDouble() * maxDeviation;
        return baseNumber + diff;
    }

    public static double deviatedPositive(double baseNumber, double maxDeviation) {
        final double diff = RND.nextDouble() * maxDeviation;
        return baseNumber + diff;
    }

    public static int deviatedByPercentage(int baseNumber, double percentage) {
        return (int)deviated((double)baseNumber, (percentage * baseNumber));
    }

    public static long deviatedByPercentage(long baseNumber, double percentage) {
        return (long)deviated((double)baseNumber, percentage * baseNumber);
    }

    public static double deviatedByPercentage(double baseNumber, double percentage) {
        return deviated(baseNumber, percentage * baseNumber);
    }

    /**
     * Generates random numbers collection that sums up to 1.0
     * 
     * @param numbers
     *            - numbers count
     */
    public static List<Double> distributedValues(int numbers) {
        final List<Double> doubles = new ArrayList<>();
        for (int i = 0; i < numbers; i++) {
            doubles.add(0.01 + RND.nextDouble());
        }
        final double sum = doubles.stream().mapToDouble(Double::valueOf).sum();
        return doubles.stream().map(number -> number / sum).sorted().collect(Collectors.toList());
    }

    public static <T> List<Pair<T, Double>> distributedValues(Collection<T> objects) {
        final List<T> distinctObjects = objects.stream().distinct().collect(Collectors.toList());
        final List<Double> distributedWeights = distributedValues(distinctObjects.size());

        final List<Pair<T, Double>> result = new ArrayList<>(distinctObjects.size());
        for (int i = 0; i < distinctObjects.size(); i++) {
            result.add(Pair.create(distinctObjects.get(i), distributedWeights.get(i)));
        }
        return result;
    }

    public static <T> Collection<Pair<T, Double>> normalize(Collection<Pair<T, Double>> collection) {
        final double sum = collection.stream().mapToDouble(Pair::getSecond).sum();
        return collection.stream()
            .map(pair -> Pair.create(pair.getFirst(), pair.getSecond() / sum))
            .sorted((a, b) -> b.getSecond().compareTo(a.getSecond()))
            .collect(Collectors.toList());
    }

    public static <T> T randomValue(Collection<Pair<T, Double>> collection, List<String> history) {
        final WeightedRandomBag<T> bag = new WeightedRandomBag<T>();
        collection.forEach(pair -> bag.addEntry(pair.getFirst(), pair.getSecond()));
        T random = bag.getRandom();
        if (!history.isEmpty()){
            String last = history.get(history.size() -1);
            return changeTransportMode(last, random);
        } else {
            return random;
        }
    }

    // Pozmieniać środki transportu
    private static <T> T changeTransportMode(String last, T random){
        return random;
    }

    public static class WeightedRandomBag<T> {

        private List<Entry> entries = new ArrayList<Entry>();

        private double accumulatedWeight;

        private Random rand = new Random();

        public synchronized void addEntry(T item, double weight) {
            accumulatedWeight += weight;
            entries.add(new Entry(item, weight, accumulatedWeight));
        }

        /**
         * This operation is very expensive! Use with caution.
         */
        public synchronized void removeEntry(T item) {
            final List<WeightedRandomBag<T>.Entry> newEntries = entries.stream()
                .filter(existingEntry -> !Objects.equals(existingEntry.item, item))
                .collect(Collectors.toList());

            clear();
            newEntries.forEach(entry -> addEntry(entry.item, entry.weight));
        }

        public T getRandom() {
            double r = rand.nextDouble() * accumulatedWeight;
            for (Entry e : entries) {
                if (e.accumulatedWeight >= r) {
                    return e.item;
                }
            }
            return null;
        }

        public synchronized boolean isEmpty() {
            return entries.isEmpty();
        }

        public synchronized void clear() {
            entries.clear();
            accumulatedWeight = 0;
        }

        public synchronized Collection<T> entries() {
            return entries.stream().map(entry -> entry.item).collect(Collectors.toList());
        }

        @Override
        public String toString() {
            return entries.toString();
        }

        @AllArgsConstructor
        @EqualsAndHashCode
        class Entry {

            public final T item;

            public final double weight;

            public final double accumulatedWeight;

            @Override
            public String toString() {
                return String.format("(%s, %.2f)", item, weight);
            }
        }
    }

    public abstract static class WeightedBagCollector<T> implements Collector<T, WeightedRandomBag<T>, WeightedRandomBag<T>> {

        @Override
        public Supplier<WeightedRandomBag<T>> supplier() {
            return WeightedRandomBag::new;
        }

        @Override
        public BiConsumer<WeightedRandomBag<T>, T> accumulator() {
            return (bag, val) -> addValueToBag(bag, val);
        }

        public abstract void addValueToBag(WeightedRandomBag<T> bag, T value);

        @Override
        public BinaryOperator<WeightedRandomBag<T>> combiner() {
            return (f, s) -> {
                throw new UnsupportedOperationException();
            };
        }

        @Override
        public Function<WeightedRandomBag<T>, WeightedRandomBag<T>> finisher() {
            return Function.identity();
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Sets.immutableEnumSet(Characteristics.IDENTITY_FINISH);
        }

        public static <U> WeightedBagCollector<U> create(BiConsumer<WeightedRandomBag<U>, U> accumulator) {
            return new WeightedBagCollector<U>() {
                @Override
                public void addValueToBag(WeightedRandomBag<U> bag, U value) {
                    accumulator.accept(bag, value);
                }
            };
        }
    }
}
