package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#routes_fields
public class GRoute {

	public static final String FILENAME = "routes.txt";

	public static final String ROUTE_ID = "route_id";
	public String route_id;
	public static final String ROUTE_SHORT_NAME = "route_short_name";
	public String route_short_name;
	public static final String ROUTE_LONG_NAME = "route_long_name";
	public String route_long_name;
	public static final String ROUTE_TYPE = "route_type";
	public String route_type;

	public static final String AGENCY_ID = "agency_id";
	public String agency_id;
	public static final String ROUTE_DESC = "route_desc";
	public String route_desc;
	public static final String ROUTE_URL = "route_url";
	public String route_url;
	public static final String ROUTE_COLOR = "route_color";
	public String route_color;
	public static final String ROUTE_TEXT_COLOR = "route_text_color";
	public String route_text_color;

	public GRoute(String route_id, String route_short_name, String route_long_name, String route_type) {
		this.route_id = route_id;
		this.route_short_name = route_short_name;
		this.route_long_name = route_long_name;
		this.route_type = route_type;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(route_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_short_name).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_long_name).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_type).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(agency_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_desc).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_url).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_color).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(route_text_color).append(Constants.STRING_DELIMITER) //
				.toString();
	}

}
