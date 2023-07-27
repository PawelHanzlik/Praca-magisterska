package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.util.Collection;

//interfejs z metodą planującą całą trasę
public interface IPoiPlanner<T extends IPoi> {

	IRoutePlan planRoute(Collection<PoiHolder<T>> poisForProfile);
}
