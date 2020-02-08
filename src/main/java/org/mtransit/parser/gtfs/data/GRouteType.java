package org.mtransit.parser.gtfs.data;

import java.util.HashMap;

// https://developers.google.com/transit/gtfs/reference/#routestxt
// https://developers.google.com/transit/gtfs/reference/extended-route-types
public enum GRouteType {

	// STANDARD:
	LIGHT_RAIL(0), // Tram, streetcar, or light rail. Used for any light rail or street-level system within a metropolitan area.
	SUBWAY(1), // Subway or metro. Used for any underground rail system within a metropolitan area.
	TRAIN(2), // Rail. Used for intercity or long-distance travel.
	BUS(3), // Bus. Used for short- and long-distance bus routes.
	FERRY(4), // Ferry. Used for short- and long-distance boat service
	// 5: Cable car. Used for street-level cable cars where the cable runs beneath the car.
	// 6: Gondola or suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.
	// 7: Funicular. Used for any rail system that moves on steep inclines with a cable traction system.

	// EXTENDED:
	EX_BUS_SERVICE(700), // Bus Service
	EX_SHARE_TAXI_SERVICE(717), // Share Taxi Service
	EX_COMMUNAL_TAXI_SERVICE(1501), // Communal Taxi Service (Please use 717)
	;

	private int id;

	private static HashMap<Integer, GRouteType> mappings;

	private synchronized static HashMap<Integer, GRouteType> getMappings() {
		if (mappings == null) {
			mappings = new HashMap<>();
		}
		return mappings;
	}

	GRouteType(int id) {
		this.id = id;
		GRouteType.getMappings().put(id, this);
	}

	public int getId() {
		return id;
	}

	public static boolean isUnknown(Integer routeType) {
		return !getMappings().containsKey(routeType);
	}

	public static boolean isSameType(int agencyRouteType, int routeType) {
		if (agencyRouteType == routeType) {
			return true;
		}
		if (agencyRouteType == BUS.id) {
			if (routeType == EX_BUS_SERVICE.id //
					|| routeType == EX_SHARE_TAXI_SERVICE.id //
					|| routeType == EX_COMMUNAL_TAXI_SERVICE.id) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return GRouteType.class.getSimpleName() + "{" +
				"id=" + id +
				'}';
	}
}
