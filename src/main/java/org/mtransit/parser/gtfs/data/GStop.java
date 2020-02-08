package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stops_fields
public class GStop {

	public static final String FILENAME = "stops.txt";

	public static final String STOP_ID = "stop_id";
	private String stop_id;
	public static final String STOP_NAME = "stop_name";
	private String stop_name;
	public static final String STOP_LAT = "stop_lat";
	private double stop_lat;
	public static final String STOP_LON = "stop_lon";
	private double stop_lon;
	public static final String STOP_CODE = "stop_code";
	private String stop_code;

	public static final String LOCATION_TYPE = "location_type";

	public GStop(String stop_id, String stop_name, double stop_lat, double stop_lon, String stop_code) {
		this.stop_id = stop_id;
		this.stop_name = stop_name;
		this.stop_lat = stop_lat;
		this.stop_lon = stop_lon;
		this.stop_code = stop_code;
	}

	public String getStopId() {
		return stop_id;
	}

	public String getStopName() {
		return stop_name;
	}

	public double getStopLat() {
		return this.stop_lat;
	}

	public double getStopLong() {
		return this.stop_lon;
	}

	public String getStopCode() {
		return stop_code;
	}

	@Override
	public String toString() {
		return GStop.class.getSimpleName() + "{" +
				"stop_id='" + stop_id + '\'' +
				", stop_name='" + stop_name + '\'' +
				", stop_lat=" + stop_lat +
				", stop_lon=" + stop_lon +
				", stop_code='" + stop_code + '\'' +
				'}';
	}
}
