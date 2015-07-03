package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#stop_times_trip_id_field
// https://developers.google.com/transit/gtfs/reference#stop_times_stop_id_field
public class GTripStop {

	private String uid;

	public static final String TRIP_ID = "trip_id";
	private String trip_id;
	public static final String STOP_ID = "stop_id";
	private String stop_id;
	public static final String STOP_SEQUENCE = "stop_sequence";
	private int stop_sequence;

	public GTripStop(String uid, String trip_id, String stop_id, int stop_sequence) {
		this.uid = uid;
		this.trip_id = trip_id;
		this.stop_id = stop_id;
		this.stop_sequence = stop_sequence;
	}

	public String getUID() {
		return this.uid;
	}

	public String getTripId() {
		return this.trip_id;
	}

	public String getStopId() {
		return this.stop_id;
	}

	public int getStopSequence() {
		return this.stop_sequence;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.uid).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.trip_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_sequence).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}

	public static String getUID(String trip_uid, String stop_id, int stop_sequence) {
		return stop_id + Constants.UUID_SEPARATOR + stop_sequence + Constants.UUID_SEPARATOR + trip_uid;
	}
}
