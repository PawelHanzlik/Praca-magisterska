package pl.edu.agh.aolesek.bts.trajectory.analysis.stats.provider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import pl.edu.agh.aolesek.bts.trajectory.analysis.stats.Messages;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;

public class TimeStatsProvider implements IStatsProvider {

	@Override
	public Map<String, Object> provideStats(Collection<ProfileResult> results) {
		final Map<String, Object> timeStats = new HashMap<>();
		addStat(Messages.AverageTrajectoryDuration, meanTrajectoryDuration(results), timeStats);

		final Optional<Pair<LocalDateTime, LocalDateTime>> earliestLatestTime = earliestLatestTime(results);
		final Optional<LocalDateTime> earliestTime = earliestLatestTime.map(Pair::getFirst);
		final Optional<LocalDateTime> latestTime = earliestLatestTime.map(Pair::getSecond);
		addStat(Messages.EarliestTimestamp, earliestTime, timeStats);
		addStat(Messages.LatestTimestamp, latestTime, timeStats);

		return timeStats;
	}

	private Optional<Double> meanTrajectoryDuration(Collection<ProfileResult> results) {
		double durationsSum = 0;
		int numberOfValidTrajectories = 0;
		for (ProfileResult r : results) {
		    if (r.getTrajectory() == null)
		        continue;
			final List<Pair<LocalDateTime, Point>> pointsWithTimestamps = r.getTrajectory().getPointsWithTimestamps();
			if (pointsWithTimestamps.size() > 1) {
				numberOfValidTrajectories++;
				final LinkedList<Pair<LocalDateTime, Point>> linkedList = new LinkedList<>(pointsWithTimestamps);
				final LocalDateTime trajectoryStart = linkedList.getFirst().getFirst();
				final LocalDateTime trajectoryEnd = linkedList.getLast().getFirst();
				final long durationInSeconds = Duration.between(trajectoryStart, trajectoryEnd).toSeconds();
				durationsSum += durationInSeconds;
			}
		}
		
		return numberOfValidTrajectories > 0 ? Optional.of((durationsSum / numberOfValidTrajectories)/3600) : Optional.empty();
	}

	private Optional<Pair<LocalDateTime, LocalDateTime>> earliestLatestTime(Collection<ProfileResult> results) {
		Optional<Pair<LocalDateTime, LocalDateTime>> earliestLatest = results.stream()
				.map(ProfileResult::getTrajectory)
				.filter(Objects::nonNull)
				.map(ITrajectory::getPointsWithTimestamps)
				.flatMap(Collection::stream)
				.map(Pair::getFirst)
				.map(i -> Pair.create(i, i))
				.reduce((a, b) -> Pair.create(a.getFirst().isBefore(b.getFirst()) ? a.getFirst() : b.getFirst(),
						a.getSecond().isAfter(b.getSecond()) ? a.getSecond() : b.getSecond()));
		return earliestLatest;
	}
}
