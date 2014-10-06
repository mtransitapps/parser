package org.mtransit.parser.mt.data;

public class MServiceDate implements Comparable<MServiceDate> {

	public String serviceId;
	public int calendarDate;

	public MServiceDate(String serviceId, int calendarDate) {
		this.serviceId = serviceId;
		this.calendarDate = calendarDate;
	}

	@Override
	public int compareTo(MServiceDate otherServiceDate) {
		final int cd = calendarDate - otherServiceDate.calendarDate;
		if (cd != 0) {
			return cd;
		}
		return serviceId.compareToIgnoreCase(otherServiceDate.serviceId);
	}

	@Override
	public boolean equals(Object obj) {
		MServiceDate ts = (MServiceDate) obj;
		if (ts.serviceId != null && !ts.serviceId.equals(serviceId)) {
			return false;
		}
		if (ts.calendarDate != 0 && ts.calendarDate != calendarDate) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append('\'').append(MSpec.escape(serviceId)).append('\'').append(',') // service ID
				.append(calendarDate) // calendar date
				.toString();
	}

}
