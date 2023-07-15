package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import lombok.AllArgsConstructor;
import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//struktura korzystająca z interfejsu IRoutePart. Zawiera punkt początkowy odcinka, punkt końcowy, środek transportu i czas spędzony w danym Poi
@Data
@AllArgsConstructor
public class RoutePart implements IRoutePart {

	private final PoiHolder<? extends IPoi> source; //punkt początkowy

	private final PoiHolder<? extends IPoi> destination; //punkt końcowy

	private final String transportMode; //środek transportu

	private final long secondsSpendAtDestination; //czas spędzony
}
