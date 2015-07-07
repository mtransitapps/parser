package org.mtransit.parser.mt.data;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;

public class MFrequency implements Comparable<MFrequency> {

	private String serviceId;
	private long tripId;
	private int startTime;
	private int endTime;
	public int headwayInSec;

	public MFrequency(String serviceId, long routeId, long tripId, int startTime, int endTime, int headwayInSec) {
		this.serviceId = serviceId;
		this.tripId = tripId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.headwayInSec = headwayInSec;
		this.uuid = null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.serviceId)).append(Constants.STRING_DELIMITER); // service ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.tripId); // trip ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.startTime); // start time
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.endTime); // end time
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.headwayInSec); // headway in seconds
		return sb.toString();
	}

	private String uuid = null;

	public String getUID() {
		if (this.uuid == null) {
			this.uuid = this.serviceId + Constants.UUID_SEPARATOR + this.tripId + Constants.UUID_SEPARATOR + this.startTime + Constants.UUID_SEPARATOR
					+ this.endTime;
		}
		return this.uuid;
	}

	@Override
	public int compareTo(MFrequency otherFrequency) {
		if (!this.serviceId.equals(otherFrequency.serviceId)) {
			return this.serviceId.compareTo(otherFrequency.serviceId);
		}
		if (this.tripId != otherFrequency.tripId) {
			return Long.compare(this.tripId, otherFrequency.tripId);
		}
		if (this.startTime != otherFrequency.startTime) {
			return this.startTime - otherFrequency.startTime;
		}
		if (this.endTime != otherFrequency.endTime) {
			return this.endTime - otherFrequency.endTime;
		}
		return this.headwayInSec - otherFrequency.headwayInSec;
	}

	@Override
	public boolean equals(Object obj) {
		MFrequency ts = (MFrequency) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(serviceId)) {
			return false;
		}
		// no route ID, just for file split
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
