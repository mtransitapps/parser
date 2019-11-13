package org.mtransit.parser.mt.data;

public enum MDropOffType {

	REGULAR(0), NO_DROP_OFF(1), MUST_PHONE_AGENCY(2), MUST_COORDINATE_WITH_DRIVER(3);

	private int id;

	MDropOffType(int id) {
		this.id = id;
	}

	public static MDropOffType parse(int id) {
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

	public static MDropOffType parse(String id) {
		if (id == null) { // that's OK
			return REGULAR; // default
		}
		return parse(Integer.valueOf(id));
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
