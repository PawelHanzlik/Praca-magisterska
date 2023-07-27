package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.RandomUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//z pliku average\_spent\_minutes\_per\_category.txt wczytywane są średnie czasy jakie ma zajmować pobyt w obiekcie danej kategorii,
// wykorzystany jest interfejs ISpentTimeProvider
@Log4j2
public class RandomSpentTimeProvider implements ISpentTimeProvider {

    private final String DATA_FILE = "average_spent_minutes_per_category.txt";

    private final Map<String, Long> baseDurationsForCategories = new HashMap<>();

    private final Random rnd = new Random();

    private final Config config;

    @Inject
    public RandomSpentTimeProvider(Config config) {
        this.config = config;
        readData();
    }

    private void readData() {
        try (BufferedReader reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource(DATA_FILE).getFile()))) {
            String line = reader.readLine();
            while (line != null) {
                processLine(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error("Unable to read spent time data from file", e);
        }
    }

    private void processLine(String line) {
        final String[] split = line.split("=");
        if (split.length == 2) {
            baseDurationsForCategories.put(split[0], Long.parseLong(split[1]) * 60);
        }
    }

    @Override
    public long resolveSecondsSpent(IPoi poi) {
        final long averageSecondsSpent = resolveAverageSecondsSpent(poi);
        return averageSecondsSpent;
    }

    private long resolveAverageSecondsSpent(IPoi poi) {
        final String category = poi.getCategory();
        if (baseDurationsForCategories.containsKey(category)) {
            final Long base = baseDurationsForCategories.get(category);
            return RandomUtils.deviatedByPercentage(base, config.getDouble(Parameters.GENERATED_SPENT_SECONDS_BASE_PERCENTAGE_DEVIATION));
        }
        return config.getLong(Parameters.GENERATED_SPENT_SECONDS_BASE)
            + rnd.nextInt((int)(config.getLong(Parameters.GENERATED_SPENT_SECONDS_MAX) - 15));
    }
}
