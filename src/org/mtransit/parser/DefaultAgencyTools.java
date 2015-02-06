package org.mtransit.parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

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
	public long getRouteId(GRoute gRoute) {
		return Long.valueOf(gRoute.route_id);
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
		return gRoute.route_color;
	}


	@Override
	public boolean excludeRoute(GRoute gRoute) {
		return false;
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip) {
		if (gTrip.direction_id == null | gTrip.direction_id.length() == 0) {
			System.out.println("ERROR: default agency implementation required 'direction_id' field in 'trips.txt'!");
			System.exit(-1);
		}
		try {
			mTrip.setHeadsignString(gTrip.trip_headsign, Integer.valueOf(gTrip.direction_id));
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
		return Integer.parseInt(gStop.stop_id);
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

	@Override
	public int getDepartureTime(GStopTime gStopTime) {
		String departureTimeS = gStopTime.departure_time.replaceAll(":", "");
		Integer departureTime = Integer.valueOf(departureTimeS);
		int extraSeconds = departureTime == null ? 0 : departureTime.intValue() % PRECISON_IN_SECONDS;
		if (extraSeconds > 0) { // IF too precise DO
			departureTime = cleanDepartureTime(departureTimeS, departureTime.intValue(), extraSeconds);
		}
		return departureTime; // GTFS standard
	}

	private int cleanDepartureTime(String departureTimeS, int departureTime, int extraSeconds) {
		while (departureTimeS.length() < 6) {
			departureTimeS = "0" + departureTimeS;
		}
		String newHours = departureTimeS.substring(0, 2);
		String newMinutes = departureTimeS.substring(2, 4);
		String newSeconds = departureTimeS.substring(4, 6);
		int seconds = Integer.parseInt(newSeconds);
		if (extraSeconds < 5) {
			// remove seconds
			newSeconds = String.format("%02d", seconds - extraSeconds);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add seconds
		int secondsToAdd = PRECISON_IN_SECONDS - extraSeconds;
		if (seconds + secondsToAdd < 60) {
			newSeconds = String.format("%02d", seconds + secondsToAdd);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add minute
		newSeconds = "00";
		int minutes = Integer.parseInt(newMinutes);
		if (minutes + 1 < 60) {
			newMinutes = String.format("%02d", minutes + 1);
			return Integer.valueOf(newHours + newMinutes + newSeconds);
		}
		// add hour
		newMinutes = "00";
		int hours = Integer.parseInt(newHours);
		newHours = String.valueOf(hours + 1);
		return Integer.valueOf(newHours + newMinutes + newSeconds);
	}

	@Override
	public int getStartTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.start_time.replaceAll(":", "")); // GTFS standard
	}

	@Override
	public int getEndTime(GFrequency gFrequency) {
		return Integer.valueOf(gFrequency.end_time.replaceAll(":", "")); // GTFS standard
	}

	public static HashSet<String> extractUsefulServiceIds(String[] args, DefaultAgencyTools agencyTools) {
		System.out.printf("Extracting useful service IDs...\n");
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools);
		Integer startDate = null;
		Integer endDate = null;
		Integer todayStringInt = Integer.valueOf(new SimpleDateFormat("yyyyMMdd").format(new Date()));
		if (gtfs.calendars != null) {
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
		} else if (gtfs.calendarDates != null) {
			String todayServiceId = null;
			for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
				if (gCalendarDate.date == todayStringInt) {
					todayServiceId = gCalendarDate.service_id;
					break;
				}
			}
			if (todayServiceId != null) {
				for (GCalendarDate gCalendarDate : gtfs.calendarDates) {
					if (gCalendarDate.service_id.equals(todayServiceId)) {
						if (startDate == null || gCalendarDate.date < startDate) {
							startDate = gCalendarDate.date;
						}
						if (endDate == null || gCalendarDate.date > endDate) {
							endDate = gCalendarDate.date;
						}
					}
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
