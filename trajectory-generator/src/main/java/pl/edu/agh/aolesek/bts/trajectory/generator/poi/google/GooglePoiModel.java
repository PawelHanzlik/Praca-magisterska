package pl.edu.agh.aolesek.bts.trajectory.generator.poi.google;

import lombok.Data;

import java.util.List;

//struktura zawierająca informacje o punkcie Poi
//TODO: Implement opening hours and prices level for Google provider.
@Data
public class GooglePoiModel {

    List<GoogleModelElement> results;

    @Data
    public class GoogleModelElement {

        Geometry geometry;

        String name;

        List<String> types;

        double rating;

        String vicinity;

        String place_id;
    }

    @Data
    public class Geometry {

        Location location;

    }

    @Data
    public class Location {

        double lat;

        double lng;
    }
}
