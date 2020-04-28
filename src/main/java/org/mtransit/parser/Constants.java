package org.mtransit.parser;

import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("WeakerAccess")
public final class Constants {

	public static boolean DEBUG = false;
	// public static boolean DEBUG = true; // DEBUG

	public static boolean LOG_SQL = false;
	// public static boolean LOG_SQL = true; // DEBUG

	public static boolean LOG_SQL_UPDATE = false;
	// public static boolean LOG_SQL_UPDATE = true; // DEBUG

	public static boolean LOG_SQL_QUERY = false;
	// public static boolean LOG_SQL_QUERY = true; // DEBUG

	public static final char NEW_LINE = '\n';

	public static final char SPACE = ' ';

	public static final char COLUMN_SEPARATOR = ',';

	public static final char STRING_DELIMITER = '\'';

	public static final String EMPTY = StringUtils.EMPTY;

	public static final String UUID_SEPARATOR = "-";

}
