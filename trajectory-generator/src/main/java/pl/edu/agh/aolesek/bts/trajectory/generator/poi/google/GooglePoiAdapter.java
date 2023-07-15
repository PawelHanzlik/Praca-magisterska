package pl.edu.agh.aolesek.bts.trajectory.generator.poi.google;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import lombok.RequiredArgsConstructor;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.google.GooglePoiModel.GoogleModelElement;

//zbiór metod dla generatora
@RequiredArgsConstructor
public class GooglePoiAdapter implements IPoi {

	private final GoogleModelElement googlePoi;

	private final String foundBecause;

	private long averageSecondsSpent;

	@SuppressWarnings("unused") // For debug purposes
	private final String searchedUsing;

	@Override
	public String getId() {
		return googlePoi.getPlace_id();
	}

	@Override
	public String getName() {
		return googlePoi.getName() + "(" + getCategory() + ")";
	}

	@Override
	public String getCategory() {
		return foundBecause;
	}

	@Override
	public double getLat() {
		return googlePoi.getGeometry().getLocation().getLat();
	}

	@Override
	public double getLon() {
		return googlePoi.getGeometry().getLocation().getLng();
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
		double rating = googlePoi.getRating();
		String vicinity = googlePoi.getVicinity();
		List<String> types = googlePoi.getTypes();
		return ImmutableMap.of("rating", rating, "vicinity", vicinity, "types", types);
	}
}
