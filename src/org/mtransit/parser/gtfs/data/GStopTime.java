package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stop_times_fields
public class GStopTime {

	public static final String FILENAME = "stop_times.txt";

	public static final String TRIP_ID = "trip_id";
	public String trip_id;
	public static final String STOP_ID = "stop_id";
	public String stop_id;
	public static final String STOP_SEQUENCE = "stop_sequence";
	public int stop_sequence;
	public static final String ARRIVAL_TIME = "arrival_time";
	public String arrival_time;
	public static final String DEPARTURE_TIME = "departure_time";
	public String departure_time;

	public static final String STOP_HEADSIGN = "stop_headsign";
	public String stop_headsign;
	public static final String PICKUP_TYPE = "pickup_type";
	public GPickupType pickup_type;
	public static final String DROP_OFF_TYPE = "drop_off_type";
	public GDropOffType drop_off_type;
	public static final String SHAPE_DIST_TRAVELED = "shape_dist_traveled";
	public String shape_dist_traveled;

	public GStopTime(String trip_id, String arrival_time, String departure_time,
			String stop_id, int stop_sequence) {
		this.trip_id = trip_id;
		this.arrival_time = arrival_time;
		this.departure_time = departure_time;
		this.stop_id = stop_id;
		this.stop_sequence = stop_sequence;
	}
	
	@Override
	public String toString() {
		return new StringBuilder() //
				.append('\'').append(trip_id).append('\'').append(',') //
				.append('\'').append(stop_id).append('\'').append(',') //
				.append('\'').append(stop_sequence).append('\'').append(',') //
				.append('\'').append(arrival_time).append('\'').append(',') //
				.append('\'').append(departure_time).append('\'').append(',') //
				.append('\'').append(stop_headsign).append('\'').append(',') //
				.append('\'').append(pickup_type).append('\'').append(',') //
				.append('\'').append(drop_off_type).append('\'').append(',') //
				.append('\'').append(shape_dist_traveled).append('\'') //
				.toString();
	}
}
