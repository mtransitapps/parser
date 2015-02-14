package org.mtransit.parser.mt.data;

public class MSchedule implements Comparable<MSchedule> {

	private String serviceId;
	private long tripId;
	private int stopId;
	private int departure;

	private int headsignType = -1;
	private String headsignValue = null;

	public MSchedule(String serviceId, long routeId, long tripId, int stopId, int departure) {
		this.stopId = stopId;
		this.tripId = tripId;
		this.serviceId = serviceId;
		this.departure = departure;
	}

	public void setHeadsign(int headsignType, String headsignValue) {
		this.headsignType = headsignType;
		this.headsignValue = headsignValue;
	}

	public void clearHeadsign() {
		this.headsignType = -1;
		this.headsignValue = null;
	}

	public int getHeadsignType() {
		return this.headsignType;
	}

	public String getHeadsignValue() {
		return this.headsignValue;
	}

	public long getTripId() {
		return this.tripId;
	}

	public int getStopId() {
		return this.stopId;
	}

	public String getUID() {
		// identifies a stop + trip + service (date) => departure
		return this.serviceId + "-" + this.tripId + "-" + this.stopId + "-" + this.departure;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append('\'').append(MSpec.escape(this.serviceId)).append('\''); // service ID
		sb.append(','); //
		// no route ID, just for file split
		sb.append(this.tripId); // trip ID
		sb.append(','); //
		sb.append(this.departure); // departure
		sb.append(','); //
		sb.append(this.headsignType < 0 ? "" : this.headsignType); // HEADSIGN TYPE
		sb.append(','); //
		sb.append('\'').append(this.headsignValue == null ? "" : this.headsignValue).append('\''); // HEADSIGN STRING
		return sb.toString();
	}

	@Override
	public int compareTo(MSchedule otherSchedule) {
		// sort by service_id => trip_id => stop_id => departure
		if (!this.serviceId.equals(otherSchedule.serviceId)) {
			return this.serviceId.compareTo(otherSchedule.serviceId);
		}
		// no route ID, just for file split
		if (this.tripId != otherSchedule.tripId) {
			return Long.compare(this.tripId, otherSchedule.tripId);
		}
		if (this.stopId != otherSchedule.stopId) {
			return this.stopId - otherSchedule.stopId;
		}
		return this.departure - otherSchedule.departure;
	}

	@Override
	public boolean equals(Object obj) {
		MSchedule ts = (MSchedule) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(this.serviceId)) {
			return false;
		}
		// no route ID, just for file split
		if (ts.tripId != 0 && ts.tripId != this.tripId) {
			return false;
		}
		if (ts.stopId != 0 && ts.stopId != this.stopId) {
			return false;
		}
		if (ts.departure != 0 && ts.departure != this.departure) {
			return false;
		}
		return true;
	}

}
