package pl.edu.agh.aolesek.bts.trajectory.generator.validator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.ProfileLogger;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePart;
import pl.edu.agh.aolesek.bts.trajectory.generator.utils.GeoUtils;

public class TrajectoryTimeValidator implements ITrajectoryValidator {

	//znajduje błędy w wygenerowanych trajektoriach
	@Override
	public void validate(ProfileResult result) {
		final ProfileLogger logger = result.getProfile().getLogger();
		try {
			result.getPois().forEach(poi -> {
				final long averageSecondsSpent = poi.getPoi().getAverageSecondsSpent();
				if (averageSecondsSpent <= 0 || averageSecondsSpent >= 100000) {
					logger.warn(String.format("Average seconds spent for " + poi.getPoi().getName() + " is invalid!"));
				}
			});

			final List<Pair<LocalDateTime, IPoi>> poisWithTimestamps = result.getTrajectory().getPoisWithTimestamps();
			final Iterator<Pair<LocalDateTime, IPoi>> iterator = poisWithTimestamps.iterator();
			if (poisWithTimestamps.size() < 2) {
				logger.warn("Pois with timestampis has less than 2 elements for " + result.getProfile().getFullName());
			}

			long secondsSpentAtLastDestination = 0;
			Pair<LocalDateTime, IPoi> currentPoi = iterator.next();
			Pair<LocalDateTime, IPoi> nextPoi = null;
			while (iterator.hasNext()) {
				nextPoi = iterator.next();

				final Optional<IRoutePart> maybeCorrespondingPart = resolveCorrespondingRoutePart(currentPoi, nextPoi,
						result);
				if (maybeCorrespondingPart.isEmpty()) {
					logger.warn(String.format(
							"Unable to get corresponding part for points %s, %s, those point should be visited according to plan",
							currentPoi, nextPoi));
				} else {
					IRoutePart correspondingPart = maybeCorrespondingPart.get();
					LocalDateTime timeAtCurrentPoi = currentPoi.getFirst();
					LocalDateTime timeAtNextPoi = nextPoi.getFirst();

					if (timeAtCurrentPoi.isAfter(timeAtNextPoi)) {
						logger.warn(String.format("Time at current poi is after time at next poi!"));
					}

					long secondsBetweenPois = Duration.between(timeAtCurrentPoi, timeAtNextPoi).toSeconds();
					if (secondsSpentAtLastDestination > secondsBetweenPois) {
						logger.warn(
								String.format("Spent more time at previous destination that whole time between pois"));
					}

					long travelTime = secondsBetweenPois - secondsSpentAtLastDestination;
					if (travelTime < 0 || travelTime > 10000) {
						logger.warn(String.format("Travel time between pois %s and %s is %d", currentPoi, nextPoi,
								secondsBetweenPois));
					}

					double poiDistance = GeoUtils.distance(currentPoi.getSecond().getLat(),
							currentPoi.getSecond().getLon(), nextPoi.getSecond().getLat(),
							nextPoi.getSecond().getLon());
					if (travelTime != 0) {
						double speed = poiDistance / travelTime;

						if (speed <= 0 || speed > 50) {
							logger.warn(String.format("Travel speed between pois %s and %s seems invalid: %.2f",
									currentPoi, nextPoi, speed));
						}
					}

					secondsSpentAtLastDestination = correspondingPart.getSecondsSpendAtDestination();
				}

				currentPoi = nextPoi;
			}
		} catch (Exception e) {
			logger.warn(String.format("An exception occured during validation."));
		}
	}

	private Optional<IRoutePart> resolveCorrespondingRoutePart(Pair<LocalDateTime, IPoi> previousPlace,
			Pair<LocalDateTime, IPoi> thisPlace, ProfileResult result) {
		Optional<IRoutePart> correspondingPart = result.getRoutePlan().getParts().stream()
				.filter(part -> Objects.equals(part.getSource().getPoi(), previousPlace.getSecond())
						&& Objects.equals(part.getDestination().getPoi(), thisPlace.getSecond()))
				.findFirst();
		return correspondingPart;
	}
}
