package org.mtransit.parser;

public final class Utils {

	private Utils() {
	}

	private static final long MILLIS_PER_MILLIS = 1;
	private static final long MILLIS_PER_SECOND = 1000;
	private static final long SECONDS_PER_MINUTE = 60;
	private static final long MINUTES_PER_HOUR = 60;
	private static final long MILLIS_PER_MINUTE = SECONDS_PER_MINUTE * MILLIS_PER_SECOND;
	private static final long MILLIS_PER_HOUR = MINUTES_PER_HOUR * MILLIS_PER_MINUTE;

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
		if (printing || h > 0) {
			printing = true;
			sb.append(h).append("h ");
		}
		if (printing || m > 0) {
			printing = true;
			sb.append(m).append("m ");
		}
		if (printing || s > 0) {
			printing = true;
			sb.append(s).append("s ");
		}
		sb.append(ms).append("ms ");
		return sb.toString();
	}

}
