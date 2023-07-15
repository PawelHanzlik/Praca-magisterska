package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import java.time.LocalDateTime;
import java.util.List;

//interfejs z metodami zwracającymi listę odcinków trasy i czas rozpoczęcia wędrówki
public interface IRoutePlan {

	List<IRoutePart> getParts();

	LocalDateTime getStartTime();
}
