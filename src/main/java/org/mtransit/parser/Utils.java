package org.mtransit.parser;

import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class Utils {

	private Utils() {
	}

	private static final long MILLIS_PER_MILLIS = 1;
	private static final long MILLIS_PER_SECOND = 1000;
	private static final long SECONDS_PER_MINUTE = 60;
	private static final long MINUTES_PER_HOUR = 60;
	private static final long MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND;
	private static final long MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE;
	private static final String H = "h";
	private static final String M = "m";
	private static final String S = "s";
	private static final String MS = "ms";

	public static String getPrettyDuration(long durationInMs) {
		StringBuilder sb = new StringBuilder();
		long ms = durationInMs / MILLIS_PER_MILLIS % MILLIS_PER_SECOND;
		durationInMs -= ms * MILLIS_PER_MILLIS;
		long s = durationInMs / MILLIS_PER_SECOND % SECONDS_PER_MINUTE;
		durationInMs -= s * MILLIS_PER_SECOND;
		long m = durationInMs / MILLIS_PER_MINUTE % MINUTES_PER_HOUR;
		durationInMs -= m * MILLIS_PER_MINUTE;
		long h = durationInMs / MILLIS_PER_HOUR;
		boolean printing = false;
		//noinspection ConstantConditions
		if (printing || h > 0) {
			printing = true;
			if (sb.length() > 0) {
				sb.append(Constants.SPACE);
			}
			sb.append(h).append(H);
		}
		if (printing || m > 0) {
			printing = true;
			if (sb.length() > 0) {
				sb.append(Constants.SPACE);
			}
			sb.append(m).append(M);
		}
		if (printing || s > 0) {
			//noinspection UnusedAssignment
			printing = true;
			if (sb.length() > 0) {
				sb.append(Constants.SPACE);
			}
			sb.append(s).append(S);
		}
		if (sb.length() > 0) {
			sb.append(Constants.SPACE);
		}
		sb.append(ms).append(MS);
		return sb.toString();
	}

	public static String replaceAll(String string, Pattern[] patterns, String replacement) {
		if (string == null || string.length() == 0) {
			return string;
		}
		if (patterns != null) {
			for (Pattern pattern : patterns) {
				string = pattern.matcher(string).replaceAll(replacement);
			}
		}
		return string;
	}

	// from the Android Open Source Project by Google
	public static boolean isDigitsOnly(CharSequence str) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isDigit(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isLettersOnly(CharSequence str) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isLetter(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isLettersOnly(CharSequence str, boolean allowWhitespace) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isLetter(str.charAt(i))) {
				if (!allowWhitespace || !Character.isWhitespace(str.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}

	public static boolean isUppercaseOnly(CharSequence str, boolean allowWhitespace, boolean checkAZonly) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (checkAZonly && !Character.isAlphabetic(str.charAt(i))) {
				continue;
			}
			if (!Character.isUpperCase(str.charAt(i))) {
				if (!allowWhitespace || !Character.isWhitespace(str.charAt(i))) {
					return false;
				}
			}
		}
		return true;
	}
}
