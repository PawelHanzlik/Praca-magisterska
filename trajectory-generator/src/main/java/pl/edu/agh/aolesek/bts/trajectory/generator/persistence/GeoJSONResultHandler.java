package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import mil.nga.sf.geojson.*;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

//generowanie pliku geojson
@Log4j2
public class GeoJSONResultHandler extends AbstractResultHandler {

	@Inject
	public GeoJSONResultHandler(Config config) {
		super(config);
	}

	@Override
	protected String getFormat() {
		return "geojson";
	}

	@Override
	protected void saveResultsToOneFile(Collection<ProfileResult> results, String outputFolder) {
		final List<Feature> allFeatures = new ArrayList<>();
		results.forEach(result -> createAndAddFeatures(result, allFeatures));
		writeFeaturesToGeoJson(allFeatures, filename(outputFolder));
	}

	@Override
	protected void saveResultsToSeparateFiles(Collection<ProfileResult> results, String outputFolder) {
		results.forEach(result -> {
			final List<Feature> allFeatures = new ArrayList<>();
			createAndAddFeatures(result, allFeatures);
			writeFeaturesToGeoJson(result.getProfile(), allFeatures, filename(outputFolder, result.getTrajectory().getTrajectoryId()));
		});
	}

	private void createAndAddFeatures(ProfileResult result, List<Feature> allFeatures) {
		createAndAddTrajectoryFeature(result, allFeatures);
		createAndAddPoisFeatures(result, allFeatures);
	}

	private void createAndAddTrajectoryFeature(ProfileResult result, List<Feature> allFeatures) {
		final ProfileLogger log = result.getProfile().getLogger();
		if (result.getTrajectory() == null) {
			log.warn("Unable to write trajectory for " + result.getProfile().getFullName() + " because it is empty!");
			return;
		}

		final List<Pair<LocalDateTime, Point>> points = result.getTrajectory().getPointsWithTimestamps();
		final List<Position> trajectoryPoints = points.stream()
				.map(pointWithTimestamp -> new Position(pointWithTimestamp.getSecond().getLon(), pointWithTimestamp.getSecond().getLat()))
				.collect(Collectors.toList());

		final LineString trajectory = new LineString(trajectoryPoints);
		final Feature trajectoryFeature = new Feature(trajectory);
		setTrajectoryProperties(result, trajectoryFeature);

		allFeatures.add(trajectoryFeature);
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

	private void createAndAddPoisFeatures(ProfileResult result, List<Feature> allFeatures) {
		final ProfileLogger log = result.getProfile().getLogger();
		if (result.getTrajectory() == null) {
			log.warn("Unable to write pois for " + result.getProfile().getFullName() + " because pois are empty!");
			return;
		}

		final LinkedList<Pair<LocalDateTime, IPoi>> poisWithTimestamps = new LinkedList<>(result.getTrajectory().getPoisWithTimestamps());
		// Place of depeature as last item means going back to home at the end. This is to prevent adding this poi 2 times.
		if (poisWithTimestamps.size() > 1
				&& Objects.equals(poisWithTimestamps.getLast().getSecond().getId(), IRoutePart.PLACE_OF_DEPARTURE)) {
			poisWithTimestamps.removeLast();
		}

		final Set<Feature> poisFeatures = poisWithTimestamps.stream().map(poi -> createPoiFeature(result, poi))
				.collect(Collectors.toSet());
		allFeatures.addAll(poisFeatures);
	}

	private Feature createPoiFeature(ProfileResult result, Pair<LocalDateTime, IPoi> poiWithTimestamp) {
		final Feature f = new Feature(
				new mil.nga.sf.geojson.Point(new Position(poiWithTimestamp.getSecond().getLon(), poiWithTimestamp.getSecond().getLat())));
		setPoiProperties(result, poiWithTimestamp, f);
		return f;
	}

	private void setPoiProperties(ProfileResult result, Pair<LocalDateTime, IPoi> poiWithTimestamp, final Feature f) {
		final ProfileLogger log = result.getProfile().getLogger();

		final Map<String, Object> properties = new TreeMap<>();
		properties.put("_time", poiWithTimestamp.getFirst().toString());
		properties.put("_poi_name", poiWithTimestamp.getSecond().getName());
		properties.put("_lat;lon", poiWithTimestamp.getSecond().getLat() + ";" + poiWithTimestamp.getSecond().getLon());
		properties.put("_visitor_name", result.getProfile().getFullName());
        properties.put("_trajectory_id", result.getTrajectory().getTrajectoryId());
		properties.put("_reason", poiWithTimestamp.getSecond().getCategory());

		Optional<IRoutePart> correspondingPart = resolveCorrespondingPart(result, poiWithTimestamp);
		if (correspondingPart.isPresent()) {
			properties.put("_used_transport_mode", correspondingPart.get().getTransportMode());
			properties.put("_seconds_spent", correspondingPart.get().getSecondsSpendAtDestination());
		} else if (!Objects.equals(poiWithTimestamp.getSecond().getId(), IRoutePart.PLACE_OF_DEPARTURE)) {
			log.warn("Unable to find corresponding route part while generating geoJson for " + result.getProfile().getFullName());
		}

		poiWithTimestamp.getSecond().getAdditionalProperties().forEach(properties::put);
		f.setProperties(properties);
	}

	private Optional<IRoutePart> resolveCorrespondingPart(ProfileResult result, Pair<LocalDateTime, IPoi> poiWithTimestamp) {
		Optional<IRoutePart> correspondingPart = result.getRoutePlan().getParts().stream()
				.filter(part -> Objects.equals(part.getDestination().getPoi(), poiWithTimestamp.getSecond()))
				.findFirst();
		return correspondingPart;
	}

	private void writeFeaturesToGeoJson(List<Feature> allFeatures, String filename) {
		if (allFeatures.isEmpty()) {
			log.warn("Unable to save features to GeoJson as features collection is empty!");
			return;
		}
		writeFeaturesToGeoJson(null, allFeatures, filename);
	}

	private void writeFeaturesToGeoJson(IProfile profile, List<Feature> allFeatures, String filename) {
		if (profile != null) {
			final ProfileLogger log = profile.getLogger();
			if (allFeatures.isEmpty()) {
				log.warn("Unable to save features to GeoJson as features collection is empty!");
				return;
			}
		}
		

		try (PrintWriter out = new PrintWriter(filename, StandardCharsets.UTF_8)) {
			final String featureCollectionContent = FeatureConverter.toStringValue(new FeatureCollection(allFeatures));
			out.print(featureCollectionContent);
		} catch (IOException e) {
			throw new RuntimeException("Unable to write result as GeoJSON!", e);
		}
	}
}
