package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//generowanie pliku json
public class JSONResultHandler extends AbstractResultHandler {

    //utworzenie jsona
    final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Inject
    public JSONResultHandler(Config config) {
        super(config);
    }

    @Override
    protected String getFormat() {
        return "json";
    }

    //wybór zapisu do jednego pliku
    @Override
    protected void saveResultsToOneFile(Collection<ProfileResult> results, String outputFolder) {
        final Map<String, List<ProfileResult>> groupedResults = results.stream()
            .collect(Collectors.groupingBy(result -> result.getProfile().getId()));

        writeToJson(filename(outputFolder), groupedResults);

        Map<String, List<ProfileLogger>> allLogsById = results.stream()
            .collect(Collectors.groupingBy(result -> result.getProfile().getId(),
                Collectors.mapping(result -> result.getProfile().getLogger(), Collectors.toList())));
        writeToJson(filename(outputFolder) + ".log.json", allLogsById);
    }

    //wybór zapisu do odrębnych plików
    @Override
    protected void saveResultsToSeparateFiles(Collection<ProfileResult> results, String outputFolder) {
        results.forEach(result -> {
            Map<String, Object> mapForResult = createMapForResult(result);
            writeToJson(filename(outputFolder, result.getTrajectory().getTrajectoryId()), mapForResult);

            ProfileLogger logForResult = result.getProfile().getLogger();
            writeToJson(filename(outputFolder, result.getTrajectory().getTrajectoryId()) + ".log.json", logForResult);
        });
    }

    private Map<String, Object> createMapForResult(ProfileResult result) {
        final HashMap<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("profile", result.getProfile());
        jsonMap.put("pois", result.getPois());
        jsonMap.put("route", result.getRoutePlan());
        jsonMap.put("trajectory", result.getTrajectory());
        return jsonMap;
    }

    private void writeToJson(String filename, Object object) {
        try (FileWriter writer = new FileWriter(filename, StandardCharsets.UTF_8)) {
            gson.toJson(object, writer);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save output to json!", e);
        }
    }
}
