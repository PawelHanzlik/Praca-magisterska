package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import java.time.LocalDateTime;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//interfejs z metodą zwracają informację o tym czy Poi jest w danej chwili otwarte
public interface IOpeningHoursSupplier {

	boolean isOpen(IProfile profile, IPoi poi, LocalDateTime time);
}
