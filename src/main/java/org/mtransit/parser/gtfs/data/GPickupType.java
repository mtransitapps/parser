package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#stop_times_pickup_type_field
public enum GPickupType {

	REGULAR(0), NO_PICKUP(1), MUST_PHONE_AGENCY(2), MUST_COORDINATE_WITH_DRIVER(3);

	private int id;

	GPickupType(int id) {
		this.id = id;
	}

	public int intValue() {
		return this.id;
	}

	public static GPickupType parse(int id) {
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

	public static GPickupType parse(String id) {
		if (id == null || id.length() == 0) { // no pickup info, that's OK
			return REGULAR; // default
		}
		try {
			return parse(Integer.valueOf(id));
		} catch (NumberFormatException nfe) {
			System.out.printf("\nError while parsing '%s' as pickup tipe!\n", id);
			nfe.printStackTrace();
			throw nfe;
		}
	}

	@Override
	public String toString() {
		return GPickupType.class.getSimpleName() + "{" +
				"id=" + id +
				'}';
	}
}
