package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.osm;

import java.util.Objects;

import com.google.inject.Inject;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.OsmPoiAdapter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.PoiPlanner;

//wyznaczanie średnich prędkości dla środków transportu obsługiwanych przez Overpassa
public class OsmPoiPlanner extends PoiPlanner<OsmPoiAdapter> {

    @Inject
    public OsmPoiPlanner(Config parameters, IOpeningHoursSupplier openingHoursSupplier) {
        super(parameters, openingHoursSupplier);
    }

    protected double averageMeterPerSecondSpeedForTransportMode(String transportMode) {
        if (Objects.equals(transportMode, "cycling-regular")) {
            return 3.000;
        } else if (Objects.equals(transportMode, "driving-car")) {
            return 11.111;
        } else if (Objects.equals(transportMode, "cycling-road")) {
            return 4.333;
        } else {
            return config.getDouble(Parameters.AVERAGE_WALKING_SPEED);
        }
    }
}
