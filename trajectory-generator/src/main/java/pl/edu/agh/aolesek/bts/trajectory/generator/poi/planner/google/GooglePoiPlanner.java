package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.google;

import com.google.inject.Inject;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.google.GooglePoiAdapter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.PoiPlanner;

import java.util.Objects;

//wyznaczanie średnich prędkości dla środków transportu obsługiwanych przez Google Maps API
public class GooglePoiPlanner extends PoiPlanner<GooglePoiAdapter> {

	@Inject
	public GooglePoiPlanner(Config parameters, IOpeningHoursSupplier openingHoursSupplier) {
		super(parameters, openingHoursSupplier);
	}

	//średnia prędkość dla środków transportu
	protected double averageMeterPerSecondSpeedForTransportMode(String transportMode) {
		if (Objects.equals(transportMode, "bicycling")) {
			return 4.000;
		} else if (Objects.equals(transportMode, "transit")) { //dodanie transportu zbiorowego
			return 5.222; //średnia prędkość tramwaju w Krakowie według danych z roku 2021
		} else if (Objects.equals(transportMode, "driving")) {
			return 11.111;
        } else {
            return config.getDouble(Parameters.AVERAGE_WALKING_SPEED);
        }
	}
}