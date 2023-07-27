package pl.edu.agh.aolesek.bts.trajectory.generator.app;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
@Data
public class Config {
    //przetwarzanie danych z pliku konfiguracyjnego

    private static final String APP_NAME = "Trajectory generator";

    private static final String PARAMETER_COLLECTION_SEPARATOR = ",";

    private static final String DEFAULT_CONFIG_FILE = "config.cfg";

    private final LocalDateTime START_TIME = LocalDateTime.now();

    private final Map<Parameters, String> parameters = new HashMap<>();

    public Config(String[] args) {
        final String configPath = args.length <= 0 ? DEFAULT_CONFIG_FILE : args[0];
        try {
            Map<Parameters, String> parsedParameters = new HashMap<>();
            Files.lines(Paths.get(configPath)).forEach(line -> {
                final String[] pair = line.split("=");
                if (pair.length == 0 || pair[0].trim().isBlank()) {
                    return;
                }
                final String key = pair[0];
                final String value = pair.length > 1 ? pair[1] : "";
                final Parameters parameter = Parameters.valueOf(key.toUpperCase());
                final String parameterValue = value.trim();
                parsedParameters.put(parameter, parameterValue);
            });
            parameters.putAll(parsedParameters);
        } catch (IOException | NullPointerException e) {
            log.error("Unable to read config file! ", e);
        }
    }

    public String getAppName() {
        return APP_NAME;
    }

    public LocalDateTime resolveSimulationStartTime() {
        return START_TIME;
    }

    public String get(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return parameterValue;
    }

    public Collection<String> getStringCollection(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return Stream.of(parameterValue.split(PARAMETER_COLLECTION_SEPARATOR))
            .map(String::trim)
            .collect(Collectors.toSet());
    }

    public double getDouble(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return Double.parseDouble(parameterValue);
    }

    public int getInt(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return Integer.parseInt(parameterValue);
    }

    public long getLong(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return Long.parseLong(parameterValue);
    }

    public boolean getBoolean(Parameters p) {
        final String parameterValue = parameters.get(p);
        if (parameterValue == null) {
            log.warn("Unable to get value for parameter " + p + ". Value is null!");
        }
        return Boolean.parseBoolean(parameterValue);
    }
}
