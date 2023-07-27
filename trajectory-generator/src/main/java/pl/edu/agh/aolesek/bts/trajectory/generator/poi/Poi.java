package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import com.google.common.collect.ImmutableMap;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

//struktura zawierająca dane tworzące obiekt Poi, metody do niej są zapewniane przez interfejs IPoi
@AllArgsConstructor
@Data
public class Poi implements IPoi {

    private final String id;

    private final String name;

    private final String category;

    private final double lat;

    private final double lon;

    private long averageSecondsSpent;

    @Override
    public Map<String, ? extends Object> getAdditionalProperties() {
        return ImmutableMap.of("visit_cause", category, "average_seconds_spent", averageSecondsSpent);
    }
}
