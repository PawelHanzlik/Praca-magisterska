package pl.edu.agh.aolesek.bts.trajectory.generator.core;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.trajectory.ITrajectory;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IRoutePlan;

import java.util.Collection;

//informacje otrzymane dla danego profilu
@Data
public class ProfileResult {

    private final IProfile profile;

    private final Collection<? extends PoiHolder<? extends IPoi>> pois;

    @SerializedName("route")
    private final IRoutePlan routePlan;

    private final ITrajectory trajectory;
}
