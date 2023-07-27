package pl.edu.agh.aolesek.bts.trajectory.generator.profile.generator;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;

import java.util.Collection;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

//sprawdza poprawność wygenerowanych profilów
public class ProfileValidator {

	public static void validate(IProfile profile) {
		if (isBlank(profile.getId())) {
			throwIllegalState("Profile ID is blank!", profile);
		}
		if (isEmpty(profile.getInterests())) {
			throwIllegalState("There should be at least 1 interest in profile!", profile);
		}
		if (profile.getMaterialStatus() == null) {
			throwIllegalState("Material status cannot be null!", profile);
		}
		if (profile.getMaxRange() < 0 || profile.getMaxRange() > 10000) {
			throwIllegalState("Max range should be (0, 10000)!", profile);
		}
		if (isEmpty(profile.getPrefferedTransportModes())) {
			throwIllegalState("Preferred transport modes should not be empty!", profile);
		}

		if (profile.getPlaceOfDeparture() == null) {
			throwIllegalState("Place of departure cannot be null!", profile);
		}
		if (profile.getPreferences() == null) {
			throwIllegalState("Profile preferences cannot be null!", profile);
		}
	}

	static void throwIllegalState(String message, IProfile profile) {
		throw new IllegalStateException(String.format(message + " [%s]", profile));
	}

	static boolean isEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	static boolean isEmpty(Map<?, ?> map) {
		return map == null || map.isEmpty();
	}
}
