package pl.edu.agh.aolesek.bts.trajectory.analysis.stats;

import pl.edu.agh.aolesek.bts.trajectory.analysis.ResultReader;
import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider.IStatsProvider;
import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider.PoiStatsProvider;
import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider.TimeStatsProvider;
import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider.TravelStatsProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;

import java.util.*;

public class StatsGenerator {

    private final ResultReader resultReader;

    public StatsGenerator(Collection<String> resultFilePaths) {
        resultReader = new ResultReader(resultFilePaths);
    }

    public Collection<? extends IStatsProvider> resolveProviders() {
        return Arrays.asList(
            new PoiStatsProvider(),
            new TimeStatsProvider(),
            new TravelStatsProvider());
    }

    @SuppressWarnings("unchecked")
    public void generateStats() {
        final Collection<ProfileResult> results = resultReader.read();
        final Map<String, Object> stats = new HashMap<>();

        final Collection<? extends IStatsProvider> providers = resolveProviders();

        providers.forEach(provider -> stats.putAll(provider.provideStats(results)));

        stats.forEach((stat, val) -> {
            System.out.println(stat + " -> " + val);

            if (Objects.equals(stat, Messages.VisitsByCategories)) {
                handleMap((Map<String, Object>)val);
            }

            if (Objects.equals(stat, Messages.MostVisitedPois)) {
                handleMap((Map<String, Object>)val);
            }
        });
    }

    private void handleMap(Map<String, Object> map) {
        map.entrySet().stream()
            .map(this::transformEntry)
            .sorted(this::compareMapEntry)
            .forEach(this::printMapEntryAsTableRow);
    }

    private Pair<String, Pair<Long, Double>> transformEntry(Map.Entry<String, Object> entry) {
        @SuppressWarnings("unchecked")
        final Pair<Long, Double> value = (Pair<Long, Double>)entry.getValue();
        return Pair.create(entry.getKey(), value);
    }

    private int compareMapEntry(Pair<String, Pair<Long, Double>> e1, Pair<String, Pair<Long, Double>> e2) {
        return e2.getSecond().getFirst().compareTo(e1.getSecond().getFirst());
    }

    private void printMapEntryAsTableRow(Pair<String, Pair<Long, Double>> entry) {
        System.out.println(
            String.format("%s \t\t\t %d \t\t\t %.2f", entry.getFirst(), entry.getSecond().getFirst(), entry.getSecond().getSecond()*100));
    }
}
