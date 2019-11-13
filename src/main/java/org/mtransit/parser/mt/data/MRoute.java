package org.mtransit.parser.mt.data;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;

public class MRoute implements Comparable<MRoute> {

	private long id;
	private String shortName;
	private String longName;

	private String color;

	public MRoute(long id, String shortName, String longName, String color) {
		this.id = id;
		this.shortName = shortName;
		this.longName = longName;
		this.color = color;
	}

	public long getId() {
		return id;
	}

	public String getShortName() {
		return shortName;
	}

	public String getLongName() {
		return longName;
	}

	public void setLongName(String longName) {
		this.longName = longName;
	}

	public String getColor() {
		return color;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(this.id) // ID
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER) //
				.append(this.shortName == null ? Constants.EMPTY : CleanUtils.escape(this.shortName)) // short name
				.append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER) //
				.append(this.longName == null ? Constants.EMPTY : CleanUtils.escape(this.longName)) // long name
				.append(Constants.STRING_DELIMITER) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER) //
				.append(this.color == null ? Constants.EMPTY : this.color) // color
				.append(Constants.STRING_DELIMITER) // color
				.toString();
	}

	@Override
	public int compareTo(MRoute otherRoute) {
		return Long.compare(this.id, otherRoute.id);
	}

	@Override
	public boolean equals(Object obj) {
		MRoute o = (MRoute) obj;
		if (this.id != o.id) {
			return false;
		}
		if (!StringUtils.equals(this.shortName, o.shortName)) {
			return false;
		}
		if (!StringUtils.equals(this.longName, o.longName)) {
			return false;
		}
		return true;
	}

	public boolean equalsExceptLongName(Object obj) {
		MRoute o = (MRoute) obj;
		if (this.id != o.id) {
			return false;
		}
		if (!StringUtils.equals(this.shortName, o.shortName)) {
			return false;
		}
		return true;
	}

	private static final String SLASH = " / ";

	public boolean mergeLongName(MRoute mRouteToMerge) {
		if (mRouteToMerge == null || mRouteToMerge.longName == null) {
			return true;
		}
		if (this.longName == null) {
			this.longName = mRouteToMerge.longName;
			return true;
		}
		if (mRouteToMerge.longName.contains(this.longName)) {
			this.longName = mRouteToMerge.longName;
			return true;
		}
		if (this.longName.contains(mRouteToMerge.longName)) {
			return true;
		}
		if (this.longName.compareTo(mRouteToMerge.longName) > 0) {
			this.longName = mRouteToMerge.longName + SLASH + this.longName;
		} else {
			this.longName = this.longName + SLASH + mRouteToMerge.longName;
		}
		return true;
	}

	public boolean simpleMergeLongName(MRoute mRouteToMerge) {
		if (mRouteToMerge == null || mRouteToMerge.longName == null) {
			return true;
		}
		if (this.longName == null) {
			return true;
		}
		if (mRouteToMerge.longName.contains(this.longName)) {
			return true;
		}
		if (this.longName.contains(mRouteToMerge.longName)) {
			return true;
		}
		return false;
	}
}
