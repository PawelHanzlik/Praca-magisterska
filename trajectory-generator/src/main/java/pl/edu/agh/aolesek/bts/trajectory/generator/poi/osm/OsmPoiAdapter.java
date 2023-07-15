package pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.osm.model.OsmPoiModel.OsmModelElement;

//zbiór metod dla generatora
@RequiredArgsConstructor
public class OsmPoiAdapter implements IPoi {

    private final OsmModelElement osmModel;

    // text phrase that was used to match this poi to the profile (category, tag, depends on implementation)
    private final String foundBecause;

    private long averageSecondsSpent;

    @Override
    public String getName() {
        String name = getId();
        if (osmModel.getTags().get("name") != null) {
            name = String.valueOf(osmModel.getTags().get("name"));
        } else if (osmModel.getTags().get("tourism") != null) {
            name = String.valueOf(osmModel.getTags().get("tourism"));
        }

        return String.format("%s(%s)", name, getCategory());
    }

    @Override
    public double getLat() {
        return osmModel.getLat();
    }

    @Override
    public double getLon() {
        return osmModel.getLon();
    }

    @Override
    public String getCategory() {
        return foundBecause;
    }

    @Override
    public long getAverageSecondsSpent() {
        return averageSecondsSpent;
    }

    @Override
    public void setAverageSecondsSpent(long seconds) {
        this.averageSecondsSpent = seconds;
    }

    @Override
    public Map<String, ? extends Object> getAdditionalProperties() {
        final HashMap<String, String> additionalProperties = new HashMap<>(osmModel.getTags());
        additionalProperties.put("found_because", getCategory());
        return additionalProperties;
    }

    @Override
    public String getId() {
        return String.valueOf(osmModel.getId());
    }
}
