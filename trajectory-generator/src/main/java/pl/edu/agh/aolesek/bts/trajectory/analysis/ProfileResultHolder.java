package pl.edu.agh.aolesek.bts.trajectory.analysis;

import java.util.List;

import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.core.ProfileResult;

@Data
public class ProfileResultHolder {

    List<ProfileResult> results;
}
