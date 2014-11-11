package org.mtransit.parser.mt.data;

import java.util.List;

public class MTripStop implements Comparable<MTripStop> {

	private int tripId;
	private int stopId;
	public int stopSequence;
	private String uid;

	private boolean decentOnly = false;

	public MTripStop(int tripId, int stopId, int stopSequence) {
		this.tripId = tripId;
		this.stopId = stopId;
		this.stopSequence = stopSequence;
		this.uid = this.tripId + "" + this.stopId;
	}

	public void setDecentOnly(boolean decentOnly) {
		this.decentOnly = decentOnly;
	}

	public String getUID() {
		return this.uid;
	}

	public int getTripId() {
		return tripId;
	}

	public int getStopId() {
		return stopId;
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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder() //
				.append(tripId) // TRIP ID
				.append(',') //
				.append(stopId) // STOP ID
				.append(',') //
				.append(stopSequence); // STOP SEQUENCE
		sb.append(',').append(decentOnly ? 1 : 0); // DECENT ONLY
		return sb.toString();
	}

	@Override
	public int compareTo(MTripStop otherTripStop) {
		// sort by trip_id => stop_sequence
		if (tripId != otherTripStop.tripId) {
			return tripId - otherTripStop.tripId;
		}
		return stopSequence - otherTripStop.stopSequence;
	}

	public static String printStops(List<MTripStop> l) {
		StringBuilder sb = new StringBuilder();
		for (MTripStop mTripStop : l) {
			if (sb.length() > 0) {
				sb.append(',');
			}
			sb.append(mTripStop.stopId);
		}
		return sb.toString();
	}

}
