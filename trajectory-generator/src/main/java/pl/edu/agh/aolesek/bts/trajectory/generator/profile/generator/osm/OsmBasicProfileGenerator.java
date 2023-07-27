package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.osm;

import com.google.inject.Inject;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.BasicProfileGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator.StartPoints;

import java.util.Arrays;
import java.util.List;

//modyfikuje dane z profilów, nazwy obiektów i metod poruszania się, aby były zgodne z API opartymi o OSM
public class OsmBasicProfileGenerator extends BasicProfileGenerator {

    @Inject
    public OsmBasicProfileGenerator(Config config) {
        super(config);
    }

    @Override
    public List<String> getAmenities() {
        return Constants.AMENITIES;
    }

    @Override
    protected String getPrefferedTransportMode() {
        return "foot-walking";
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

        //kategorie Poi z OSM API
        static final List<String> AMENITIES = Arrays.asList("cinema",
            "bar",
            "restaurant",
            "arts_centre",
            "gambling",
            "planetarium",
            "theatre",
            "dive_centre",
            "marketplace",
            "monastery",
            "place_of_worship",
            "viewpoint",
            "beach",
            "museum",
            "aquarium",
            "theme_park",
            "castle",
            "battlefield",
            "memorial",
            "monument",
            "ruins");

        //środki transportu z OSM API
        static final List<String> TRANSPORT_MODES = Arrays.asList(
            "driving-car", "cycling-regular", "cycling-road", "foot-walking");

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
