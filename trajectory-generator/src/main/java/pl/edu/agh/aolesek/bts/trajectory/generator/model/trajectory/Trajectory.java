package pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//tworzenie obiektów wchodzących w skład trajektorii
@Data
public class Trajectory implements ITrajectory {

    private final String trajectoryId;

    @JsonIgnore
    transient IProfile profile;

    private List<Pair<LocalDateTime, IPoi>> poisWithTimestamps;

    private List<Pair<LocalDateTime, Point>> pointsWithTimestamps;

    public Trajectory(List<Pair<LocalDateTime, IPoi>> poisWithTimestamps, List<Pair<LocalDateTime, Point>> pointsWithTimestamps,
        IProfile profile) {
        this.trajectoryId = profile.getFullName() + "@" + UUID.randomUUID().toString() + "  [profileId " + profile.getId() + "]";
        this.profile = profile;
        this.poisWithTimestamps = poisWithTimestamps;
        this.pointsWithTimestamps = pointsWithTimestamps;
    }
}
