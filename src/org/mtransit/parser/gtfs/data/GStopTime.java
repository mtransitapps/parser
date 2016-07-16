package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#stop_times_fields
public class GStopTime {

	public static final String FILENAME = "stop_times.txt";

	public static final String TRIP_ID = "trip_id";
	private String trip_id;
	public static final String STOP_ID = "stop_id";
	private String stop_id;
	public static final String STOP_SEQUENCE = "stop_sequence";
	private int stop_sequence;
	public static final String ARRIVAL_TIME = "arrival_time";
	private String arrival_time;
	public static final String DEPARTURE_TIME = "departure_time";
	private String departure_time;

	public static final String STOP_HEADSIGN = "stop_headsign";
	private String stop_headsign;

	public static final String PICKUP_TYPE = "pickup_type";
	private int pickup_type;

	public GStopTime(String trip_id, String arrival_time, String departure_time, String stop_id, int stop_sequence, String stop_headsign, int pickup_type) {
		this.trip_id = trip_id;
		this.arrival_time = arrival_time;
		this.departure_time = departure_time;
		this.stop_id = stop_id;
		this.stop_sequence = stop_sequence;
		this.stop_headsign = stop_headsign;
		this.pickup_type = pickup_type;
		this.uid = null;
	}

	public int getStopSequence() {
		return stop_sequence;
	}

	public String getStopId() {
		return stop_id;
	}

	public String getTripId() {
		return trip_id;
	}

	public String getArrivalTime() {
		return arrival_time;
	}

	public String getDepartureTime() {
		return departure_time;
	}

	public String getStopHeadsign() {
		return stop_headsign;
	}

	public int getPickupType() {
		return pickup_type;
	}

	public boolean hasStopHeadsign() {
		return this.stop_headsign != null && this.stop_headsign.length() > 0;
	}

	private String uid = null;

	public String getUID() {
		if (this.uid == null) {
			this.uid = getUID(this.trip_id, this.stop_id, this.stop_sequence);
		}
		return this.uid;
	}

	public static String getUID(String trip_uid, String stop_id, int stop_sequence) {
		return stop_id + Constants.UUID_SEPARATOR + stop_sequence + Constants.UUID_SEPARATOR + trip_uid;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.trip_id).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_id).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_sequence).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.arrival_time).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.departure_time).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_headsign).append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.pickup_type).append(Constants.STRING_DELIMITER) //
				.toString();
	}
}
