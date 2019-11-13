package org.mtransit.parser.mt.data;

public enum MDirectionType {

	NONE(""), EAST("E"), WEST("W"), NORTH("N"), SOUTH("S");

	private static final String WEST_FR = "O";

	private String id;

	MDirectionType(String id) {
		this.id = id;
	}

	public static MDirectionType parse(String id) {
		if (EAST.id.equals(id)) {
			return EAST;
		}
		if (WEST.id.equals(id) || WEST_FR.equals(id)) {
			return WEST;
		}
		if (NORTH.id.equals(id)) {
			return NORTH;
		}
		if (SOUTH.id.equals(id)) {
			return SOUTH;
		}
		return NONE; // default
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return id;
	}

	public int intValue() {
		if (EAST.id.equals(this.id)) {
			return 1;
		} else if (WEST.id.equals(this.id)) {
			return 2;
		} else if (NORTH.id.equals(this.id)) {
			return 3;
		} else if (SOUTH.id.equals(this.id)) {
			return 4;
		} else {
			System.out.printf("\nUnknow direction type '%s'!\n", this.id);
			System.exit(-1);
			return -1;
		}
	}
}
