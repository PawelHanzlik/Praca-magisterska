package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import java.util.Collection;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.PoiHolder;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

//interfejs pobierający i przetwarzający obiekty Poi
public interface IPoiGenerator<T extends IPoi> {

    Collection<PoiHolder<T>> generatePois(IProfile profile);

    default void logStats() {
        // do nothing by default
    }
}
