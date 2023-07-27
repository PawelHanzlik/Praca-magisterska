package pl.edu.agh.aolesek.bts.trajectory.generator.utils;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;

import static java.lang.Math.*;

//wyznaczanie odległości
public final class GeoUtils {

    private GeoUtils() {
    }

    public static double kmDistance(Point a, Point b) {
        return distance(a.getLat(), a.getLon(), b.getLat(), b.getLon()) / 1000.0;
    }

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth

        double latDistance = toRadians(lat2 - lat1);
        double lonDistance = toRadians(lon2 - lon1);
        double a = sin(latDistance / 2) * sin(latDistance / 2) + cos(toRadians(lat1))
            * cos(toRadians(lat2)) * sin(lonDistance / 2) * sin(lonDistance / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }

    public static Point moveCoordinate(double originLat, double originLon, double distanceMeters, double bearingDegrees) {
        double distRadians = distanceMeters / (6372797.6);
        double rbearing = bearingDegrees * PI / 180.0;

        double lat1 = originLat * PI / 180;
        double lon1 = originLon * PI / 180;

        double lat2 = asin(sin(lat1) * cos(distRadians) + cos(lat1) * sin(distRadians) * cos(rbearing));
        double lon2 = lon1 + atan2(sin(rbearing) * sin(distRadians) * cos(lat1), cos(distRadians) - sin(lat1) * sin(lat2));

        return new Point(lat2 * 180 / PI, lon2 * 180 / PI);
    }
}
