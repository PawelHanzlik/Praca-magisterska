package pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import com.google.inject.Inject;

import lombok.Data;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Config;
import pl.edu.agh.aolesek.bts.trajectory.generator.app.Parameters;
import pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider.GoogleDetailsModel.PlaceOpeningHoursPeriod;
import pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider.GoogleDetailsModel.Result;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;
import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPricesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.ISpentTimeProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.RandomPricesProvider;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.google.PlacesApi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;
import pl.edu.agh.aolesek.bts.trajectory.generator.retrofit.RetrofitClientFactory;

//obsługa informacji od Google
public class GoogleAdditionalDataProvider implements IPricesProvider, ISpentTimeProvider, IOpeningHoursSupplier {

	private final PlacesApi api;

	private final String placesApiKey;

	private final Map<String, PlaceDetails> detailsCache = new HashMap<>();

	private final IPricesProvider randomPricesProvider = new RandomPricesProvider();

	@Inject
	public GoogleAdditionalDataProvider(Config config) {
		this.placesApiKey = config.get(Parameters.GOOGLE_API_KEY); //pobranie klucza do API
		api = RetrofitClientFactory.getRetrofitInstance(config.get(Parameters.GOOGLE_API_URL)).create(PlacesApi.class);
	}

	//sprawdzanie czy odwiedzane miejsce jest otwarte, pobranie godzin otwarcia
	@Override
	public boolean isOpen(IProfile profile, IPoi poi, LocalDateTime time) {
		final BiFunction<IPoi, LocalDateTime, Optional<Boolean>> isOpenFunction = getDetails(poi).getIsOpenChecker();
		final Optional<Boolean> isOpen = isOpenFunction.apply(poi, time);
		if (isOpen.isPresent()) {
			profile.getLogger().info(String.format("Found opening hours for %s ", poi.getName()));
			return isOpen.get();
		}
		return true;
	}

	//Google nie daje tylu informacji co usługi oparte o OSM
	@Override
	public long resolveSecondsSpent(IPoi poi) {
		throw new UnsupportedOperationException("Google Mapis API does not support this feature yet.");
	}

	//sprawdzenie cen
	@Override
	public Level resolvePriceLevel(IPoi poi) {
		return getDetails(poi).getPriceLevel();
	}

	private synchronized PlaceDetails getDetails(IPoi poi) {
		return detailsCache.computeIfAbsent(poi.getId(), poiId -> queryForDetails(poiId, poi));
	}

	private PlaceDetails queryForDetails(String placeId, IPoi poi) {
		return api.details(placeId, "name,price_level,opening_hours", placesApiKey)
				.map(result -> extractDetails(result, poi))
				.onErrorReturn(e -> generateDetails(placeId, poi))
				.blockingGet();
	}

	private PlaceDetails extractDetails(GoogleDetailsModel googleDetailsModel, IPoi poi) {
		final Result result = googleDetailsModel.getResult();

		final Integer priceLevel = result.getPrice_level();
		final Level finalPriceLevel = priceLevel != null ? Level.values()[priceLevel] : randomPricesProvider.resolvePriceLevel(poi);

		final BiFunction<IPoi, LocalDateTime, Optional<Boolean>> isOpenChecker = resolveOpenChecker(result, poi);
		return new PlaceDetails(isOpenChecker, 0L, finalPriceLevel);
	}

	private final BiFunction<IPoi, LocalDateTime, Optional<Boolean>> resolveOpenChecker(Result result, IPoi poi) {
		if (result.getOpening_hours() == null) {
			return (a, b) -> Optional.empty();
		}
		List<PlaceOpeningHoursPeriod> periods = result.getOpening_hours().getPeriods();

		return (poiToCheck, time) -> {
			try {
				if (periods.size() == 1 && periods.iterator().next().getClose() == null) {
					return Optional.of(true);
				}
				int dayOfWeek = time.getDayOfWeek().getValue() - 1;
				PlaceOpeningHoursPeriod period = periods.get(dayOfWeek);

				Long open = Long.valueOf(period.getOpen().getTime());
				Long close = Long.valueOf(period.getClose().getTime());

				Long timeToCheck = (long) (time.getHour() * 100 + time.getMinute());

				return Optional.of(timeToCheck >= open && timeToCheck <= close);
			} catch (RuntimeException e) {
				return Optional.empty();
			}
		};
	}

	private PlaceDetails generateDetails(String placeId, IPoi poi) {
		final Level priceLevel = randomPricesProvider.resolvePriceLevel(poi);
		final BiFunction<IPoi, LocalDateTime, Optional<Boolean>> isOpenChecker = (p, time) -> Optional.empty();
		return new PlaceDetails(isOpenChecker, 0L, priceLevel);
	}

	@Data
	static class PlaceDetails {

		final BiFunction<IPoi, LocalDateTime, Optional<Boolean>> isOpenChecker;

		final Long averageSecondsSpent;

		final Level priceLevel;
	}
}
