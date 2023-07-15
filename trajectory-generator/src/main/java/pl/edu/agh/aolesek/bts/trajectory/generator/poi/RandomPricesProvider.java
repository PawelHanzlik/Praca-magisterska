package pl.edu.agh.aolesek.bts.trajectory.generator.poi;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.Level;

/**
 * Provides random prices for GIS providers that do not provide info about average prices of POIs.
 */
//dostarcza losowe poziomy cenowe jeśli usługa udostępniająca obiekty Poi nie zwraca tego typu danych, wykorzystany jest interfejs IPricesProvider
public class RandomPricesProvider implements IPricesProvider {

	final Random rand = new Random();

	final Map<IPoi, Level> poiRandomPrices = new HashMap<>();

	@Override
	public synchronized Level resolvePriceLevel(IPoi poi) {
		return poiRandomPrices.computeIfAbsent(poi, key -> randomLevel());
	}

	private Level randomLevel() {
		final Level[] levels = Level.values();
		final Level randomLevel = levels[rand.nextInt(levels.length)];
		return randomLevel;
	}
}
