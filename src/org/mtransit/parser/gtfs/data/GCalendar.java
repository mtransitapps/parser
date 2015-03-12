package org.mtransit.parser.gtfs.data;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#calendar_fields
public class GCalendar {

	public static final String FILENAME = "calendar.txt";

	public static final String SERVICE_ID = "service_id";
	public String service_id;

	public static final String MONDAY = "monday";
	public boolean monday;

	public static final String TUESDAY = "tuesday";
	public boolean tuesday;

	public static final String WEDNESDAY = "wednesday";
	public boolean wednesday;

	public static final String THURSDAY = "thursday";
	public boolean thursday;

	public static final String FRIDAY = "friday";
	public boolean friday;

	public static final String SATURDAY = "saturday";
	public boolean saturday;

	public static final String SUNDAY = "sunday";
	public boolean sunday;

	public static final String START_DATE = "start_date";
	public int start_date; // YYYYMMDD

	public static final String END_DATE = "end_date";
	public int end_date; // YYYYMMDD

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

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(service_id).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(monday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(tuesday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(wednesday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(thursday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(friday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(saturday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(sunday).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(start_date).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.append(Constants.STRING_DELIMITER).append(end_date).append(Constants.STRING_DELIMITER).append(Constants.COLUMN_SEPARATOR) //
				.toString();
	}

	// NOT THREAD SAFE
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	public List<GCalendarDate> getDates() {
		if (this.allDates == null) {
			initAllDates();
		}
		return this.allDates;
	}

	private void initAllDates() {
		this.allDates = new ArrayList<GCalendarDate>();
		try {
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
					System.out.println("Error while parsing date '" + c + "'!");
					e.printStackTrace();
					System.exit(-1);
				}
			}
		} catch (Exception e) {
			System.out.println("Error while parsing dates between '" + this.start_date + "' and '" + this.end_date + "'!");
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
