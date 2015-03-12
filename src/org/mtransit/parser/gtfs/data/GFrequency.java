package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#frequencies_fields
public class GFrequency {

	public static final String FILENAME = "frequencies.txt";

	public static final String TRIP_ID = "trip_id";
	public String trip_id;
	public static final String START_TIME = "start_time";
	public String start_time;
	public static final String END_TIME = "end_time";
	public String end_time;
	public static final String HEADWAY_SECS = "headway_secs";
	public String headway_secs;

	public GFrequency(String trip_id, String start_time, String end_time, String headway_secs) {
		this.trip_id = trip_id;
		this.start_time = start_time;
		this.end_time = end_time;
		this.headway_secs = headway_secs;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(trip_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(start_time).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(end_time).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(headway_secs).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}
}
