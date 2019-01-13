package org.mtransit.commons;

import org.mtransit.parser.mt.data.MDirectionType;

public final class OneBusAwayCommons {

	// TRIP DIRECTION ID USED BY REAL-TIME API

	public static final int MORNING = 11;
	public static final int AFTERNOON = 13;

	public static final int EAST = MDirectionType.EAST.intValue();
	public static final int WEST = MDirectionType.WEST.intValue();
	public static final int NORTH = MDirectionType.NORTH.intValue();
	public static final int SOUTH = MDirectionType.SOUTH.intValue();

	public static final int EAST_SPLITTED_CIRCLE = 1000;
	public static final int WEST_SPLITTED_CIRCLE = 2000;

	public static final int NORTH_SPLITTED_CIRCLE = 3000;
	public static final int SOUTH_SPLITTED_CIRCLE = 4000;

}
