package org.mtransit.parser.mt.data;

import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.Constants;

public class MServiceDate implements Comparable<MServiceDate> {

	public String serviceId;
	public int calendarDate;

	public MServiceDate(String serviceId, int calendarDate) {
		this.serviceId = serviceId;
		this.calendarDate = calendarDate;
	}

	@Override
	public int compareTo(MServiceDate otherServiceDate) {
		int cd = this.calendarDate - otherServiceDate.calendarDate;
		if (cd != 0) {
			return cd;
		}
		return this.serviceId.compareToIgnoreCase(otherServiceDate.serviceId);
	}

	@Override
	public boolean equals(Object obj) {
		MServiceDate ts = (MServiceDate) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(this.serviceId)) {
			return false;
		}
		if (ts.calendarDate != 0 && ts.calendarDate != this.calendarDate) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return (this.serviceId == null ? 0 : this.serviceId.hashCode()) + this.calendarDate;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(CleanUtils.escape(this.serviceId)).append(Constants.STRING_DELIMITER) // service ID
				.append(Constants.COLUMN_SEPARATOR) //
				.append(this.calendarDate) // calendar date
				.toString();
	}
}
