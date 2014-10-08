package org.mtransit.parser.mt.data;

import org.apache.commons.lang3.StringUtils;

public class MRoute implements Comparable<MRoute> {

	public int id;
	public String shortName;
	public String longName;

	public String color;
	public String textColor;

	public MRoute(int id, String shortName, String longName) {
		this.id = id;
		this.shortName = shortName;
		this.longName = longName;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(id).append(',') // ID
				.append('\'').append(shortName == null ? "" : shortName).append('\'').append(',') // short name
				.append('\'').append(longName == null ? "" : MSpec.escape(longName)).append('\'').append(',') // long name
				.append('\'').append(color == null ? "" : color).append('\'').append(',') // color
				.append('\'').append(textColor == null ? "" : textColor).append('\'') // text color
				.toString();
	}

	@Override
	public int compareTo(MRoute otherRoute) {
		return id - otherRoute.id;
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
}
