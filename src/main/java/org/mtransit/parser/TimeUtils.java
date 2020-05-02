package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@SuppressWarnings("WeakerAccess")
public final class TimeUtils {

	private static final int PRECISION_IN_SECONDS = 10;

	@Nullable
	public static Integer cleanExtraSeconds(@Nullable Integer time) {
		int extraSeconds = time == null ? 0 : time % PRECISION_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			return cleanTime(time, extraSeconds);
		}
		return time; // GTFS standard
	}

	private static final String CLEAN_TIME_FORMAT = "%02d";
	private static final String CLEAN_TIME_LEADING_ZERO = "0";
	private static final String CLEAN_TIME_DEFAULT_MINUTES = "00";
	private static final String CLEAN_TIME_DEFAULT_SECONDS = "00";

	private static String cleanTime(int time) {
		return String.format(Locale.ENGLISH, CLEAN_TIME_FORMAT, time);
	}

	public static int cleanTime(Integer time, int extraSeconds) {
		try {
			String timeS = convertTimeToString(time);
			String newHours = timeS.substring(0, 2);
			String newMinutes = timeS.substring(2, 4);
			String newSeconds = timeS.substring(4, 6);
			int seconds = Integer.parseInt(newSeconds);
			if (extraSeconds < 5) {
				if (extraSeconds > seconds) {
					newSeconds = CLEAN_TIME_DEFAULT_SECONDS;
				} else {
					newSeconds = cleanTime(seconds - extraSeconds);
				}
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			int secondsToAdd = PRECISION_IN_SECONDS - extraSeconds;
			if (seconds + secondsToAdd < 60) {
				newSeconds = cleanTime(seconds + secondsToAdd);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newSeconds = CLEAN_TIME_DEFAULT_SECONDS;
			int minutes = Integer.parseInt(newMinutes);
			if (minutes + 1 < 60) {
				newMinutes = cleanTime(minutes + 1);
				return Integer.parseInt(newHours + newMinutes + newSeconds);
			}
			newMinutes = CLEAN_TIME_DEFAULT_MINUTES;
			newHours = String.valueOf(Integer.parseInt(newHours) + 1);
			return Integer.parseInt(newHours + newMinutes + newSeconds);
		} catch (Exception e) {
			MTLog.logFatal(e, "Error while cleaning time '%s' '%s' !\n", time, extraSeconds);
			return -1;
		}
	}

	public static String convertTimeToString(Integer time) {
		StringBuilder sb = new StringBuilder(time.toString());
		while (sb.length() < 6) {
			sb.insert(0, CLEAN_TIME_LEADING_ZERO);
		}
		return sb.toString();
	}
}
