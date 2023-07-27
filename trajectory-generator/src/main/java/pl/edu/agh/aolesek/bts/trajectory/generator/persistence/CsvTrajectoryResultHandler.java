package pl.edu.agh.aolesek.bts.trajectory.generator.persistence;

import com.google.inject.Inject;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CsvTrajectoryResultHandler extends AbstractFileResultHandler {

	@Inject
	public CsvTrajectoryResultHandler(Config config) {
		super(config);
	}

	@Override
	protected String getFormat() {
		return "csv";
	}

	protected void writeResultToFile(ProfileResult result, PrintWriter writer) {
		final ProfileLogger log = result.getProfile().getLogger();
		if (result.getTrajectory() == null) {
			log.warn("Unable to write trajectory for " + result.getProfile().getFullName() + " because it is empty!");
			return;
		}

		final List<Pair<LocalDateTime, Point>> trajectoryPointsWithTimestamps = result.getTrajectory().getPointsWithTimestamps();
		final List<Pair<LocalDateTime, IPoi>> poisWithTimestamps = result.getTrajectory().getPoisWithTimestamps();
		trajectoryPointsWithTimestamps.stream()
				.map(point -> poiToCsvStringWithProfile(point, result, poisWithTimestamps))
				.forEach(writer::println);
	}

	private String poiToCsvStringWithProfile(Pair<LocalDateTime, Point> pointWithTimestamp, ProfileResult result,
			List<Pair<LocalDateTime, IPoi>> poisWithTimestamps) {
		return String.format("%s;%s;%s;%s;%f;%f;%s",
				result.getProfile().getId(),
				result.getProfile().getFullName(),
				result.getTrajectory().getTrajectoryId(),
				pointWithTimestamp.getFirst().toString(),
				pointWithTimestamp.getSecond().getLat(),
				pointWithTimestamp.getSecond().getLon(),
				resolveType(pointWithTimestamp, poisWithTimestamps, result.getProfile()));
	}

	private String resolveType(Pair<LocalDateTime, Point> point, List<Pair<LocalDateTime, IPoi>> poisWithTimestamps, IProfile profile) {
		List<Pair<LocalDateTime, IPoi>> poisForThisTimestamp = poisWithTimestamps.stream()
				.filter(poiWithTimestamp -> Objects.equals(poiWithTimestamp.getFirst(), point.getFirst())
						&& coordinatesEqual(poiWithTimestamp.getSecond().getLat(), point.getSecond().getLat())
						&& coordinatesEqual(poiWithTimestamp.getSecond().getLon(), point.getSecond().getLon()))
				.collect(Collectors.toList());

		if (poisForThisTimestamp.size() == 1) {
			IPoi poi = poisForThisTimestamp.iterator().next().getSecond();
			return "poi(" + poi.getName() + ")";
		} else if (poisForThisTimestamp.size() > 1) {
			profile.getLogger().warn("Two waypoints with same time and coordinates, unable to resolve type for csv file!");
			return "poi/waypoint";
		}

		return "waypoint";
	}

	private boolean coordinatesEqual(Double a, Double b) {
		if (a != null && b != null) {
			return Math.abs(a - b) <= 0.001;
		}
		return Objects.equals(a, b);
	}

	protected void writeHeader(PrintWriter writer) {
		writer.println("profile id;profile name;trajectory id;timestamp;latitude;longitude;type");
	}
}
