package org.mtransit.parser.mt.data;

import org.mtransit.parser.Constants;

public class MStop implements Comparable<MStop> {

	public int id;

	public String code;
	public String name;

	public String lat;
	public String lng;

	public MStop(int id, String code, String name, String lat, String lng) {
		this.id = id;
		this.code = code;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(id); // ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER).append(code == null ? Constants.EMPTY : code).append(Constants.STRING_DELIMITER);// code
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER).append(MSpec.escape(name)).append(Constants.STRING_DELIMITER); // name
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(lat); // lat
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(lng); // lng
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
		if (!this.lat.equals(o.lat)) {
			return false;
		}
		if (!this.lng.equals(o.lng)) {
			return false;
		}
		return true;
	}
}
