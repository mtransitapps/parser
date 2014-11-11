package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stops_fields
public class GStop {

	public static final String FILENAME = "stops.txt";

	public static final String STOP_ID = "stop_id";
	public String stop_id;
	public static final String STOP_NAME = "stop_name";
	public String stop_name;
	public static final String STOP_LAT = "stop_lat";
	public String stop_lat;
	public static final String STOP_LON = "stop_lon";
	public String stop_lon;

	public static final String STOP_CODE = "stop_code";
	public String stop_code;
	public static final String STOP_DESC = "stop_desc";
	public String stop_desc;
	public static final String ZONE_ID = "zone_id";
	public String zone_id;
	public static final String STOP_URL = "stop_url";
	public String stop_url;
	public static final String LOCATION_TYPE = "location_type";
	public String location_type;
	public static final String PARENT_STATION = "parent_station";
	public String parent_station;
	public static final String STOP_TIMEZONE = "stop_timezone";
	public String stop_timezone;

	public GStop(String stop_id, String stop_name, String stop_lat, String stop_lon) {
		this.stop_id = stop_id;
		this.stop_name = stop_name;
		this.stop_lat = stop_lat;
		this.stop_lon = stop_lon;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append('\'').append(stop_id).append('\'').append(',') //
				.append('\'').append(stop_name).append('\'').append(',') //
				.append('\'').append(stop_lat).append('\'').append(',') //
				.append('\'').append(stop_lon).append('\'').append(',') //
				.append('\'').append(stop_code).append('\'').append(',') //
				.append('\'').append(stop_desc).append('\'').append(',') //
				.append('\'').append(zone_id).append('\'').append(',') //
				.append('\'').append(stop_url).append('\'').append(',') //
				.append('\'').append(location_type).append('\'').append(',') //
				.append('\'').append(parent_station).append('\'').append(',') //
				.append('\'').append(stop_timezone).append('\'') //
				.toString();
	}
}
