package org.mtransit.parser;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class MTLog {

	public static void log(Character character) {
		System.out.print(character);
	}

	public static void log(String format, Object... args) {
		System.out.printf("\n" + format, args);
	}

	public static void logNonFatal(Throwable t) {
		logNonFatal(t, t.getMessage());
	}

	public static void logNonFatal(Throwable t, String format, Object... args) {
		System.out.printf("\n" + format, args);
		t.printStackTrace(); // NON-FATAL
	}

	public static void logFatal(String format, Object... args) {
		log("ERROR: " + format + "\n", args);
		System.exit(-1);
	}

	public static void logFatal(Throwable t, String format, Object... args) {
		log("ERROR: " + format + "\n", args);
		t.printStackTrace();
		System.exit(-1);
	}
}
