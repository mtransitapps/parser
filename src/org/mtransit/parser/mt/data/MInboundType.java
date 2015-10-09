package org.mtransit.parser.mt.data;

public enum MInboundType {

	NONE(""), INBOUND("1"), OUTBOUND("0");

	private String id;

	MInboundType(String id) {
		this.id = id;
	}

	public static MInboundType parse(String id) {
		if (INBOUND.id.equals(id)) {
			return INBOUND;
		}
		if (OUTBOUND.id.equals(id)) {
			return OUTBOUND;
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
		if (INBOUND.id.equals(this.id)) {
			return 1;
		} else if (OUTBOUND.id.equals(this.id)) {
			return 0;
		} else {
			System.out.printf("\nUnknow inbound type '%s'!\n", this.id);
			System.exit(-1);
			return -1;
		}
	}
}
