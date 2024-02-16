package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused", "WeakerAccess"})
public final class MTLog {

	private static final Character POINT = '.';

	public static void logDebug(@NotNull Character character) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.print(character);
	}

	public static void logDebug(@NotNull String format, @Nullable Object... args) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.printf("\n" + format, args);
	}

	public static void logDebugMethodCall(@NotNull String methodName, @Nullable Object... args) {
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

	public static void logDebugMethodEnd(@NotNull String methodName) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.print("\n" + "----------" + methodName + "()");
	}

	public static void logDebugMethodEnd(@NotNull String methodName, @Nullable Object result) {
		if (!Constants.DEBUG) {
			return;
		}
		System.out.printf("\n" + "----------" + methodName + "() > %s.", result);
	}

	public static void logPOINT() {
		logChar(POINT);
	}

	public static void logChar(@NotNull Character character) {
		System.out.print(character);
	}

	public static void logChar(@NotNull String string) {
		System.out.print(string);
	}

	public static void log(boolean debug, @NotNull String format, @Nullable Object... args) {
		if (debug) {
			logDebug(format, args);
		} else {
			log(format, args);
		}
	}

	public static void log(@NotNull String format, @Nullable Object... args) {
		System.out.printf("\n" + format, args);
	}

	public static void logNonFatal(@NotNull Throwable t) {
		logNonFatal(t, t.getMessage());
	}

	public static void logNonFatal(@NotNull Throwable t, @NotNull String format, @Nullable Object... args) {
		System.out.printf("\n" + format, args);
		t.printStackTrace(); // NON-FATAL
	}

	@Deprecated
	public static void logFatal(@NotNull String format, @Nullable Object... args) {
		log("FATAL ERROR: \n" + format + "\n", args);
		System.exit(-1);
	}

	@Deprecated
	public static void logFatal(@NotNull Throwable t, @NotNull String format, @Nullable Object... args) {
		log("FATAL ERROR: \n" + format + "\n", args);
		t.printStackTrace();
		System.exit(-1);
	}

	public static void waitForEnter() {
		// if (true) return;
		if (DefaultAgencyTools.IS_CI) {
			return;
		}
		System.out.println("\nPress Enter to continue...");
		try {
			//noinspection ResultOfMethodCallIgnored
			System.in.read(new byte[2]); // requires run { standardInput = System.in } in build.gradle
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while waiting for Enter!");
		}
	}

	public static class Fatal extends RuntimeException {
		public Fatal(@NotNull String format, @Nullable Object... args) {
			super();
			log("FATAL ERROR: \n" + format + "\n", args);
			System.exit(-1);
		}

		public Fatal(@NotNull Throwable t, @NotNull String format, @Nullable Object... args) {
			super();
			log("FATAL ERROR: \n" + format + "\n", args);
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
