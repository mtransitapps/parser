package org.mtransit.parser.gtfs.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#calendar_fields
public class GCalendar {

	public static final String FILENAME = "calendar.txt";

	public static final String SERVICE_ID = "service_id";
	private String service_id;

	public static final String MONDAY = "monday";
	private boolean monday;

	public static final String TUESDAY = "tuesday";
	private boolean tuesday;

	public static final String WEDNESDAY = "wednesday";
	private boolean wednesday;

	public static final String THURSDAY = "thursday";
	private boolean thursday;

	public static final String FRIDAY = "friday";
	private boolean friday;

	public static final String SATURDAY = "saturday";
	private boolean saturday;

	public static final String SUNDAY = "sunday";
	private boolean sunday;

	public static final String START_DATE = "start_date";
	private int start_date; // YYYYMMDD

	public static final String END_DATE = "end_date";
	private int end_date; // YYYYMMDD

	private ArrayList<GCalendarDate> allDates;

	public GCalendar(String service_id, boolean monday, boolean tuesday, boolean wednesday, boolean thursday, boolean friday, boolean saturday, boolean sunday,
			int start_date, int end_date) {
		this.service_id = service_id;
		this.monday = monday;
		this.tuesday = tuesday;
		this.wednesday = wednesday;
		this.thursday = thursday;
		this.friday = friday;
		this.saturday = saturday;
		this.sunday = sunday;
		this.start_date = start_date;
		this.end_date = end_date;
		initAllDates();
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

	public int getStartDate() {
		return start_date;
	}

	public int getEndDate() {
		return end_date;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(this.service_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.monday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.tuesday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.wednesday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.thursday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.friday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.saturday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.sunday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.start_date).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(this.end_date).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}

	public boolean startsBefore(Integer date) {
		return this.start_date < date;
	}

	public boolean startsBetween(Integer startDate, Integer endDate) {
		return startDate <= this.start_date && this.start_date <= endDate;
	}

	public boolean isOverlapping(Integer startDate, Integer endDate) {
		return startsBetween(startDate, endDate) || endsBetween(startDate, endDate);
	}

	public boolean isInside(Integer startDate, Integer endDate) {
		return startsBetween(startDate, endDate) && endsBetween(startDate, endDate);
	}

	public boolean endsBetween(Integer startDate, Integer endDate) {
		return startDate <= this.end_date && this.end_date <= endDate;
	}

	public boolean endsAfter(Integer date) {
		return this.end_date > date;
	}

	public boolean containsDate(Integer date) {
		return this.start_date <= date && date <= this.end_date;
	}

	public List<GCalendarDate> getDates() {
		if (this.allDates == null) {
			initAllDates();
		}
		return this.allDates;
	}

	private void initAllDates() {
		this.allDates = new ArrayList<GCalendarDate>();
		try {
			SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
			Calendar startDate = Calendar.getInstance();
			startDate.setTime(DATE_FORMAT.parse(String.valueOf(this.start_date)));
			Calendar endDate = Calendar.getInstance();
			endDate.setTime(DATE_FORMAT.parse(String.valueOf(this.end_date)));
			Calendar c = startDate; // no need to clone because not re-using startDate later
			c.add(Calendar.DAY_OF_MONTH, -1); // starting yesterday because increment done at the beginning of the loop
			int date;
			while (c.before(endDate)) {
				c.add(Calendar.DAY_OF_MONTH, +1);
				try {
					date = Integer.valueOf(DATE_FORMAT.format(c.getTime()));
					switch (c.get(Calendar.DAY_OF_WEEK)) {
					case Calendar.MONDAY:
						if (this.monday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.TUESDAY:
						if (this.tuesday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.WEDNESDAY:
						if (this.wednesday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.THURSDAY:
						if (this.thursday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.FRIDAY:
						if (this.friday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.SATURDAY:
						if (this.saturday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					case Calendar.SUNDAY:
						if (this.sunday) {
							this.allDates.add(new GCalendarDate(this.service_id, date, GCalendarDatesExceptionType.SERVICE_ADDED));
							continue; // service today
						}
						break;
					}
				} catch (Exception e) {
					System.out.printf("\nError while parsing date '%s'!\n", c);
					e.printStackTrace();
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			System.out.printf("\nError while parsing dates between '%s' and '%s'!\n", this.start_date, this.end_date);
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
