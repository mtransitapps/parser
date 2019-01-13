package org.mtransit.parser.mt.data;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;

public class MTrip implements Comparable<MTrip> {

	public static final int HEADSIGN_TYPE_STRING = 0;
	public static final int HEADSIGN_TYPE_DIRECTION = 1;
	public static final int HEADSIGN_TYPE_INBOUND = 2;
	public static final int HEADSIGN_TYPE_STOP_ID = 3;
	public static final int HEADSIGN_TYPE_DESCENT_ONLY = 4;

	private int headsignType = HEADSIGN_TYPE_STRING; // 0=string, 1=direction, 2=inbound, 3=stopId, 4=descent-only
	private String headsignValue = Constants.EMPTY;
	private int headsignId = 0;
	private long routeId;

	private long id = -1;
	private String idString = null;

	public MTrip(long routeId) {
		this.routeId = routeId;
	}

	private static final String ZERO = "0";

	public long getId() {
		if (this.id < 0) {
			this.id = getNewId(this.routeId, this.headsignId);
		}
		return this.id;
	}

	public static long getNewId(long routeId, int headsignId) {
		return Long.parseLong(routeId + ZERO + headsignId);
	}

	public String getIdString() {
		if (this.idString == null) {
			this.idString = this.routeId + Constants.UUID_SEPARATOR + this.headsignValue;
		}
		return this.idString;
	}

	public MTrip setHeadsignStringNotEmpty(String headsignString, int headsignId) {
		if (StringUtils.isEmpty(headsignString) || headsignId < 0) {
			System.out.printf("\nInvalid trip head sign string '%s' or ID '%s' (%s)!\n", headsignString, headsignId, this);
			System.exit(-1);
			return this;
		}
		return setHeadsignString(headsignString, headsignId);
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
		this.headsignValue = direction.getId();
		this.idString = null; // reset
		this.headsignId = direction.intValue();
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignInbound(MInboundType inbound) {
		this.headsignType = HEADSIGN_TYPE_INBOUND;
		this.headsignValue = inbound.getId();
		this.idString = null; // reset
		this.headsignId = Integer.valueOf(inbound.getId());
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignStop(MStop stop) {
		this.headsignType = HEADSIGN_TYPE_STOP_ID;
		this.headsignValue = String.valueOf(stop.getId());
		this.idString = null; // reset
		this.headsignId = stop.getId();
		this.id = -1; // reset
		return this;
	}

	public MTrip setHeadsignDescentOnly() {
		this.headsignType = HEADSIGN_TYPE_DESCENT_ONLY;
		this.headsignValue = null;
		this.idString = null; // reset
		this.headsignId = 0;
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

	public static boolean mergeEmpty(MTrip mTrip, MTrip mTripToMerge) {
		if (StringUtils.isEmpty(mTrip.getHeadsignValue())) {
			mTrip.setHeadsignString(mTripToMerge.getHeadsignValue(), mTrip.getHeadsignId());
			return true; // merged
		} else if (StringUtils.isEmpty(mTripToMerge.getHeadsignValue())) {
			mTrip.setHeadsignString(mTrip.getHeadsignValue(), mTrip.getHeadsignId());
			return true; // merged
		} else {
			return false; // not merged
		}
	}

	private static final String SLASH = " / ";

	public boolean mergeHeadsignValue(MTrip mTripToMerge) {
		if (mTripToMerge == null || mTripToMerge.headsignValue == null) {
			System.out.printf("\n%s: mergeHeadsignValue() > no trip heading value to merge > %s.", this.routeId, this.headsignValue);
			return true;
		}
		if (this.headsignValue == null) {
			this.headsignValue = mTripToMerge.headsignValue;
			System.out.printf("\n%s: mergeHeadsignValue() > no current headsign value > %s.", this.routeId, this.headsignValue);
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
			this.headsignValue = mTripToMerge.headsignValue + SLASH + this.headsignValue;
		} else {
			this.headsignValue = this.headsignValue + SLASH + mTripToMerge.headsignValue;
		}
		System.out.printf("\n%s: mergeHeadsignValue() > merge 2 headsign value > %s.", this.routeId, this.headsignValue);
		return true;
	}

	public static String mergeHeadsignValue(String mTripHeadsign, String mTripHeadsignToMerge) {
		if (mTripHeadsignToMerge == null || mTripHeadsignToMerge.length() == 0) {
			return mTripHeadsign;
		}
		if (mTripHeadsign == null || mTripHeadsign.length() == 0) {
			return mTripHeadsignToMerge;
		}
		if (mTripHeadsignToMerge.contains(mTripHeadsign)) {
			return mTripHeadsignToMerge;
		}
		if (mTripHeadsign.contains(mTripHeadsignToMerge)) {
			return mTripHeadsign;
		}
		if (mTripHeadsign.compareTo(mTripHeadsignToMerge) > 0) {
			return mTripHeadsignToMerge + SLASH + mTripHeadsign;
		} else {
			return mTripHeadsign + SLASH + mTripHeadsignToMerge;
		}
	}

	@Override
	public String toString() {
		return printString();
	}

	public String printString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(getId()); // ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
			sb.append(this.headsignType); // HEADSIGN TYPE
			sb.append(Constants.COLUMN_SEPARATOR); //
			sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.headsignValue)).append(Constants.STRING_DELIMITER); // HEADSIGN STRING
			sb.append(Constants.COLUMN_SEPARATOR); //
		} else {
			if (this.headsignType == HEADSIGN_TYPE_DESCENT_ONLY) {
				sb.append(HEADSIGN_TYPE_STRING); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape("Descent Only")).append(Constants.STRING_DELIMITER); // HEADSIGN STRING
				sb.append(Constants.COLUMN_SEPARATOR); //
			} else {
				sb.append(this.headsignType); // HEADSIGN TYPE
				sb.append(Constants.COLUMN_SEPARATOR); //
				sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.headsignValue)).append(Constants.STRING_DELIMITER); // HEADSIGN STRING
				sb.append(Constants.COLUMN_SEPARATOR); //
			}

		}
		sb.append(this.routeId); // ROUTE ID
		return sb.toString();
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
