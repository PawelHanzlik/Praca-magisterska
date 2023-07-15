package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.inject.Inject;

import lombok.extern.log4j.Log4j2;
import mil.nga.sf.geojson.Feature;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.LineString;
import mil.nga.sf.geojson.Position;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;

//generowanie pliku one.geojson
@Log4j2
public class OneGeoJSONResultHandler extends AbstractResultHandler {

    @Inject
    public OneGeoJSONResultHandler(Config config) {
        super(config);
    }

    @Override
    protected String getFormat() {
        return "one.geojson";
    }

    @Override
    protected void saveResultsToOneFile(Collection<ProfileResult> results, String outputFolder) {
        saveResultsToOneGeoJSONFile(results, outputFolder);
    }

    @Override
    protected void saveResultsToSeparateFiles(Collection<ProfileResult> results, String outputFolder) {
        //zastąpienie wyjątku zwykłą informacją
        //throw new UnsupportedOperationException();
        log.warn("one.geojson file can't be generated in this mode");
    }

    private void saveResultsToOneGeoJSONFile(Collection<ProfileResult> resultsToSave, String outputFolder) {
        final Set<Feature> allPois = createAllPoisFeature(resultsToSave);
        final Set<Feature> trajectories = createTrajectoriesFeatures(resultsToSave);
        writeFeaturesToGeoJson(allPois, trajectories, filename(outputFolder));
    }

    private Set<Feature> createAllPoisFeature(Collection<ProfileResult> results) {
        final Set<Feature> allDistinctPois = results.stream()
            .map(ProfileResult::getPois)
            .flatMap(Collection::stream)
            .map(PoiHolder::getPoi)
            .filter(distinctByKey(poi -> Arrays.asList(poi.getLat(), poi.getLon(), poi.getName())))
            .map(this::createPoiFeature)
            .collect(Collectors.toSet());
        return allDistinctPois;
    }

    private Feature createPoiFeature(IPoi poi) {
        final Feature f = new Feature(new mil.nga.sf.geojson.Point(new Position(poi.getLon(), poi.getLat())));
        setPoiProperties(poi, f);
        return f;
    }

    private void setPoiProperties(IPoi poi, Feature feature) {
        final Map<String, Object> properties = new TreeMap<>();
        properties.put("_poi_name", poi.getName());
        properties.put("_lat;lon", poi.getLat() + ";" + poi.getLon());
        properties.put("_category", poi.getCategory());
        poi.getAdditionalProperties().forEach(properties::put);
        feature.setProperties(properties);
    }

    private Set<Feature> createTrajectoriesFeatures(Collection<ProfileResult> results) {
        return results.stream().map(this::createTrajectoryFeature).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Feature createTrajectoryFeature(ProfileResult result) {
        if (result.getTrajectory() == null) {
            log.error("null trajectory for " + result.getProfile().getFullName());
            return null;
        }
        final List<Pair<LocalDateTime, Point>> points = result.getTrajectory().getPointsWithTimestamps();
        final List<Position> trajectoryPoints = points.stream()
            .map(pointWithTimestamp -> new Position(pointWithTimestamp.getSecond().getLon(), pointWithTimestamp.getSecond().getLat()))
            .collect(Collectors.toList());

        final LineString trajectory = new LineString(trajectoryPoints);
        final Feature trajectoryFeature = new Feature(trajectory);
        setTrajectoryProperties(result, trajectoryFeature);
        return trajectoryFeature;
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private void writeFeaturesToGeoJson(Set<Feature> pois, Collection<Feature> trajectories, String filename) {
        if (pois == null || trajectories.isEmpty()) {
            log.warn("Unable to save features to GeoJson as features collection is empty!");
            return;
        }
        final Set<Feature> allFeatures = Stream.concat((pois.stream()), trajectories.stream()).collect(Collectors.toSet());
        try (PrintWriter out = new PrintWriter(filename, StandardCharsets.UTF_8)) {
            final String featureCollectionContent = FeatureConverter.toStringValue(new FeatureCollection(allFeatures));
            out.print(featureCollectionContent);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write result as GeoJSON!", e);
        }
    }

    private void setTrajectoryProperties(ProfileResult result, Feature trajectoryFeature) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("_interests", result.getProfile().getInterests().stream()
            .map(pair -> String.format(Locale.ROOT, "%s(%.2f)", pair.getFirst(), pair.getSecond())).collect(Collectors.toList()));
        properties.put("_owner_name", result.getProfile().getFullName());
        properties.put("_transport_modes",
            result.getRoutePlan().getParts().stream().map(IRoutePart::getTransportMode).collect(Collectors.toSet()));
        properties.put("_trajectory_id", result.getTrajectory().getTrajectoryId());
        trajectoryFeature.setProperties(properties);
    }
}
