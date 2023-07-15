package pl.edu.agh.aolesek.bts.trajectory.generator.google.data.provider;

import java.util.List;

import lombok.Data;

//struktura z informacjami zwracanymi przez Google
@Data
public class GoogleDetailsModel {

	private Result result;

	@Data
	static class Result {

		private final String name;

		private final OpeningHours opening_hours;

		private final Integer price_level;
	}

	@Data
	static class OpeningHours {

		private final List<PlaceOpeningHoursPeriod> periods;

	}

	@Data
	static class PlaceOpeningHoursPeriod {

		private final PlaceOpeningHoursTime open;

		private final PlaceOpeningHoursTime close;
	}

	@Data
	static class PlaceOpeningHoursTime {

		private final String time;
	}
}
