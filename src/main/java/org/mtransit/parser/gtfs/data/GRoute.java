package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#routes_fields
public class GRoute {

	public static final String FILENAME = "routes.txt";

	public static final String ROUTE_ID = "route_id";
	private String route_id;
	public static final String ROUTE_SHORT_NAME = "route_short_name";
	private String route_short_name;
	public static final String ROUTE_LONG_NAME = "route_long_name";
	private String route_long_name;
	public static final String ROUTE_TYPE = "route_type";
	private int route_type;

	public static final String AGENCY_ID = "agency_id";
	private String agency_id;
	public static final String ROUTE_DESC = "route_desc";
	private String route_desc;
	public static final String ROUTE_COLOR = "route_color";
	private String route_color;

	public GRoute(String agency_id, String route_id, String route_short_name, String route_long_name, String route_desc, int route_type, String route_color) {
		this.agency_id = agency_id;
		this.route_id = route_id;
		this.route_short_name = route_short_name;
		this.route_long_name = route_long_name;
		this.route_desc = route_desc;
		this.route_type = route_type;
		this.route_color = route_color;
	}

	public String getAgencyId() {
		return agency_id;
	}

	public String getRouteId() {
		return route_id;
	}

	public String getRouteShortName() {
		return route_short_name;
	}

	public String getRouteLongName() {
		return route_long_name;
	}

	public String getRouteDesc() {
		return route_desc;
	}

	public String getRouteColor() {
		return route_color;
	}

	public int getRouteType() {
		return route_type;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.route_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.route_short_name).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.route_long_name).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.route_type).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.agency_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.route_desc).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.route_color).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}
}
