package org.mtransit.parser.mt.data;

import java.util.List;

import org.mtransit.parser.Constants;

public class MTripStop implements Comparable<MTripStop> {

	private long tripId;
	private int stopId;
	private int stopSequence;
	private String uid;

	private boolean descentOnly = false;

	public MTripStop(long tripId, int stopId, int stopSequence) {
		this.tripId = tripId;
		this.stopId = stopId;
		this.stopSequence = stopSequence;
		this.uid = this.tripId + Constants.EMPTY + this.stopId;
	}

	public void setDescentOnly(boolean descentOnly) {
		this.descentOnly = descentOnly;
	}

	public boolean isDescentOnly() {
		return this.descentOnly;
	}

	public String getUID() {
		return this.uid;
	}

	public long getTripId() {
		return tripId;
	}

	public int getStopId() {
		return stopId;
	}

	public void setStopSequence(int stopSequence) {
		this.stopSequence = stopSequence;
	}

	@Override
	public boolean equals(Object obj) {
		MTripStop ts = (MTripStop) obj;
		if (ts.tripId != 0 && ts.tripId != tripId) {
			return false;
		}
		if (ts.stopId != 0 && ts.stopId != stopId) {
			return false;
		}
		if (ts.stopSequence != 0 && ts.stopSequence != stopSequence) {
			return false;
		}
		return true;
	}

	public boolean equalsExceptStopSequence(MTripStop ts) {
		if (ts.tripId != 0 && ts.tripId != tripId) {
			return false;
		}
		if (ts.stopId != 0 && ts.stopId != stopId) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(this.tripId); // TRIP ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.stopId); // STOP ID
		sb.append(Constants.COLUMN_SEPARATOR);
		sb.append(this.stopSequence); // STOP SEQUENCE
		sb.append(Constants.COLUMN_SEPARATOR);
		sb.append(this.descentOnly ? 1 : 0); // DESCENT ONLY
		return sb.toString();
	}

	@Override
	public int compareTo(MTripStop otherTripStop) {
		// sort by trip_id => stop_sequence
		if (this.tripId != otherTripStop.tripId) {
			return Long.compare(this.tripId, otherTripStop.tripId);
		}
		return stopSequence - otherTripStop.stopSequence;
	}

	public static String printStops(List<MTripStop> l) {
		StringBuilder sb = new StringBuilder();
		for (MTripStop mTripStop : l) {
			if (sb.length() > 0) {
				sb.append(Constants.COLUMN_SEPARATOR);
			}
			sb.append(mTripStop.stopId);
		}
		return sb.toString();
	}
}
