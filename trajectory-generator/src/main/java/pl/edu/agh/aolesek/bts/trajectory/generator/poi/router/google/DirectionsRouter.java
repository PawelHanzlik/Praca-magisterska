package pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.google;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.maps.model.EncodedPolyline;
import lombok.Data;
import mil.nga.sf.geojson.LineString;
import mil.nga.sf.geojson.Position;
import okhttp3.ResponseBody;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Point;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.AbstractRouter;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.router.RoutingForPart;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

//klasa wywołująca zapytania, wyznaczająca czas pokonywania odcinka, konwertująca współrzędne geograficzne
// na typ tekstowy i wypakowująca dane z informacji zwrotnych od API. Zawiera również kilka klas statycznych stanowiących kontenery z danymi
public class DirectionsRouter extends AbstractRouter {

	private static final String DEFAULT_TRANSPORT_MODE = "walking";

	private final DirectionsApi api;

	private final String apiKey;

	private final Gson gson;

	@Inject
	public DirectionsRouter(Config config) {
		super(config);
		this.apiKey = config.get(Parameters.GOOGLE_API_KEY);
		this.api = RetrofitClientFactory.getRetrofitInstanceRaw(config.get(Parameters.GOOGLE_API_URL)).create(DirectionsApi.class);
		this.gson = new Gson();
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
		switch(transportMode)
		{
			case "driving-car":
				return "driving";
			case "cycling-regular":
				return "bicycling";
			case "cycling-road":
				return "bicycling";
			case "foot-walking":
				return "walking";
			default:
				return "correct";
		}
	}

	//wywoływanie zapytania
	@Override
	protected RoutingForPart queryExternalProvider(Point from, Point to, String transportMode) throws IOException {



		final ResponseBody content = api
				.route(poiToString(from), poiToString(to), transportMode, apiKey)
				.blockingGet();
		/*
		final ResponseBody content = api
				.route(poiToString(from), poiToString(to), "TRANSIT", apiKey)
				.blockingGet();
		 */
		return extractRoutingFromExternalService(content.string());
	}

	//konwersja współrzędnych do stringa
	@Override
	protected String poiToString(Point point) {
		final double lat = point.getLat();
		final double lon = point.getLon();
		return String.format(Locale.ROOT, "%f,%f", lat, lon);
	}

	//wypakowywanie informacji
	private RoutingForPart extractRoutingFromExternalService(String content) {
		final DirectionsRouting routing = gson.fromJson(content, DirectionsRouting.class);
		final List<Route> routes = routing.getRoutes();

		if (routes.isEmpty() || routes.get(0).getLegs().isEmpty()) {
			throw new IllegalStateException("Received routing is empty or routing leg is empty!");
		}
		final Route route = routes.iterator().next();
		final Leg leg = route.legs.iterator().next();
		final List<Position> decodedLine = new EncodedPolyline(route.getOverview_polyline().getPoints()).decodePath()
				.stream()
				.map(latLng -> new Position(latLng.lng, latLng.lat))
				.collect(Collectors.toList());

		return new RoutingForPart(leg.getDuration().getValue(), leg.getDistance().getValue(),
				new LineString(decodedLine));
	}

	@Data
	static class DirectionsRouting {
		List<Route> routes;
	}

	@Data
	static class Route {
		List<Leg> legs;

		Line overview_polyline;
	}

	@Data
	static class Leg {
		Distance distance;

		Duration duration;
	}

	@Data
	static class Distance {
		long value;
	}

	@Data
	static class Duration {
		long value;
	}

	@Data
	static class Line {
		String points;
	}
}
