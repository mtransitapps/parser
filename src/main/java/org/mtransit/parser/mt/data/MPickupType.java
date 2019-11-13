package org.mtransit.parser.mt.data;

public enum MPickupType {

	REGULAR(0), NO_PICKUP(1), MUST_PHONE_AGENCY(2), MUST_COORDINATE_WITH_DRIVER(3);

	private int id;

	MPickupType(int id) {
		this.id = id;
	}

	public static MPickupType parse(int id) {
		if (REGULAR.id == id) {
			return REGULAR;
		}
		if (NO_PICKUP.id == id) {
			return NO_PICKUP;
		}
		if (MUST_PHONE_AGENCY.id == id) {
			return MUST_PHONE_AGENCY;
		}
		if (MUST_COORDINATE_WITH_DRIVER.id == id) {
			return MUST_COORDINATE_WITH_DRIVER;
		}
		return REGULAR; // default
	}

	public static MPickupType parse(String id) {
		if (id == null) {// no pickup info, that's OK
			return REGULAR; // default
		}
		try {
			return parse(Integer.valueOf(id));
		} catch (NumberFormatException nfe) {
			System.out.printf("\nError while parsing %s as pickup tipe!\n", id);
			throw nfe;
		}
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return String.valueOf(id);
	}
}
