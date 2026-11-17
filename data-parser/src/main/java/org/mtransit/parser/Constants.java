package org.mtransit.parser;

@SuppressWarnings({"WeakerAccess", "unused", "RedundantSuppression"})
public final class Constants {

	public static boolean DEBUG;
	static {
		// DEBUG = false;
		DEBUG = org.mtransit.commons.Constants.getDEBUG(); // DEBUG
		// DEBUG = true; // DEBUG
	}

	public static boolean LOG_SQL = false;
	// public static boolean LOG_SQL = true; // DEBUG

	public static boolean LOG_SQL_UPDATE = false;
	// public static boolean LOG_SQL_UPDATE = true; // DEBUG

	public static boolean LOG_SQL_QUERY = false;
	// public static boolean LOG_SQL_QUERY = true; // DEBUG

	public static boolean SKIP_FILE_DUMP = false;
	// public static boolean SKIP_FILE_DUMP = true; // DEBUG

	public static final char NEW_LINE = '\n';

	public static final char SPACE = ' ';

	public static final String EMPTY = "";

	public static final String SPACE_ = " ";

}
