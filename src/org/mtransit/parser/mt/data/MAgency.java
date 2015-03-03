package org.mtransit.parser.mt.data;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.Constants;

public class MAgency implements Comparable<MAgency> {

	public static final int ROUTE_TYPE_SUBWAY = 1;
	public static final int ROUTE_TYPE_TRAIN = 2;
	public static final int ROUTE_TYPE_BUS = 3;

	private String id;
	private String timezone;
	private String color;
	private Integer type;

	public MAgency(String id, String timezone, String color, Integer type) {
		this.id = id;
		this.timezone = timezone;
		this.color = color;
		this.type = type;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(this.id) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(this.timezone) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(this.color) //
				.append(Constants.COLUMN_SEPARATOR) //
				.append(this.type) //
				.toString();

	}

	public String getId() {
		return id;
	}

	public String getTimezone() {
		return timezone;
	}

	public String getColor() {
		return color;
	}

	public Integer getType() {
		return type;
	}

	@Override
	public int compareTo(MAgency otherAgency) {
		if (otherAgency == null) {
			return +1;
		}
		if (this.id != null) {
			if (this.id.equals(otherAgency.id)) {
				return this.id.compareTo(otherAgency.id);
			}
		} else if (otherAgency.id != null) {
			return -1;
		}
		if (this.timezone != null) {
			if (this.timezone.equals(otherAgency.timezone)) {
				return this.timezone.compareTo(otherAgency.timezone);
			}
		} else if (otherAgency.timezone != null) {
			return -1;
		}
		if (this.color != null) {
			if (this.color.equals(otherAgency.color)) {
				return this.color.compareTo(otherAgency.color);
			}
		} else if (otherAgency.color != null) {
			return -1;
		}
		if (this.type != null) {
			if (this.type.equals(otherAgency.type)) {
				return this.type.compareTo(otherAgency.type);
			}
		} else if (otherAgency.type != null) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		MAgency o = (MAgency) obj;
		if (!StringUtils.equals(this.id, o.id)) {
			return false;
		}
		if (!StringUtils.equals(this.timezone, o.timezone)) {
			return false;
		}
		if (!StringUtils.equals(this.color, o.color)) {
			return false;
		}
		if (this.type != null) {
			if (!this.type.equals(o.type)) {
				return false;
			}
		} else if (o.type != null) {
			return false;
		}
		return true;
	}
}
