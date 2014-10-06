package org.mtransit.parser.mt.data;

public enum MInboundType {

	NONE(""), INBOUND("0"), OUTBOUND("1");
	
	public String id;

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
	
	@Override
	public String toString() {
		return id;
	}
}
