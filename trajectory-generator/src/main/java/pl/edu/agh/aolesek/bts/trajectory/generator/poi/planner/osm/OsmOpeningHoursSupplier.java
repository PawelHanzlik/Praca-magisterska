package pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.osm;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.edu.agh.aolesek.bts.trajectory.generator.model.profile.IProfile;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.IPoi;
import pl.edu.agh.aolesek.bts.trajectory.generator.poi.planner.IOpeningHoursSupplier;

//wyznaczanie godzin otwarcia
public class OsmOpeningHoursSupplier implements IOpeningHoursSupplier {

	private final Map<String, Integer> weekdays;

	private final Pattern pattern = Pattern.compile("[A-z][a-z](-[A-z][a-z])+ (.*)");

	public OsmOpeningHoursSupplier() {
		weekdays = new HashMap<>();
		weekdays.put("Mo", 1);
		weekdays.put("Tu", 2);
		weekdays.put("We", 3);
		weekdays.put("Th", 4);
		weekdays.put("Fr", 5);
		weekdays.put("Sa", 6);
		weekdays.put("Su", 7);
	}

	@Override
	public boolean isOpen(IProfile profile, IPoi poi, LocalDateTime time) {
		final Object openingHours = poi.getAdditionalProperties().get("opening_hours");
		if (openingHours == null) {
			return true;
		}
		profile.getLogger().info(String.format("Found opening hours for %s (%s)", poi.getName(), String.valueOf(openingHours)));
		return checkIfOpened(profile, poi, String.valueOf(openingHours), time);

	}

	private boolean checkIfOpened(IProfile profile, IPoi poi, String openingHours, LocalDateTime time) {
		if (openingHours.matches("24/7")) {
			return true;
		}
		final List<String> values = Arrays.asList(openingHours.split(";"));
		return values.stream().anyMatch(value -> checkIfOpenedForSignleValue(profile, poi, value, time));
	}

	private boolean checkIfOpenedForSignleValue(IProfile profile, IPoi poi, String singleValue, LocalDateTime time) {
		try {
			int dayToCheck = time.getDayOfWeek().getValue(); // 1 - monday, 7 - sunday

			if (singleValue.trim().matches("^[A-Z][a-z] .*")) {
				return singleDay(singleValue.trim(), time, dayToCheck);
			} else if (singleValue.trim().matches("^[A-Z][a-z]-[A-Z][a-z] .*")) {
				return dayRange(singleValue.trim(), time, dayToCheck);
			} else if (singleValue.trim().matches("^[A-Z][a-z], ?[A-Z][a-z] .*")) {
				return multipleDays(singleValue.trim(), time, dayToCheck);
			}
			throw new IllegalArgumentException();
		} catch (RuntimeException e) {
			profile.getLogger().info(String.format("Unable to parse part of opening hours for %s (%s)", poi.getName(), singleValue));
			return true;
		}
	}

	private boolean singleDay(String singleValue, LocalDateTime time, int dayToCheck) {
		int weekday = parseWeekday(singleValue);
		if (weekday == dayToCheck) {
			return checkTime(singleValue, time);
		} else {
			return false;
		}
	}

	private boolean dayRange(String singleValue, LocalDateTime time, int dayToCheck) {
		int weekdayFrom = parseWeekday(singleValue.substring(0, 2));
		int weekdayTo = parseWeekday(singleValue.substring(3, 5));
		if (dayToCheck >= weekdayFrom && dayToCheck <= weekdayTo) {
			return checkTime(singleValue, time);
		} else {
			return false;
		}
	}

	private boolean multipleDays(String singleValue, LocalDateTime time, int dayToCheck) {
		int weekdayFrom = parseWeekday(singleValue.substring(0, 2));
		int weekdayTo = parseWeekday(singleValue.substring(3, 5));
		if (dayToCheck >= weekdayFrom && dayToCheck <= weekdayTo) {
			return checkTime(singleValue, time);
		} else {
			return false;
		}
	}

	private int parseWeekday(String weekday) {
		for (Map.Entry<String, Integer> d : weekdays.entrySet()) {
			if (weekday.startsWith(d.getKey())) {
				return d.getValue();
			}
		}
		throw new IllegalArgumentException();
	}

	private boolean checkTime(String singleValue, LocalDateTime time) {
		final Matcher m = pattern.matcher(singleValue);
		if (m.find()) {
			final String timeValue = m.group(2);
			final String[] split = timeValue.split("-");

			final int fromHour = Integer.parseInt(split[0].split(":")[0]);
			final int fromMin = Integer.parseInt(split[0].split(":")[1]);
			final int toHour = Integer.parseInt(split[1].split(":")[0]);
			final int toMin = Integer.parseInt(split[1].split(":")[1]);

			int hour = time.toLocalTime().getHour();
			int min = time.toLocalTime().getMinute();

			int checkedTime = hour * 60 + min;
			int start = fromHour * 60 + fromMin;
			int end = toHour * 60 + toMin;
			if (start <= checkedTime && checkedTime <= end) {
				return true;
			}
			return false;
		}
		return false;
	}
}
