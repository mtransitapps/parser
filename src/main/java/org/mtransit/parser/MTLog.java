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

	public static void logPOINT() {
		log(POINT);
	}

	public static void log(@NotNull Character character) {
		System.out.print(character);
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

	public static void logFatal(@NotNull String format, @Nullable Object... args) {
		log("FATAL ERROR: " + format + "\n", args);
		System.exit(-1);
	}

	public static void logFatal(@NotNull Throwable t, @NotNull String format, @Nullable Object... args) {
		log("FATAL ERROR: " + format + "\n", args);
		t.printStackTrace();
		System.exit(-1);
	}

	public static class Fatal extends RuntimeException {
		public Fatal(@NotNull String format, @Nullable Object... args) {
			super();
			log("FATAL ERROR: " + format + "\n", args);
			System.exit(-1);
		}

		public Fatal(@NotNull Throwable t, @NotNull String format, @Nullable Object... args) {
			super();
			log("FATAL ERROR: " + format + "\n", args);
			t.printStackTrace();
			System.exit(-1);
		}
	}
}
