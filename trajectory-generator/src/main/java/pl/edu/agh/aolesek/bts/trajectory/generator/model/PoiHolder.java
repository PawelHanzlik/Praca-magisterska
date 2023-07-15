package pl.edu.agh.aolesek.bts.trajectory.generator.model;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;

//informacje o POI
@Data
public class PoiHolder<T extends IPoi> {

	@JsonIgnore
	private transient final IProfile profile;

	private final IPoi poi;

	private final Map<String, Object> reason;

	public static <T extends IPoi> PoiHolder<T> create(IProfile a, T b, Map<String, Object> c) {
		return new PoiHolder<T>(a, b, c);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PoiHolder)) {
			return false;
		}
		PoiHolder<?> other = (PoiHolder<?>) obj;
		return Objects.equals(poi, other.poi) && Objects.equals(profile, other.profile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(poi, profile);
	}

	@Override
	public String toString() {
		return String.format("PoiHolder[%s(%.3f, %.3f) %s]", poi.getName(), poi.getLat(), poi.getLon(), profile.getFullName());
	}
}
