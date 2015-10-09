package org.mtransit.parser.mt.data;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;

public class MStop implements Comparable<MStop> {

	private int id;

	private String code;
	private String name;

	private double lat;
	private double lng;

	public MStop(int id, String code, String name, double lat, double lng) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
	}

	public int getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public double getLat() {
		return lat;
	}

	public boolean hasLat() {
		return lat != 0.0d;
	}

	public double getLng() {
		return lng;
	}

	public boolean hasLng() {
		return lng != 0.0d;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(this.id); // ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER).append(this.code == null ? Constants.EMPTY : this.code).append(Constants.STRING_DELIMITER);// code
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.name)).append(Constants.STRING_DELIMITER); // name
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.lat); // latitude
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(this.lng); // longitude
		return sb.toString();
	}

	@Override
	public int compareTo(MStop otherStop) {
		return id - otherStop.id;
	}

	@Override
	public boolean equals(Object obj) {
		MStop o = (MStop) obj;
		if (this.id != o.id) {
			return false;
		}
		if (this.code != null && !this.code.equals(o.code)) {
			return false;
		}
		if (!this.name.equals(o.name)) {
			return false;
		}
		if (this.lat != o.lat) {
			return false;
		}
		if (this.lng != o.lng) {
			return false;
		}
		return true;
	}
}
