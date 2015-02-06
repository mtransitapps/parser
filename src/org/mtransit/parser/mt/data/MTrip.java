package org.mtransit.parser.mt.data;

public class MTrip implements Comparable<MTrip> {

	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	public static final int HEADSIGN_TYPE_STOP_ID = 3;

	private int headsignType = HEADSIGN_TYPE_STRING; // 0 = String, 1 = direction, 2= inbound, 3=stopId
	private String headsignValue = "";
	private int headsignId = 0;
	private long routeId;

	private long id = -1;
	private String idString = null;

	public MTrip(long routeId) {
		this.routeId = routeId;
	}

	public long getId() {
		if (this.id < 0) {
			this.id = Long.valueOf(this.routeId + "0" + this.headsignId);
		}
		return this.id;
	}

	public String getIdString() {
		if (this.idString == null) {
			this.idString = this.routeId + "-" + this.headsignValue;
		}
		return this.idString;
	}

	public MTrip setHeadsignString(String headsignString, int headsignId) {
		this.headsignType = HEADSIGN_TYPE_STRING;
		this.headsignValue = headsignString;
		this.idString = null; // reset
		this.headsignId = headsignId;
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignDirection(MDirectionType direction) {
		this.headsignType = HEADSIGN_TYPE_DIRECTION;
		this.headsignValue = direction.id;
		this.idString = null; // reset
		this.headsignId = direction.intValue();
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignInbound(MInboundType inbound) {
		this.headsignType = HEADSIGN_TYPE_INBOUND;
		this.headsignValue = inbound.id;
		this.idString = null; // reset
		this.headsignId = Integer.valueOf(inbound.id);
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignStop(MStop stop) {
		this.headsignType = HEADSIGN_TYPE_STOP_ID;
		this.headsignValue = String.valueOf(stop.id);
		this.idString = null; // reset
		this.headsignId = stop.id;
		this.id = -1; // reset
		return this;
	}

	public int getHeadsignType() {
		return this.headsignType;
	}

	public String getHeadsignValue() {
		return this.headsignValue;
	}

	public int getHeadsignId() {
		return this.headsignId;
	}

	public long getRouteId() {
		return this.routeId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		MTrip t = (MTrip) obj;
		if (t.headsignType != this.headsignType) {
			return false;
		}
		if (t.headsignValue != null && !t.headsignValue.equals(this.headsignValue)) {
			return false;
		}
		if (t.routeId != 0 && t.routeId != this.routeId) {
			return false;
		}
		return true;
	}

	public boolean equalsExceptHeadsignValue(Object obj) {
		if (obj == null) {
			return false;
		}
		MTrip t = (MTrip) obj;
		if (t.headsignType != this.headsignType) {
			return false;
		}
		if (t.routeId != 0 && t.routeId != this.routeId) {
			return false;
		}
		return true;
	}

	public boolean mergeHeadsignValue(MTrip mTripToMerge) {
		if (mTripToMerge == null || mTripToMerge.headsignValue == null) {
			System.out.println("mergeHeadsignValue() > no trip heading value to merge > " + this.headsignValue);
			return true;
		}
		if (this.headsignValue == null) {
			this.headsignValue = mTripToMerge.headsignValue;
			System.out.println("mergeHeadsignValue() > no current headsign value > " + this.headsignValue);
			return true;
		}
		if (mTripToMerge.headsignValue.contains(this.headsignValue)) {
			this.headsignValue = mTripToMerge.headsignValue;
			return true;
		}
		if (this.headsignValue.contains(mTripToMerge.headsignValue)) {
			return true;
		}
		if (this.headsignValue.compareTo(mTripToMerge.headsignValue) > 0) {
			this.headsignValue = mTripToMerge.headsignValue + " / " + this.headsignValue;
		} else {
			this.headsignValue = this.headsignValue + " / " + mTripToMerge.headsignValue;
		}
		System.out.println("mergeHeadsignValue() > merge 2 headsign value > " + this.headsignValue);
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(getId()).append(',') // ID
				.append(this.headsignType).append(',') // HEADSIGN TYPE
				.append('\'').append(MSpec.escape(this.headsignValue)).append('\'').append(',') // HEADSIGN STRING
				.append(this.routeId) // ROUTE ID
				.toString();
	}

	@Override
	public int compareTo(MTrip otherTrip) {
		// sort by trip route id => trip id
		if (this.routeId != otherTrip.routeId) {
			return Long.compare(this.routeId, otherTrip.routeId);
		}
		return this.getIdString().compareTo(otherTrip.getIdString());
	}

}
