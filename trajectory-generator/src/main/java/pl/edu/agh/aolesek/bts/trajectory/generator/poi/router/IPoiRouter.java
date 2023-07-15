package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;

//interfejs zawierający metodę do wyznaczania przebiegu odcinka trajektorii
public interface IPoiRouter {

	ITrajectory route(IProfile profile, IRoutePlan routePlan);

	default void logStats() {
		// do nothing by default
	}
}
