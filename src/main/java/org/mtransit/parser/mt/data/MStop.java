package org.mtransit.parser.mt.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;

import java.util.Objects;

public class MStop implements Comparable<MStop> {

	private int id;

	@NotNull
	private String code;
	@NotNull
	private String name;

	private double lat;
	private double lng;

	@Nullable
	private String originalId;

	public MStop(int id, @NotNull String code, @Nullable String originalId, @NotNull String name, double lat, double lng) {
		this.id = id;
		this.code = code;
		this.originalId = originalId;
		this.name = name;
		this.lat = lat;
		this.lng = lng;
	}

	public int getId() {
		return id;
	}

	@NotNull
	@SuppressWarnings("unused")
	public String getCode() {
		return code;
	}

	@NotNull
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
		return printString();
	}

	public String printString() {
		StringBuilder sb = new StringBuilder(); //
		sb.append(this.id); // ID
		sb.append(Constants.COLUMN_SEPARATOR); //
		sb.append(Constants.STRING_DELIMITER).append(this.code).append(Constants.STRING_DELIMITER); // code
		if (DefaultAgencyTools.EXPORT_ORIGINAL_ID) {
			sb.append(Constants.COLUMN_SEPARATOR); //
			sb.append(Constants.STRING_DELIMITER).append(this.originalId == null ? Constants.EMPTY : this.originalId).append(Constants.STRING_DELIMITER); //
		}
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
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		MStop o = (MStop) obj;
		if (this.id != o.id) {
			return false;
		}
		if (!this.code.equals(o.code)) {
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
		//noinspection RedundantIfStatement
		if (!Objects.equals(this.originalId, o.originalId)) {
			return false;
		}
		return true;
	}
}
