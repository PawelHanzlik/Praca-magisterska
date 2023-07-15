package pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory;

import java.time.LocalDateTime;
import java.util.List;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Pair;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//informacje o trajektorii i profilu, do którego należy
public interface ITrajectory {

    List<Pair<LocalDateTime, IPoi>> getPoisWithTimestamps();

    List<Pair<LocalDateTime, Point>> getPointsWithTimestamps();

    IProfile getProfile();
    
    String getTrajectoryId();
}
