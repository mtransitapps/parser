package org.mtransit.parser.gtfs.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Constants;
import org.mtransit.parser.MTLog;

import java.util.Objects;

// https://developers.google.com/transit/gtfs/reference#stop_times_fields
public class GStopTime implements Comparable<GStopTime> {

	public static final String FILENAME = "stop_times.txt";

	public static final String TRIP_ID = "trip_id";
	private String trip_id;
	public static final String STOP_ID = "stop_id";
	private String stop_id;
	public static final String STOP_SEQUENCE = "stop_sequence";
	private int stop_sequence;
	public static final String ARRIVAL_TIME = "arrival_time";
	private String arrival_time;
	public static final String DEPARTURE_TIME = "departure_time";
	private String departure_time;

	public static final String STOP_HEADSIGN = "stop_headsign";
	private String stop_headsign;

	public static final String PICKUP_TYPE = "pickup_type";
	private int pickup_type;

	public static final String DROP_OFF_TYPE = "drop_off_type";
	private int drop_off_type;

	public GStopTime(String trip_id, String arrival_time, String departure_time, String stop_id, int stop_sequence, String stop_headsign, int pickup_type,
					 int drop_off_type) {
		this.trip_id = trip_id;
		this.arrival_time = arrival_time;
		this.departure_time = departure_time;
		this.stop_id = stop_id;
		this.stop_sequence = stop_sequence;
		this.stop_headsign = stop_headsign;
		this.pickup_type = pickup_type;
		this.drop_off_type = drop_off_type;
		this.uid = null;
	}

	public int getStopSequence() {
		return stop_sequence;
	}

	public String getStopId() {
		return stop_id;
	}

	public String getTripId() {
		return trip_id;
	}

	public String getArrivalTime() {
		return arrival_time;
	}

	public String getDepartureTime() {
		return departure_time;
	}

	public String getStopHeadsign() {
		return stop_headsign;
	}

	public int getPickupType() {
		return pickup_type;
	}

	public void setPickupType(int pickupType) {
		this.pickup_type = pickupType;
	}

	public int getDropOffType() {
		return drop_off_type;
	}

	public void setDropOffType(int dropOffType) {
		this.drop_off_type = dropOffType;
	}

	public boolean hasStopHeadsign() {
		return this.stop_headsign != null && this.stop_headsign.length() > 0;
	}

	@Nullable
	private String uid;

	public String getUID() {
		if (this.uid == null) {
			this.uid = getUID(this.trip_id, this.stop_id, this.stop_sequence);
		}
		return this.uid;
	}

	public static String getUID(String trip_uid, String stop_id, int stop_sequence) {
		return stop_id + Constants.UUID_SEPARATOR + stop_sequence + Constants.UUID_SEPARATOR + trip_uid;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		GStopTime otherGStopTime = (GStopTime) o;
		return Objects.equals(this.trip_id, otherGStopTime.trip_id) //
				&& Objects.equals(this.stop_id, otherGStopTime.stop_id) //
				&& this.stop_sequence == otherGStopTime.stop_sequence //
				&& Objects.equals(this.arrival_time, otherGStopTime.arrival_time) //
				&& Objects.equals(this.departure_time, otherGStopTime.departure_time) //
				&& Objects.equals(this.stop_headsign, otherGStopTime.stop_headsign) //
				&& this.pickup_type == otherGStopTime.pickup_type //
				&& this.drop_off_type == otherGStopTime.drop_off_type;
	}

	@Override
	public int hashCode() {
		return Objects.hash( //
				this.trip_id, //
				this.stop_id, //
				this.stop_sequence, //
				this.arrival_time, //
				this.departure_time, //
				this.stop_headsign, //
				this.pickup_type, //
				this.drop_off_type //
		);
	}

	@Override
	public int compareTo(@NotNull GStopTime otherGStopTime) {
		// sort by trip_id, stop_sequence
		if (!Objects.equals(this.trip_id, otherGStopTime.trip_id)) {
			return this.trip_id.compareTo(otherGStopTime.trip_id);
		}
		if (this.stop_sequence != otherGStopTime.stop_sequence) {
			return Integer.compare(this.stop_sequence, otherGStopTime.stop_sequence);
		}
		if (!Objects.equals(this.departure_time, otherGStopTime.departure_time)) {
			return this.departure_time.compareTo(otherGStopTime.departure_time);
		}
		throw new MTLog.Fatal("Unexpected stop times to compare: '%s' & '%s'!", this, otherGStopTime);
	}

	@Override
	public String toString() {
		return GStopTime.class.getSimpleName() + "{" +
				"trip_id='" + trip_id + '\'' +
				", stop_id='" + stop_id + '\'' +
				", stop_sequence=" + stop_sequence +
				", arrival_time='" + arrival_time + '\'' +
				", departure_time='" + departure_time + '\'' +
				", stop_headsign='" + stop_headsign + '\'' +
				", pickup_type=" + pickup_type +
				", drop_off_type=" + drop_off_type +
				", uid='" + uid + '\'' +
				'}';
	}
}
