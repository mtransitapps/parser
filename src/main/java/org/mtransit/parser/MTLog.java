package org.mtransit.parser;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class MTLog {

	public static void logDebug(Character character) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.print(character);
	}

	public static void logDebug(String format, Object... args) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.printf("\n" + format, args);
	}

	public static void logDebugMethodCall(String methodName, Object... args) {
		if (!Constants.DEBUG) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		sb.append("----------");
		sb.append(methodName).append("(");
		if (args != null && args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				sb.append("%s");
			}
		}
		sb.append(")");
		System.out.printf(sb.toString(), args);
	}

	public static void logDebugMethodEnd(String methodName) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.print("\n" + "----------" + methodName + "()");
	}

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
