package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

import java.time.LocalDateTime;

//interfejs z metodą zwracają informację o tym czy Poi jest w danej chwili otwarte
public interface IOpeningHoursSupplier {

	boolean isOpen(IProfile profile, IPoi poi, LocalDateTime time);
}
