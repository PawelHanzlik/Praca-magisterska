package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.google;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.inject.Inject;

import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.BasicProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.StartPoints;

//modyfikuje dane z profilów, nazwy obiektów i metod poruszania się, aby były zgodne z Google API
public class GoogleBasicProfileGenerator extends BasicProfileGenerator {

	@Inject
	public GoogleBasicProfileGenerator(Config config) {
		super(config);
	}

	@Override
	public List<String> getAmenities() {
		return Constants.AMENITIES;
	}

	@Override
	protected String getPrefferedTransportMode() {
		return "walking";
	}

	@Override
	protected List<String> getTransportModes() {
		return Constants.TRANSPORT_MODES;
	}

	@Override
	public Point randomPlaceOfDeparture() {
		return Constants.START_POINTS.get(RND.nextInt(Constants.START_POINTS.size()));
	}

	static class Constants {

		//kategorie obiektów z Google API
		static final List<String> AMENITIES = Arrays.asList("amusement_park",
				"aquarium",
				"art_gallery",
				"bar",
				"beauty_salon",
				"bowling_alley",
				"cafe",
				"campground",
				"casino",
				"cemetery",
				"church",
				"city_hall",
				"florist",
				"gym",
				"hindu_temple",
				"movie_rental",
				"jewelry_store",
				"library",
				"liquor_store",
				"mosque",
				"movie_rental",
				"movie_theater",
				"night_club",
				"museum",
				"park",
				"pet_store",
				"restaurant",
				"rv_park",
				"spa",
				"stadium",
				"store",
				"synagogue",
				"zoo",
				"travel_agency",
				"archipelago",
				"natural_feature",
				"place_of_worship",
				"town_square",
				"gambling",
				"planetarium",
				//"dive_centre", brak kategorii na liście Google
				"marketplace",
				"monastery",
				"viewpoint",
				"beach",
				"museum",
				"theme_park",
				"castle",
				"battlefield",
				"memorial",
				"monument",
				"ruins");

		//środki transportu z Google API
		static final List<String> TRANSPORT_MODES = Arrays.asList(
				//"driving", "walking", "bicycling");
				"driving", "walking", "bicycling", "transit"); //dodanie transportu zbiorowego

		//punkty początkowe!!!
		//poniżej stary kod
		/*static final List<Point> START_POINTS = Arrays.asList(new Point(50.081441, 19.974166),
				new Point(50.071876, 19.928880),
				new Point(50.067644, 20.004548),
				new Point(50.055132, 19.920281),
				new Point(50.052740, 20.007128),
				new Point(50.076150, 19.866481),
				new Point(50.021153, 19.920064),
				new Point(50.026365, 20.019441),
				new Point(50.064786, 19.845023),
				new Point(50.110331, 19.977865),
				new Point(50.083009, 19.896740),
				new Point(50.064348, 19.930454),
				new Point(50.055515, 19.948511),
				new Point(50.069131, 19.913256));*/

		static List<Point> START_POINTS = StartPoints.getStartPoints();
	}
}
