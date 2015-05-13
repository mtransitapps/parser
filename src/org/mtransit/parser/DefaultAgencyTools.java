package org.mtransit.parser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

public class DefaultAgencyTools implements GAgencyTools {

	public static final int THREAD_POOL_SIZE = 1;

	public static void main(String[] args) {
		new DefaultAgencyTools().start(args);
	}

	public void start(String[] args) {
		System.out.printf("Generating agency data...\n");
		long start = System.currentTimeMillis();
		// GTFS parsing
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this);
		gtfs.tripStops = GReader.extractTripStops(gtfs);
		Map<Long, GSpec> gtfsByMRouteId = GReader.splitByRouteId(gtfs, this);
		// Objects generation
		MSpec mSpec = MGenerator.generateMSpec(gtfsByMRouteId, gtfs.stops, this);
		// Dump to files
		MGenerator.dumpFiles(mSpec, args[1], args[2]);
		System.out.printf("Generating agency data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public String getAgencyColor() {
		return null;
	}

	@Override
	public Integer getAgencyRouteType() {
		return null;
	}

	@Override
	public String cleanServiceId(String serviceId) {
		return serviceId;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		try {
			return Long.valueOf(gRoute.route_id);
		} catch (Exception e) {
			System.out.println("Error while extracting route ID from " + gRoute);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public String getRouteShortName(GRoute gRoute) {
		return gRoute.route_short_name;
	}

	@Override
	public String getRouteLongName(GRoute gRoute) {
		return MSpec.cleanLabel(gRoute.route_long_name);
	}

	@Override
	public boolean mergeRouteLongName(MRoute mRoute, MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (getAgencyColor() != null && getAgencyColor().equals(gRoute.route_color)) {
			return null;
		}
		return gRoute.route_color;
	}

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (getAgencyRouteType() == null) {
			System.out.println("ERROR: unspecified agency route type '" + getAgencyRouteType() + "'!");
			System.exit(-1);
		}
		if (getAgencyRouteType() != gRoute.route_type) {
			return true;
		}
		return false;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip) {
		if (gTrip.direction_id < 0 || gTrip.direction_id > 1) {
			System.out.println("ERROR: default agency implementation required 'direction_id' field in 'trips.txt'!");
			System.exit(-1);
		}
		try {
			mTrip.setHeadsignString(gTrip.trip_headsign, gTrip.direction_id);
		} catch (NumberFormatException nfe) {
			System.out.println("ERROR: default agency implementation required integer 'direction_id' field in 'trips.txt'!");
			System.exit(-1);
		}
	}

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		return tripHeadsign;
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		return mTrip.mergeHeadsignValue(mTripToMerge);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		return false;
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		return false;
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		return false;
	}

	@Override
	public String cleanStopName(String gStopName) {
		return MSpec.cleanLabel(gStopName);
	}

	public String cleanStopNameFR(String stopName) {
		return MSpec.cleanLabelFR(stopName);
	}

	@Override
	public String getStopCode(GStop gStop) {
		return gStop.stop_code;
	}

	@Override
	public int getStopId(GStop gStop) {
		try {
			return Integer.parseInt(gStop.stop_id);
		} catch (Exception e) {
			System.out.println("Error while extracting stop ID from " + gStop);
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public int compare(MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		return 0; // nothing
	}

	@Override
	public boolean excludeStop(GStop gStop) {
		return false;
	}

	@Override
	public int getThreadPoolSize() {
		return THREAD_POOL_SIZE;
	}

	private static final int PRECISON_IN_SECONDS = 10;
	private static final String TIME_SEPARATOR = ":";
	private static final SimpleDateFormat HHMMSS = new SimpleDateFormat("HHmmss");


	@Override
	public int getDepartureTime(GStopTime gStopTime, List<GStopTime> gStopTimes) {
		String departureTimeS = null;
		if (StringUtils.isEmpty(gStopTime.departure_time)) {
			try {
				Integer newDepartureTime = null;
				Integer previousDepartureTime = null;
				Integer previousDepartureTimeStopSequence = null;
				Integer nextDepartureTime = null;
				Integer nextDepartureTimeStopSequence = null;
				for (GStopTime aStopTime : gStopTimes) {
					if (!gStopTime.trip_id.equals(aStopTime.trip_id)) {
						continue;
					}
					if (aStopTime.stop_sequence < gStopTime.stop_sequence) {
						if (!StringUtils.isEmpty(aStopTime.departure_time)) {
							newDepartureTime = Integer.valueOf(aStopTime.departure_time.replaceAll(TIME_SEPARATOR, Constants.EMPTY));
							if (previousDepartureTime == null || previousDepartureTimeStopSequence == null
									|| previousDepartureTimeStopSequence < aStopTime.stop_sequence) {
								previousDepartureTime = newDepartureTime;
								previousDepartureTimeStopSequence = aStopTime.stop_sequence;
							}
						}
					} else if (aStopTime.stop_sequence > gStopTime.stop_sequence) {
						if (!StringUtils.isEmpty(aStopTime.departure_time)) {
							newDepartureTime = Integer.valueOf(aStopTime.departure_time.replaceAll(TIME_SEPARATOR, Constants.EMPTY));
							if (nextDepartureTime == null || nextDepartureTimeStopSequence == null || nextDepartureTimeStopSequence > aStopTime.stop_sequence) {
								nextDepartureTime = newDepartureTime;
								nextDepartureTimeStopSequence = aStopTime.stop_sequence;
							}
						}
					}
				}
				long previousDepartureTimeInMs = HHMMSS.parse(String.valueOf(previousDepartureTime)).getTime();
				long nextDepartureTimeInMs = HHMMSS.parse(String.valueOf(nextDepartureTime)).getTime();
				long timeDiffInMs = nextDepartureTimeInMs - previousDepartureTimeInMs;
				int nbStop = nextDepartureTimeStopSequence - previousDepartureTimeStopSequence;
				long timeBetweenStopInMs = timeDiffInMs / nbStop;
				long departureTimeInMs = previousDepartureTimeInMs + (timeBetweenStopInMs * (gStopTime.stop_sequence - previousDepartureTimeStopSequence));
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(departureTimeInMs);
				departureTimeS = HHMMSS.format(calendar.getTime());
				if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
					departureTimeS = String.valueOf(240000 + Integer.valueOf(departureTimeS));
				}
			} catch (Exception e) {
				System.out.println("Error while interpolating departure time for " + gStopTime + "!");
				e.printStackTrace();
				System.exit(-1);
				departureTimeS = null;
			}
		} else {
			departureTimeS = gStopTime.departure_time.replaceAll(TIME_SEPARATOR, Constants.EMPTY);
		}
		Integer departureTime = Integer.valueOf(departureTimeS);
		int extraSeconds = departureTime == null ? 0 : departureTime.intValue() % PRECISON_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			return cleanDepartureTime(departureTimeS, extraSeconds);
		}
		return departureTime; // GTFS standard
	}

	private static final String CLEAN_DEPARTURE_TIME_FORMAT = "%02d";
	private static final String CLEAN_DEPARTURE_TIME_LEADING_ZERO = "0";
	private static final String CLEAN_DEPARTURE_TIME_DEFAULT_MINUTES = "00";
	private static final String CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS = "00";

	private int cleanDepartureTime(String departureTimeS, int extraSeconds) {
		try {
			while (departureTimeS.length() < 6) {
				departureTimeS = CLEAN_DEPARTURE_TIME_LEADING_ZERO + departureTimeS;
			}
			String newHours = departureTimeS.substring(0, 2);
			String newMinutes = departureTimeS.substring(2, 4);
			String newSeconds = departureTimeS.substring(4, 6);
			int seconds = Integer.parseInt(newSeconds);
			if (extraSeconds < 5) {
				if (extraSeconds > seconds) {
					newSeconds = CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS;
				} else {
					newSeconds = String.format(CLEAN_DEPARTURE_TIME_FORMAT, seconds - extraSeconds);
				}
				return Integer.valueOf(newHours + newMinutes + newSeconds);
			}
			int secondsToAdd = PRECISON_IN_SECONDS - extraSeconds;
			if (seconds + secondsToAdd < 60) {
				newSeconds = String.format(CLEAN_DEPARTURE_TIME_FORMAT, seconds + secondsToAdd);
				return Integer.valueOf(newHours + newMinutes + newSeconds);
			}
			newSeconds = CLEAN_DEPARTURE_TIME_DEFAULT_SECONDS;
			int minutes = Integer.parseInt(newMinutes);
			if (minutes + 1 < 60) {
				newMinutes = String.format(CLEAN_DEPARTURE_TIME_FORMAT, minutes + 1);
				return Integer.valueOf(newHours + newMinutes + newSeconds);
			}
			newMinutes = CLEAN_DEPARTURE_TIME_DEFAULT_MINUTES;
			int hours = Integer.parseInt(newHours);
			newHours = String.valueOf(hours + 1);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		} catch (Exception e) {
			System.out.println("Error while cleaning departure time '" + departureTimeS + "' '" + extraSeconds + "' !");
			e.printStackTrace();
			System.exit(-1);
			return -1;
		}
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.start_time.replaceAll(TIME_SEPARATOR, Constants.EMPTY)); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.end_time.replaceAll(TIME_SEPARATOR, Constants.EMPTY)); // GTFS standard
	}

	private static final int MIN_COVERAGE_AFTER_TODAY_IN_DAYS = 1;

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools) {
		System.out.printf("Extracting useful service IDs...\n");
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools);
		Integer startDate = null;
		Integer endDate = null;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		Integer todayStringInt = Integer.valueOf(simpleDateFormat.format(new Date()));
		todayStringInt++; // TOMORROW (too late to publish today's schedule)
		if (gtfs.calendars != null && gtfs.calendars.size() > 0) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if (gCalendar.start_date <= todayStringInt && gCalendar.end_date >= todayStringInt) {
					if (startDate == null || gCalendar.start_date < startDate) {
						startDate = gCalendar.start_date;
					}
					if (endDate == null || gCalendar.end_date > endDate) {
						endDate = gCalendar.end_date;
					}
				}
			}
		} else if (gtfs.calendarDates != null && gtfs.calendarDates.size() > 0) {
			HashSet<String> todayServiceIds = new HashSet<String>();
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.date == todayStringInt) {
					todayServiceIds.add(gCalendarDate.service_id);
				}
			}
			if (todayServiceIds != null && todayServiceIds.size() > 0) {
				for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
					for (String todayServiceId : todayServiceIds) {
						if (gCalendarDate.service_id.equals(todayServiceId)) {
							if (startDate == null || gCalendarDate.date < startDate) {
								startDate = gCalendarDate.date;
							}
							if (endDate == null || gCalendarDate.date > endDate) {
								endDate = gCalendarDate.date;
							}
						}
					}
				}
				if (endDate - startDate < MIN_COVERAGE_AFTER_TODAY_IN_DAYS) {
					endDate = startDate + MIN_COVERAGE_AFTER_TODAY_IN_DAYS;
				}
			} else {
				System.out.println("NO schedule available for " + todayStringInt + "!");
				System.exit(-1);
				return null;
			}
		} else {
			System.out.println("NO schedule available for " + todayStringInt + "!");
			System.exit(-1);
			return null;
		}
		System.out.println("Generated on " + todayStringInt + " | Schedules from " + startDate + " to " + endDate);
		HashSet<String> serviceIds = new HashSet<String>();
		if (gtfs.calendars != null) {
			for (GCalendar gCalendar : gtfs.calendars) {
				if ((gCalendar.start_date >= startDate && gCalendar.start_date <= endDate) //
						|| (gCalendar.end_date >= startDate && gCalendar.end_date <= endDate)) {
					serviceIds.add(gCalendar.service_id);
				}
			}
		}
		if (gtfs.calendarDates != null) {
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.date >= startDate && gCalendarDate.date <= endDate) {
					serviceIds.add(gCalendarDate.service_id);
				}
			}
		}
		System.out.println("Service IDs: " + serviceIds);
		gtfs = null;
		System.out.printf("Extracting useful service IDs... DONE\n");
		return serviceIds;
	}

	public static boolean excludeUselessCalendar(GCalendar gCalendar, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gCalendar.service_id);
	}

	public static boolean excludeUselessCalendarDate(GCalendarDate gCalendarDate, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gCalendarDate.service_id);
	}

	public static boolean excludeUselessTrip(GTrip gTrip, HashSet<String> serviceIds) {
		if (serviceIds == null) {
			return false; // keep
		}
		return !serviceIds.contains(gTrip.service_id);
	}

}
