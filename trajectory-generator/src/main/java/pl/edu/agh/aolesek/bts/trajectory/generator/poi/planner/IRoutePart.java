package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//interfejs z metodami do planowania części trasy
public interface IRoutePart {

	static final String PLACE_OF_DEPARTURE = "place_of_departure";

	PoiHolder<? extends IPoi> getSource();

	PoiHolder<? extends IPoi> getDestination();

	String getTransportMode();

	long getSecondsSpendAtDestination();
}
