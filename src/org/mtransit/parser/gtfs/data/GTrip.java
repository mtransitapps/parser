package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#trips_fields
public class GTrip {

	public static final String FILENAME = "trips.txt";

	public static final String ROUTE_ID = "route_id";
	private String route_id;
	public static final String SERVICE_ID = "service_id";
	private String service_id;
	public static final String TRIP_ID = "trip_id";
	private String trip_id;

	public static final String TRIP_HEADSIGN = "trip_headsign";
	private String trip_headsign;
	public static final String DIRECTION_ID = "direction_id";
	private Integer direction_id;
	public static final String SHAPE_ID = "shape_id";
	private String shape_id;

	private String uid;

	public GTrip(String route_id, String service_id, String trip_id, Integer direction_id, String trip_headsign, String shape_id) {
		this.route_id = route_id;
		this.service_id = service_id;
		this.trip_id = trip_id;
		this.direction_id = direction_id;
		this.trip_headsign = trip_headsign;
		this.shape_id = shape_id;
		this.uid = getUID(this.getRouteId(), this.trip_id);
	}

	public String getShapeId() {
		return shape_id;
	}

	private static String getUID(String routeId, String tripId) {
		return routeId + tripId;
	}

	public String getUID() {
		return uid;
	}

	public String getRouteId() {
		return route_id;
	}

	public String getTripId() {
		return trip_id;
	}

	public String getServiceId() {
		return service_id;
	}

	public Integer getDirectionId() {
		return direction_id;
	}

	public String getTripHeadsign() {
		return trip_headsign;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.route_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.service_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.trip_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.trip_headsign).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.direction_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.shape_id).append(Constants.STRING_DELIMITER) //
				.toString();
	}
}
