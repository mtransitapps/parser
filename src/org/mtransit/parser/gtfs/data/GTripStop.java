package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stop_times_trip_id_field
// https://developers.google.com/transit/gtfs/reference#stop_times_stop_id_field
public class GTripStop {

	public static final String TRIP_ID = "trip_id";
	public String trip_id;
	public static final String STOP_ID = "stop_id";
	public String stop_id;
	public static final String STOP_SEQUENCE = "stop_sequence";
	public int stop_sequence;

	public GTripStop(String trip_id, String stop_id, int stop_sequence) {
		this.trip_id = trip_id;
		this.stop_id = stop_id;
		this.stop_sequence = stop_sequence;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append('\'').append(trip_id).append('\'').append(',') //
				.append('\'').append(stop_id).append('\'').append(',') //
				.append('\'').append(stop_sequence).append('\'').append(',') //
				.toString();
	}

	public static String getUID(String trip_uid, String stop_id) {
		return trip_uid + stop_id;
	}

}
