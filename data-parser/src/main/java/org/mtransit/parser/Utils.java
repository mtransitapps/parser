package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.RegexUtils;

import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused", "RedundantSuppression"})
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

	@NotNull
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

	@Deprecated
	@Nullable
	public static String replaceAll(@Nullable String string, @Nullable Pattern[] patterns, @NotNull String replacement) {
		return RegexUtils.replaceAll(string, patterns, replacement);
	}

	@NotNull
	public static String replaceAllNN(@NotNull String string, @NotNull Pattern[] patterns, @NotNull String replacement) {
		return RegexUtils.replaceAllNN(string, patterns, replacement);
	}

	@Deprecated
	public static boolean isDigitsOnly(@NotNull CharSequence str) {
		return CharUtils.isDigitsOnly(str);
	}

	public static boolean isLettersOnly(@NotNull CharSequence str) {
		final int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!Character.isLetter(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean isLettersOnly(@NotNull CharSequence str, boolean allowWhitespace) {
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

	@Deprecated
	public static boolean isUppercaseOnly(@NotNull String str, boolean allowWhitespace, boolean checkAZOnly, @NotNull String... excludedWords) {
		return isUppercaseOnly(
				org.mtransit.commons.CleanUtils.cleanWords(excludedWords)
						.matcher(str).replaceAll(
						org.mtransit.commons.CleanUtils.cleanWordsReplacement(Constants.EMPTY)
				),
				allowWhitespace, checkAZOnly);
	}

	@Deprecated
	public static boolean isUppercaseOnly(@NotNull CharSequence str, boolean allowWhitespace, boolean checkAZOnly) {
		return CharUtils.isUppercaseOnly(str, allowWhitespace, checkAZOnly);
	}

	@Deprecated
	public static boolean isRomanDigits(@NotNull CharSequence str) {
		return CharUtils.isRomanDigits(str);
	}
}
