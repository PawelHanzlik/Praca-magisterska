package pl.edu.agh.aolesek.bts.trajectory.analysis;

import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

import java.util.List;

@Data
public class ProfileResultHolder {

    List<ProfileResult> results;
}
