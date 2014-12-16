package org.mtransit.parser.mt.data;

public class MFrequency implements Comparable<MFrequency> {

	public String serviceId;
	public long tripId;
	public int startTime;
	public int endTime;
	public int headwayInSec;

	public MFrequency(String serviceId, long routeId, long tripId, int startTime, int endTime, int headwayInSec) {
		this.serviceId = serviceId;
		this.tripId = tripId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.headwayInSec = headwayInSec;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append('\'').append(MSpec.escape(serviceId)).append('\''); // service ID
		sb.append(','); //
		sb.append(tripId); // trip ID
		sb.append(','); //
		sb.append(startTime); // start time
		sb.append(','); //
		sb.append(endTime); // end time
		sb.append(','); //
		sb.append(headwayInSec); // headway in seconds
		return sb.toString();
	}

	public String getUID() {
		return this.serviceId + "-" + this.tripId + "-" + this.startTime + "-" + this.endTime;
	}

	@Override
	public int compareTo(MFrequency otherFrequency) {
		if (!serviceId.equals(otherFrequency.serviceId)) {
			return serviceId.compareTo(otherFrequency.serviceId);
		}
		if (tripId != otherFrequency.tripId) {
			return Long.compare(tripId, otherFrequency.tripId);
		}
		if (startTime != otherFrequency.startTime) {
			return startTime - otherFrequency.startTime;
		}
		if (endTime != otherFrequency.endTime) {
			return endTime - otherFrequency.endTime;
		}
		return headwayInSec - otherFrequency.headwayInSec;
	}

	@Override
	public boolean equals(Object obj) {
		MFrequency ts = (MFrequency) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(serviceId)) {
			return false;
		}
		if (ts.tripId != 0 && ts.tripId != tripId) {
			return false;
		}
		if (ts.startTime != 0 && ts.startTime != startTime) {
			return false;
		}
		if (ts.endTime != 0 && ts.endTime != endTime) {
			return false;
		}
		if (ts.headwayInSec != 0 && ts.headwayInSec != headwayInSec) {
			return false;
		}
		return true;
	}

}
