package pl.edu.agh.aolesek.bts.trajectory.generator.poi.google;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import lombok.extern.log4j.Log4j2;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.AbstractPoiGenerator;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPricesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.ISpentTimeProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.recommender.IPoiRecommender;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

//klasy odpowiedzialne za Google Maps Places API
@Log4j2
public class GooglePoiGenerator extends AbstractPoiGenerator<GooglePoiAdapter> {

    private final PlacesApi api;

    private final String placesApiKey;

    private AtomicInteger requestsCount = new AtomicInteger(0);

    private final ISpentTimeProvider spentTimeProvider;

    private final IPricesProvider pricesProvider;

    //konstruktor
    @Inject
    public GooglePoiGenerator(Config parameters, ISpentTimeProvider spentTimeProvider, IPricesProvider pricesProvider,
        IPoiRecommender recommender) {
        super(parameters, recommender);
        this.placesApiKey = parameters.get(Parameters.GOOGLE_API_KEY);
        this.pricesProvider = pricesProvider;
        this.spentTimeProvider = spentTimeProvider;
        api = RetrofitClientFactory.getRetrofitInstance(parameters.get(Parameters.GOOGLE_API_URL)).create(PlacesApi.class);
    }

    //dodawanie do loggera informacji o liczbie zapytań przesłanych do Google
    @Override
    public void logStats() {
        log.info(String.format("Sent %d Google Places API requests.", requestsCount.get()));
    }

    //wyszukiwanie Poi
    @Override
    protected Collection<GooglePoiAdapter> searchPois(String category, IProfile profile) {
        requestsCount.getAndIncrement();
        final String location = createLocation(profile);
        return api.searchPois(location, (long)profile.getMaxRange(), category, placesApiKey)
            .map(GooglePoiModel::getResults)
            .onErrorReturn(throwable -> {
                profile.getLogger().info("Unable to contact places api", ImmutableMap.of("error", throwable));
                log.error("Unable to contact Places API", throwable);
                return null;
            })
            .filter(poi -> poi != null)
            .blockingGet()
            .stream()
            .map(googleModel -> new GooglePoiAdapter(googleModel, category,
                createDebugRequestInfo(location, (long)profile.getMaxRange(), category)))
            .collect(Collectors.toList());
    }

    //tworzenie pary współrzędnych tworzących punkt
    private String createLocation(IProfile profile) {
        return String.format(Locale.ROOT, "%f,%f", profile.getPlaceOfDeparture().getLat(), profile.getPlaceOfDeparture().getLon());
    }

    private String createDebugRequestInfo(String location, long maxRange, String keyword) {
        return String.format("/place/nearbysearch/json?location=%s&radius=%d&keyword=%s&key=?", location, maxRange, keyword);
    }

    //przetworzenie poziomu cenowego
    @Override
    protected Level resolvePriceLevel(IPoi poi) {
        return pricesProvider.resolvePriceLevel(poi);
    }

    //wyznaczenie czasu spędzanego w tym obiekcie
    @Override
    protected long resolveAverageSecondsSpent(GooglePoiAdapter poi) {
        return spentTimeProvider.resolveSecondsSpent(poi);
    }
}
