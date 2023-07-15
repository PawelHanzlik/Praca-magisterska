package pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.model;

import java.util.List;
import java.util.Map;

import lombok.Data;

//struktura zawierająca informacje na temat Poi
@Data
public class OsmPoiModel {

    List<OsmModelElement> elements;

    @Data
    public class OsmModelElement {

        String type;

        long id;

        double lat;

        double lon;

        Map<String, String> tags;
    }
}
