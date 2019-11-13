package org.mtransit.parser.gtfs.data;

import java.util.Collection;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#calendar_dates_fields
public class GCalendarDate {

	public static final String FILENAME = "calendar_dates.txt";

	public static final String SERVICE_ID = "service_id";
	private String service_id;

	public static final String DATE = "date";
	private int date; // YYYYMMDD

	public static final String EXCEPTION_DATE = "exception_type";
	private GCalendarDatesExceptionType exception_type;

	public GCalendarDate(String service_id, int date, GCalendarDatesExceptionType exception_type) {
		this.service_id = service_id;
		this.date = date;
		this.exception_type = exception_type;
	}

	public String getServiceId() {
		return service_id;
	}

	public boolean isServiceId(String serviceId) {
		return this.service_id.equals(serviceId);
	}

	public boolean isServiceIds(Collection<String> serviceIds) {
		return serviceIds.contains(this.service_id);
	}

	public int getDate() {
		return date;
	}

	public boolean isBefore(Integer date) {
		return this.date < date;
	}

	public boolean isBetween(Integer startDate, Integer endDate) {
		return startDate <= this.date && this.date <= endDate;
	}

	public boolean is(Integer date) {
		return this.date == date;
	}

	public boolean isAfter(Integer date) {
		return this.date > date;
	}

	public GCalendarDatesExceptionType getExceptionType() {
		return exception_type;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.service_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.date).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.exception_type).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}

	public String getUID() {
		return this.date + this.service_id;
	}
}
