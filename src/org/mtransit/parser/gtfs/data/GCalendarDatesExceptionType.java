package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#calendar_dates_exception_type_field
public enum GCalendarDatesExceptionType {

	SERVICE_ADDED(1), SERVICE_REMOVED(2);

	public int id;

	GCalendarDatesExceptionType(int id) {
		this.id = id;
	}

	public static GCalendarDatesExceptionType parse(int id) {
		if (SERVICE_ADDED.id == id) {
			return SERVICE_ADDED;
		}
		if (SERVICE_REMOVED.id == id) {
			return SERVICE_REMOVED;
		}
		return SERVICE_ADDED; // default
	}

	public static GCalendarDatesExceptionType parse(String id) {
		if (id == null) { // that's OK
			return SERVICE_ADDED; // default
		}
		return parse(Integer.valueOf(id));
	}
	
	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
