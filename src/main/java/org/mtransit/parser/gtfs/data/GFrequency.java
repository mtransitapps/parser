package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#frequencies_fields
public class GFrequency {

	public static final String FILENAME = "frequencies.txt";

	public static final int DEFAULT_PICKUP_TYPE = GPickupType.REGULAR.intValue(); // Regularly scheduled pickup
	public static final int DEFAULT_DROP_OFF_TYPE = GDropOffType.REGULAR.intValue(); // Regularly scheduled drop off

	public static final String TRIP_ID = "trip_id";
	private String trip_id;
	public static final String START_TIME = "start_time";
	private String start_time;
	public static final String END_TIME = "end_time";
	private String end_time;
	public static final String HEADWAY_SECS = "headway_secs";
	private int headway_secs;

	public GFrequency(String trip_id, String start_time, String end_time, int headway_secs) {
		this.trip_id = trip_id;
		this.start_time = start_time;
		this.end_time = end_time;
		this.headway_secs = headway_secs;
	}

	public String getTripId() {
		return trip_id;
	}

	public String getStartTime() {
		return start_time;
	}

	public String getEndTime() {
		return end_time;
	}

	public int getHeadwaySecs() {
		return headway_secs;
	}

	@Override
	public String toString() {
		return GFrequency.class.getSimpleName() + "{" +
				"trip_id='" + trip_id + '\'' +
				", start_time='" + start_time + '\'' +
				", end_time='" + end_time + '\'' +
				", headway_secs=" + headway_secs +
				'}';
	}
}
