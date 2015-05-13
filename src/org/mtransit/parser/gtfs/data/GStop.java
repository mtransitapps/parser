package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#stops_fields
public class GStop {

	public static final String FILENAME = "stops.txt";

	public static final String STOP_ID = "stop_id";
	public String stop_id;
	public static final String STOP_NAME = "stop_name";
	public String stop_name;
	public static final String STOP_LAT = "stop_lat";
	private double stop_lat;
	public static final String STOP_LON = "stop_lon";
	private double stop_lon;
	public static final String STOP_CODE = "stop_code";
	public String stop_code;

	public static final String LOCATION_TYPE = "location_type";
	public GStop(String stop_id, String stop_name, double stop_lat, double stop_lon, String stop_code) {
		this.stop_id = stop_id;
		this.stop_name = stop_name;
		this.stop_lat = stop_lat;
		this.stop_lon = stop_lon;
		this.stop_code = stop_code;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.stop_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_name).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_lat).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_lon).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.stop_code).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}

	public double getLatD() {
		return this.stop_lat;
	}

	public double getLongD() {
		return this.stop_lon;
	}
}
