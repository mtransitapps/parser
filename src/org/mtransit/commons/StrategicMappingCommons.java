package org.mtransit.commons;

import org.mtransit.parser.mt.data.MDirectionType;

public final class StrategicMappingCommons {

	// TRIP DIRECTION ID USED BY REAL-TIME API

	public static final int CLOCKWISE = 0;
	public static final int CLOCKWISE_0 = 0;
	public static final int CLOCKWISE_1 = 1;

	public static final int COUNTERCLOCKWISE = 1;
	public static final int COUNTERCLOCKWISE_0 = 0;
	public static final int COUNTERCLOCKWISE_1 = 1;

	public static final int INBOUND = 0;

	public static final int OUTBOUND = 1;
	public static final int OUTBOUND_0 = 10;
	public static final int OUTBOUND_1 = 11;

	public static final int EAST = MDirectionType.EAST.intValue();
	public static final int WEST = MDirectionType.WEST.intValue();
	public static final int NORTH = MDirectionType.NORTH.intValue();
	public static final int SOUTH = MDirectionType.SOUTH.intValue();

}
