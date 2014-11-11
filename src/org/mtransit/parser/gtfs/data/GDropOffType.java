package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stop_times_drop_off_type_field
public enum GDropOffType {

	REGULAR(0), NO_DROP_OFF(1), MUST_PHONE_AGENCY(2), MUST_COORDINATE_WITH_DRIVER(3);

	public int id;

	GDropOffType(int id) {
		this.id = id;
	}

	public static GDropOffType parse(int id) {
		if (REGULAR.id == id) {
			return REGULAR;
		}
		if (NO_DROP_OFF.id == id) {
			return NO_DROP_OFF;
		}
		if (MUST_PHONE_AGENCY.id == id) {
			return MUST_PHONE_AGENCY;
		}
		if (MUST_COORDINATE_WITH_DRIVER.id == id) {
			return MUST_COORDINATE_WITH_DRIVER;
		}
		return REGULAR; // default
	}

	public static GDropOffType parse(String id) {
		if (id == null) { // that's OK
			return REGULAR; // default
		}
		return parse(Integer.valueOf(id));
	}
}
