package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.ors;

import com.google.inject.Inject;
import mil.nga.sf.geojson.FeatureCollection;
import mil.nga.sf.geojson.FeatureConverter;
import mil.nga.sf.geojson.Geometry;
import mil.nga.sf.geojson.LineString;
import okhttp3.ResponseBody;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.AbstractRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.RoutingForPart;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

//klasa wywołująca zapytania, wyznaczająca czas pokonywania odcinka, konwertująca współrzędne geograficzne na typ tekstowy
// i wypakowująca dane z informacji zwrotnych od API
public class OpenRouteServicePoiRouter extends AbstractRouter {

	private static final String DEFAULT_TRANSPORT_MODE = "foot-walking";

	private final OpenRouteServiceApi api;

	@Inject
	public OpenRouteServicePoiRouter(Config config) {
		super(config);
		this.api = RetrofitClientFactory.getRetrofitInstanceRaw(config.get(Parameters.ORS_URL)).create(OpenRouteServiceApi.class);
	}

	//modyfikacja czasu pokonywania odcinka
	@Override
	protected double modifyDuration(IProfile profile, String transportMode, double totalDuration) {
		if (defaultMode().equals(transportMode)) {
			final double walkingSpeedModifier = profile.getPreferences().getWalkingSpeedModifier();
			return totalDuration * walkingSpeedModifier;
		}
		return totalDuration;
	}

	@Override
	protected String defaultMode() {
		return DEFAULT_TRANSPORT_MODE;
	}

	@Override
	protected String checkTransportMode(String transportMode)
	{
		//konwersja nazw środków transportu z myślą o krzyżowaniu usług
		switch(transportMode){
			case "driving":
				return "driving";
			case "bicycling":
				return "cycling-regular"; //domyślny rodzaj podróży rowerem
			case "transit":
				return "driving"; //z powodu braku transportu zbiorowego w ORS zamieniam na transport samochodowy
			case "walking":
				return "foot-walking";
			default:
				return "correct";
		}
	}

	//tworzenie zapytania
	@Override
	protected RoutingForPart queryExternalProvider(Point from, Point to, String transportMode) throws IOException {



		final ResponseBody content = api
				.route(transportMode, resolveApiKey(), poiToString(from), poiToString(to))
				.blockingGet();
		return extractRoutingFromExternalService(FeatureConverter.toFeatureCollection(content.string()));
	}

	public String resolveApiKey() {
		final String key = config.get(Parameters.ORS_KEY);
		return key == null ? "" : key;
	}

	//wypakowywanie danych z odpowiedzi na zapytanie
	@SuppressWarnings("unchecked")
	private RoutingForPart extractRoutingFromExternalService(FeatureCollection featureCollection) {
		final Map<String, Object> summary = (Map<String, Object>) featureCollection.getFeature(0).getProperties().get("summary");
		final Geometry geometry = featureCollection.getFeature(0).getGeometry();
		try {
			final double duration = (double) summary.get("duration");
			final double distance = (double) summary.get("distance");
			return new RoutingForPart(duration, distance, (LineString) geometry);
		} catch (NullPointerException e) {
			return new RoutingForPart(1, 1, (LineString) geometry);
		}
	}

	@Override
	protected String poiToString(Point point) {
		final double lat = point.getLat();
		final double lon = point.getLon();
		return String.format(Locale.ROOT, "%f,%f", lon, lat);
	}
}
