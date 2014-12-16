package org.mtransit.parser.mt.data;

public class MSchedule implements Comparable<MSchedule> {

	public String serviceId;
	public long tripId;
	public int stopId;
	public int departure;

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

	public String getUID() {
		// identifies a stop + trip + service (date) => departure
		return this.serviceId + "-" + this.tripId + "-" + this.stopId + "-" + this.departure;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append('\'').append(MSpec.escape(serviceId)).append('\''); // service ID
		sb.append(','); //
		// no route ID, just for file split
		sb.append(tripId); // trip ID
		sb.append(','); //
		sb.append(stopId); // stop ID
		sb.append(','); //
		sb.append(departure); // departure
		sb.append(','); //
		sb.append(headsignType < 0 ? "" : headsignType); // HEADSIGN TYPE
		sb.append(','); //
		sb.append('\'').append(headsignValue == null ? "" : headsignValue).append('\''); // HEADSIGN STRING
		return sb.toString();
	}

	@Override
	public int compareTo(MSchedule otherSchedule) {
		// sort by service_id => trip_id => stop_id => departure
		if (!serviceId.equals(otherSchedule.serviceId)) {
			return serviceId.compareTo(otherSchedule.serviceId);
		}
		// no route ID, just for file split
		if (tripId != otherSchedule.tripId) {
			return Long.compare(tripId, otherSchedule.tripId);
		}
		if (stopId != otherSchedule.stopId) {
			return stopId - otherSchedule.stopId;
		}
		return departure - otherSchedule.departure;
	}

	@Override
	public boolean equals(Object obj) {
		MSchedule ts = (MSchedule) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(serviceId)) {
			return false;
		}
		// no route ID, just for file split
		if (ts.tripId != 0 && ts.tripId != tripId) {
			return false;
		}
		if (ts.stopId != 0 && ts.stopId != stopId) {
			return false;
		}
		if (ts.departure != 0 && ts.departure != departure) {
			return false;
		}
		return true;
	}

}
