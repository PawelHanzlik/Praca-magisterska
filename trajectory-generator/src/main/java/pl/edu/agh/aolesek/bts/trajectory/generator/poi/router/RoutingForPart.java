package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router;

import lombok.Data;
import mil.nga.sf.geojson.LineString;

//struktura zawierająca czas zajmowany przez pokonywanie odcinka, odległość i jedną zmienną tekstową
@Data
public class RoutingForPart {

	private final double duration;

	private final double distance;

	private final LineString line;
}
