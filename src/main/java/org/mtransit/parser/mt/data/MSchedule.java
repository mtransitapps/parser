package org.mtransit.parser.mt.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;

public class MSchedule implements Comparable<MSchedule> {

	@NotNull
	private String serviceId;
	private long tripId; // direction ID
	private int stopId;
	private int departure;

	private int headsignType = -1;
	@Nullable
	private String headsignValue = null;

	@NotNull
	private String pathId; // trip ID
	private int arrivalBeforeDeparture;

	public MSchedule(@NotNull String serviceId, long tripId, int stopId, @Nullable Pair<Integer, Integer> times, @NotNull String pathId) {
		this(serviceId, tripId, stopId,
				times == null ? 0 : times.first,
				times == null ? 0 : times.second,
				pathId);
	}

	private MSchedule(@NotNull String serviceId, long tripId, int stopId, int arrival, int departure, @NotNull String pathId) {
		this.stopId = stopId;
		this.tripId = tripId;
		this.serviceId = serviceId;
		this.departure = departure;
		this.arrivalBeforeDeparture = departure - arrival;
		this.pathId = pathId;
		resetUID();
	}

	public void setHeadsign(int headsignType, @Nullable String headsignValue) {
		this.headsignType = headsignType;
		this.headsignValue = headsignValue;
	}

	public void clearHeadsign() {
		this.headsignType = -1;
		this.headsignValue = null;
	}

	public boolean isDescentOnly() {
		return this.headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY;
	}

	@Nullable
	private String uid = null;

	@NotNull
	public String getUID() {
		if (this.uid == null) {
			// identifies a stop + trip + service (date) => departure
			this.uid = this.serviceId + Constants.UUID_SEPARATOR + this.tripId + Constants.UUID_SEPARATOR + this.stopId + Constants.UUID_SEPARATOR
					+ this.departure;
		}
		return this.uid;
	}

	private void resetUID() {
		this.uid = null;
	}

	@NotNull
	public String getServiceId() {
		return serviceId;
	}

	public int getDeparture() {
		return departure;
	}

	@Override
	public int hashCode() {
		return getUID().hashCode() + (this.headsignValue == null ? 0 : this.headsignValue.hashCode());
	}

	@Override
	public String toString() {
		return toStringNewServiceIdAndTripId();
	}

	@NotNull
	public String toStringNewServiceIdAndTripId() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.serviceId)).append(Constants.STRING_DELIMITER); // service ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		// no route ID, just for file split
		sb.append(this.tripId); // trip ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.departure); // departure
		sb.append(Constants.COLUMN_SEPARATOR); //
		if (DefaultAgencyTools.EXPORT_PATH_ID) {
			if (this.arrivalBeforeDeparture > 0) {
				// TODO ?
			}
			sb.append(this.arrivalBeforeDeparture <= 0 ? Constants.EMPTY : this.arrivalBeforeDeparture); // arrival before departure
			sb.append(Constants.COLUMN_SEPARATOR); //
		}
		if (DefaultAgencyTools.EXPORT_PATH_ID) {
			sb.append(Constants.STRING_DELIMITER).append(this.pathId).append(Constants.STRING_DELIMITER); // original trip ID
			sb.append(Constants.COLUMN_SEPARATOR); //
		}
		sb.append(this.headsignType < 0 ? Constants.EMPTY : this.headsignType); // HEADSIGN TYPE
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER) //
				.append(this.headsignValue == null ? Constants.EMPTY : this.headsignValue) //
				.append(Constants.STRING_DELIMITER); // HEADSIGN STRING
		return sb.toString();
	}

	@NotNull
	public String toStringSameServiceIdAndTripId(@Nullable MSchedule lastSchedule) {
		StringBuilder sb = new StringBuilder(); //
		if (lastSchedule == null) {
			sb.append(this.departure); // departure
		} else {
			sb.append(this.departure - lastSchedule.departure); // departure
		}
		sb.append(Constants.COLUMN_SEPARATOR); //
		if (DefaultAgencyTools.EXPORT_PATH_ID) {
			if (this.arrivalBeforeDeparture > 0) {
				// TODO ?
			}
			sb.append(this.arrivalBeforeDeparture <= 0 ? Constants.EMPTY : this.arrivalBeforeDeparture); // arrival before departure
			sb.append(Constants.COLUMN_SEPARATOR); //
		}
		if (DefaultAgencyTools.EXPORT_PATH_ID) {
			sb.append(this.pathId); // original trip ID
			sb.append(Constants.COLUMN_SEPARATOR); //
		}
		if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
			if (this.headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY) {
				sb.append(MTrip.HEADSIGN_TYPE_DESCENT_ONLY); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER) //
						.append(Constants.EMPTY) //
						.append(Constants.STRING_DELIMITER); // HEADSIGN STRING
			} else {
				sb.append(this.headsignType < 0 ? Constants.EMPTY : this.headsignType); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER) //
						.append(this.headsignValue == null ? Constants.EMPTY : this.headsignValue) //
						.append(Constants.STRING_DELIMITER); // HEADSIGN STRING
			}
		} else {
			if (this.headsignType == MTrip.HEADSIGN_TYPE_DESCENT_ONLY) {
				sb.append(MTrip.HEADSIGN_TYPE_STRING); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER) //
						.append("Drop Off Only") //
						.append(Constants.STRING_DELIMITER); // HEADSIGN STRING
			} else {
				sb.append(this.headsignType < 0 ? Constants.EMPTY : this.headsignType); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER) //
						.append(this.headsignValue == null ? Constants.EMPTY : this.headsignValue) //
						.append(Constants.STRING_DELIMITER); // HEADSIGN STRING
			}
		}
		return sb.toString();
	}

	public boolean sameServiceIdAndTripId(@Nullable MSchedule lastSchedule) {
		if (lastSchedule == null) {
			return false;
		}
		return lastSchedule.serviceId.equals(this.serviceId) && lastSchedule.tripId == this.tripId;
	}

	@Override
	public int compareTo(@NotNull MSchedule otherSchedule) {
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

	@SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
	@Override
	public boolean equals(Object obj) {
		MSchedule ts = (MSchedule) obj;
		if (!ts.serviceId.equals(this.serviceId)) {
			return false;
		}
		// no route ID, just for file split
		if (ts.tripId != 0 && ts.tripId != this.tripId) {
			return false;
		}
		if (ts.stopId != 0 && ts.stopId != this.stopId) {
			return false;
		}
		//noinspection RedundantIfStatement
		if (ts.departure != 0 && ts.departure != this.departure) {
			return false;
		}
		return true;
	}

	public int getHeadsignType() {
		return this.headsignType;
	}

	@Nullable
	public String getHeadsignValue() {
		return this.headsignValue;
	}

	public long getTripId() {
		return this.tripId;
	}

	public int getStopId() {
		return this.stopId;
	}
}
