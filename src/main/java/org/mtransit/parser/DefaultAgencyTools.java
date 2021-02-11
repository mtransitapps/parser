package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CommonsApp;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.GReader;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GDropOffType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GRouteType;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"RedundantSuppression"})
public class DefaultAgencyTools implements GAgencyTools {

	static {
		CommonsApp.setup(false);
	}

	@SuppressWarnings("WeakerAccess")
	protected static final boolean EXCLUDE = true;
	@SuppressWarnings("WeakerAccess")
	protected static final boolean KEEP = false;

	private static final int MAX_NEXT_LOOKUP_IN_DAYS = 60;

	private static final int MAX_LOOK_BACKWARD_IN_DAYS = 7;
	private static final int MAX_LOOK_FORWARD_IN_DAYS = 60;

	private static final int MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS = 5;
	private static final int MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS = 14;

	private static final int MIN_PREVIOUS_NEXT_ADDED_DAYS = 2;

	public static final boolean EXPORT_PATH_ID;
	public static final boolean EXPORT_ORIGINAL_ID;
	public static final boolean EXPORT_DESCENT_ONLY;

	static {
		EXPORT_PATH_ID = false;
		EXPORT_ORIGINAL_ID = false;
		EXPORT_DESCENT_ONLY = false;
	}

	@SuppressWarnings("WeakerAccess")
	public static final boolean GOOD_ENOUGH_ACCEPTED;

	static {
		GOOD_ENOUGH_ACCEPTED = false;
		// GOOD_ENOUGH_ACCEPTED = true; // DEBUG
	}

	public static final boolean IS_CI;

	static {
		final String isCI = System.getenv("CI");
		IS_CI = isCI != null && !isCI.isEmpty();
		// IS_CI = true; // DEBUG
	}

	private static final Integer THREAD_POOL_SIZE;

	static {
		final String envMTThreadPoolSize = System.getenv("MT_THREAD_POOL_SIZE");
		if (envMTThreadPoolSize != null
				&& !envMTThreadPoolSize.isEmpty()
				&& CharUtils.isDigitsOnly(envMTThreadPoolSize)) {
			THREAD_POOL_SIZE = Integer.parseInt(envMTThreadPoolSize);
		} else if (IS_CI) {
			THREAD_POOL_SIZE = 1;
		} else {
			THREAD_POOL_SIZE = 4;
		}
		MTLog.log("Thread pool size: %d.", THREAD_POOL_SIZE);
	}

	private static final Integer OVERRIDE_DATE;

	static {
		OVERRIDE_DATE = null; // yyyyMMdd
	}

	private static final boolean TOMORROW;

	static {
		TOMORROW = false;
	}

	public static void main(@NotNull String[] args) {
		throw new MTLog.Fatal("NEED TO IMPLEMENT MAIN METHOD"); // UNTIL WE HAVE JSON CONFIG FILE
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	public void start(@NotNull String[] args) {
		MTLog.log("Generating %s data...", getAgencyLabel());
		if (defaultExcludeEnabled()) {
			this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		}
		if (excludingAll()) {
			MGenerator.dumpFiles(this, null, args[0], args[1], args[2], true);
			return;
		}
		long start = System.currentTimeMillis();
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this, false, false);
		gtfs.cleanupExcludedData();
		gtfs.generateTripStops();
		if (args.length >= 4 && Boolean.parseBoolean(args[3])) {
			gtfs.generateStopTimesFromFrequencies(this);
		}
		gtfs.splitByRouteId(this);
		gtfs.clearRawData();
		MSpec mSpec = MGenerator.generateMSpec(gtfs, this);
		if (Constants.SKIP_FILE_DUMP) {
			return; // DEBUG
		}
		MGenerator.dumpFiles(this, mSpec, args[0], args[1], args[2]);
		MTLog.log("Generating %s data... DONE in %s.", getAgencyLabel(), Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@NotNull
	private String getAgencyLabel() {
		return getAgencyName() + " " + getAgencyType();
	}

	@NotNull
	public String getAgencyName() {
		return "Agency Name";
	}

	@NotNull
	private String getAgencyType() {
		final int type = getAgencyRouteType();
		if (type == MAgency.ROUTE_TYPE_LIGHT_RAIL) {
			return "light rail";
		} else if (type == MAgency.ROUTE_TYPE_SUBWAY) {
			return "subway";
		} else if (type == MAgency.ROUTE_TYPE_TRAIN) {
			return "train";
		} else if (type == MAgency.ROUTE_TYPE_BUS) {
			return "bus";
		} else if (type == MAgency.ROUTE_TYPE_FERRY) {
			return "ferry";
		}
		MTLog.log("Unexpected route type '%s'!", type);
		return "type";
	}

	@Override
	public boolean defaultExcludeEnabled() {
		return false; // OPT-IN feature
	}

	@Override
	public boolean excludingAll() {
		if (defaultExcludeEnabled()) {
			return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
		}
		throw new MTLog.Fatal("NEED TO IMPLEMENT EXCLUDE ALL");
	}

	@NotNull
	@Override
	public String getAgencyColor() {
		throw new MTLog.Fatal("AGENCY COLOR NOT PROVIDED");
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		throw new MTLog.Fatal("AGENCY ROUTE TYPE NOT PROVIDED");
	}

	@Override
	public boolean excludeAgencyNullable(@Nullable GAgency gAgency) {
		if (gAgency == null) {
			return EXCLUDE;
		}
		return excludeAgency(gAgency);
	}

	@Override
	public boolean excludeAgency(@NotNull GAgency gAgency) {
		return KEEP;
	}

	@NotNull
	@Override
	public String cleanServiceId(@NotNull String serviceId) {
		return serviceId;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		try {
			//noinspection deprecation
			return Long.parseLong(gRoute.getRouteId());
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while extracting route ID from %s!", gRoute);
		}
	}

	@Nullable
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		if (org.mtransit.commons.StringUtils.isEmpty(gRoute.getRouteShortName())) {
			throw new MTLog.Fatal("No default route short name for %s!", gRoute);
		}
		return gRoute.getRouteShortName();
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		return cleanRouteLongName(gRoute.getRouteLongNameOrDefault());
	}

	@NotNull
	@Override
	public String cleanRouteLongName(@NotNull String routeLongName) {
		if (org.mtransit.commons.StringUtils.isEmpty(routeLongName)) {
			throw new MTLog.Fatal("No default route long name for %s!", routeLongName);
		}
		return org.mtransit.commons.CleanUtils.cleanLabel(routeLongName);
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		if (gRoute.getRouteColor() == null || gRoute.getRouteColor().isEmpty()) {
			return null; // use agency color
		}
		//noinspection ConstantConditions
		if (getAgencyColor() != null && getAgencyColor().equalsIgnoreCase(gRoute.getRouteColor())) {
			return null;
		}
		return ColorUtils.darkenIfTooLight(gRoute.getRouteColor());
	}

	@Override
	public boolean excludeRouteNullable(@Nullable GRoute gRoute) {
		if (gRoute == null) {
			return EXCLUDE;
		}
		return excludeRoute(gRoute);
	}

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection ConstantConditions
		if (getAgencyRouteType() == null) {
			throw new MTLog.Fatal("ERROR: unspecified agency route type '%s'!", getAgencyRouteType());
		}
		if (GRouteType.isUnknown(gRoute.getRouteType())) {
			throw new MTLog.Fatal("ERROR: unexpected route type '%s'!", gRoute.getRouteType());
		}
		if (!GRouteType.isSameType(getAgencyRouteType(), gRoute.getRouteType())) {
			MTLog.logDebug("Route excluded because of different type: %s != %s", getAgencyRouteType(), gRoute.toStringPlus());
			return EXCLUDE;
		}
		return KEEP;
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		ArrayList<MTrip> mTrips = new ArrayList<>();
		mTrips.add(new MTrip(mRoute.getId()));
		return mTrips;
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute,
												 @NotNull GTrip gTrip,
												 @NotNull GTripStop gTripStop,
												 @NotNull ArrayList<MTrip> splitTrips,
												 @NotNull GSpec routeGTFS) {
		return new Pair<>(
				new Long[]{splitTrips.get(0).getId()},
				new Integer[]{gTripStop.getStopSequence()}
		);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (directionFinderEnabled()) {
			mTrip.setHeadsignString(
					cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
					gTrip.getDirectionIdOrDefault()
			);
			return;
		}
		if (gTrip.getDirectionId() == null || gTrip.getDirectionId() < 0 || gTrip.getDirectionId() > 1) {
			throw new MTLog.Fatal("Default agency implementation required 'direction_id' field in 'trips.txt'!");
		}
		try {
			mTrip.setHeadsignString(
					cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
					gTrip.getDirectionIdOrDefault()
			);
		} catch (NumberFormatException nfe) {
			throw new MTLog.Fatal(nfe, "Default agency implementation not possible!");
		}
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		return tripHeadsign;
	}

	@Override
	public boolean directionSplitterEnabled() {
		return false; // OPT-IN feature // WIP
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return directionSplitterEnabled();
	}

	@Deprecated
	@NotNull
	@Override
	public String cleanDirectionHeadsign(@NotNull String directionHeadSign) {
		return cleanDirectionHeadsign(false, directionHeadSign);
	}

	@NotNull
	@Override
	public String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign) {
		return cleanTripHeadsign(directionHeadSign);
	}

	@Override
	public boolean directionFinderEnabled() {
		return false; // OPT-IN feature
	}

	@Override
	public boolean directionFinderEnabled(long routeId, @NotNull GRoute gRoute) {
		return directionFinderEnabled();
	}

	@Override
	public boolean directionHeadSignsDescriptive(@NotNull Map<Integer, String> directionHeadSigns) {
		return directionHeadSignsDescriptiveS(directionHeadSigns);
	}

	@Nullable
	@Override
	public String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		return null; // optimize direction finder by selecting based on head-sign only
	}

	static boolean directionHeadSignsDescriptiveS(@NotNull Map<Integer, String> directionHeadSigns) {
		if (directionHeadSigns.isEmpty()) {
			return true; // nothing is not not descriptive
		}
		Set<String> distinctHeadSigns = new HashSet<>();
		for (String directionHeadSign : directionHeadSigns.values()) {
			if (!directionHeadSignDescriptiveS(directionHeadSign)) {
				return false;
			}
			distinctHeadSigns.add(directionHeadSign);
		}
		return distinctHeadSigns.size() == directionHeadSigns.size(); // must have the same number of distinct items
	}

	@Override
	public boolean directionHeadSignDescriptive(@NotNull String directionHeadSign) {
		return directionHeadSignDescriptiveS(directionHeadSign);
	}

	@SuppressWarnings("WeakerAccess")
	static boolean directionHeadSignDescriptiveS(@NotNull String directionHeadSign) {
		return !org.mtransit.commons.StringUtils.isBlank(directionHeadSign); // empty/blank head-sign is NOT descriptive
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	@Override
	public int getDirectionType() {
		return -1; // no preferred direction type
	}

	@NotNull
	@Override
	public List<Integer> getDirectionTypes() {
		//noinspection deprecation
		final int deprecatedDirectionType = getDirectionType();
		if (deprecatedDirectionType != -1
				&& deprecatedDirectionType != MTrip.HEADSIGN_TYPE_STRING) {
			return Arrays.asList(
					deprecatedDirectionType,
					MTrip.HEADSIGN_TYPE_STRING
			);
		}
		return Collections.singletonList(
				MTrip.HEADSIGN_TYPE_STRING // default = string
		);
	}

	@Nullable
	@Override
	public MDirectionType convertDirection(@Nullable String headSign) {
		if (headSign != null) {
			if (getDirectionTypes().contains(MTrip.HEADSIGN_TYPE_DIRECTION)) {
				final String tripHeadsignLC = headSign.toLowerCase(Locale.ENGLISH);
				switch (tripHeadsignLC) {
				case "eastbound":
				case "east":
				case "est":
				case "eb":
				case "e":
				case "eb / east":
					return MDirectionType.EAST;
				case "westbound":
				case "west":
				case "ouest":
				case "wb":
				case "w":
				case "o":
				case "wb / west":
					return MDirectionType.WEST;
				case "northbound":
				case "north":
				case "nord":
				case "nb":
				case "n":
				case "nb / north":
					return MDirectionType.NORTH;
				case "southbound":
				case "south":
				case "sud":
				case "sb":
				case "s":
				case "sb / south":
					return MDirectionType.SOUTH;
				}
				if (getDirectionTypes().size() == 1) { // only DIRECTION!
					throw new MTLog.Fatal("Unexpected direction for '%s'!", headSign);
				}
			}
		}
		return null; // no direction conversion by default
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
		if (directionFinderEnabled()) {
			throw new MTLog.Fatal("Unexpected trips to merge: %s & %s!", mTrip, mTripToMerge);
		}
		return mTrip.mergeHeadsignValue(mTripToMerge);
	}

	@Override
	public boolean excludeStopTime(@NotNull GStopTime gStopTime) {
		return GPickupType.NO_PICKUP.getId() == gStopTime.getPickupType() //
				&& GDropOffType.NO_DROP_OFF.getId() == gStopTime.getDropOffType();
	}

	@Override
	public boolean excludeTripNullable(@Nullable GTrip gTrip) {
		if (gTrip == null) {
			return EXCLUDE;
		}
		return excludeTrip(gTrip);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (defaultExcludeEnabled()) {
			if (this.serviceIdInts != null) {
				return excludeUselessTripInt(gTrip, this.serviceIdInts);
			}
		}
		return KEEP;
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDate) {
		if (defaultExcludeEnabled()) {
			if (this.serviceIdInts != null) {
				return excludeUselessCalendarDateInt(gCalendarDate, this.serviceIdInts);
			}
		}
		return false;
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (defaultExcludeEnabled()) {
			if (this.serviceIdInts != null) {
				return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
			}
		}
		return false;
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		return org.mtransit.commons.CleanUtils.cleanLabel(gStopName);
	}

	@Nullable
	@Override
	public String cleanStopHeadsign(@Nullable String stopHeadsign) {
		if (stopHeadsign == null) {
			return null;
		}
		return cleanTripHeadsign(stopHeadsign);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		return gStop.getStopCode();
	}

	@Nullable
	@Override
	public String getStopOriginalId(@NotNull GStop gStop) {
		return null; // only if not stop code or stop ID
	}

	@Override
	public int getStopId(@NotNull GStop gStop) {
		try {
			//noinspection deprecation
			return Integer.parseInt(gStop.getStopId());
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while extracting stop ID from %s!", gStop);
		}
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		return gStopId;
	}

	@Override
	public int compareEarly(long routeId,
							@NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2,
							@NotNull MTripStop ts1, @NotNull MTripStop ts2,
							@NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		return 0; // nothing
	}

	@Override
	public int compare(long routeId,
					   @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2,
					   @NotNull MTripStop ts1, @NotNull MTripStop ts2,
					   @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		return 0; // nothing
	}

	@Override
	public boolean excludeStopNullable(@Nullable GStop gStop) {
		if (gStop == null) {
			return EXCLUDE;
		}
		return excludeStop(gStop);
	}

	@Override
	public boolean excludeStop(@NotNull GStop gStop) {
		return KEEP;
	}

	@SuppressWarnings("unused")
	public boolean isGoodEnoughAccepted() {
		return GOOD_ENOUGH_ACCEPTED;
	}

	@Override
	public int getThreadPoolSize() {
		return THREAD_POOL_SIZE;
	}

	@Deprecated
	@NotNull
	@Override
	public Pair<Integer, Integer> getTimes(@NotNull GStopTime gStopTime,
										   @NotNull List<GStopTime> tripStopTimes,
										   @Nullable SimpleDateFormat gDateFormat,
										   @NotNull SimpleDateFormat mDateFormat) {
		return getTimes(gStopTime, tripStopTimes, mDateFormat);
	}

	@NotNull
	@Override
	public Pair<Integer, Integer> getTimes(@NotNull GStopTime gStopTime,
										   @NotNull List<GStopTime> tripStopTimes,
										   @NotNull SimpleDateFormat mDateFormat) {
		if (!gStopTime.hasArrivalTime() || !gStopTime.hasDepartureTime()) {
			return extractTimes(gStopTime, tripStopTimes, mDateFormat);
		} else {
			return new Pair<>(
					TimeUtils.cleanExtraSeconds(gStopTime.getArrivalTime()),
					TimeUtils.cleanExtraSeconds(gStopTime.getDepartureTime()));
		}
	}

	@NotNull
	private static Pair<Integer, Integer> extractTimes(GStopTime gStopTime,
													   @NotNull List<GStopTime> tripStopTimes,
													   SimpleDateFormat mDateFormat) {
		try {
			Pair<Long, Long> timesInMs = extractTimeInMs(gStopTime, tripStopTimes);
			long arrivalTimeInMs = timesInMs.first;
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(arrivalTimeInMs);
			int arrivalTime = Integer.parseInt(mDateFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				arrivalTime += 24_00_00;
			}
			long departureTimeInMs = timesInMs.second;
			calendar.setTimeInMillis(departureTimeInMs);
			int departureTime = Integer.parseInt(mDateFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				departureTime += 24_00_00;
			}
			return new Pair<>(
					TimeUtils.cleanExtraSeconds(arrivalTime),
					TimeUtils.cleanExtraSeconds(departureTime)
			);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while interpolating times for %s!", gStopTime);
		}
	}

	@NotNull
	public static Pair<Long, Long> extractTimeInMs(@NotNull GStopTime gStopTime,
												   @NotNull List<GStopTime> tripStopTimes) {
		int previousArrivalTime = -1;
		int previousArrivalTimeStopSequence = -1;
		int previousDepartureTime = -1;
		int previousDepartureTimeStopSequence = -1;
		int nextArrivalTime = -1;
		int nextArrivalTimeStopSequence = -1;
		int nextDepartureTime = -1;
		int nextDepartureTimeStopSequence = -1;
		for (GStopTime aStopTime : tripStopTimes) {
			if (gStopTime.getTripIdInt() != aStopTime.getTripIdInt()) {
				continue;
			}
			if (aStopTime.getStopSequence() == gStopTime.getStopSequence()
					&& aStopTime.getStopIdInt() == gStopTime.getStopIdInt()
					&& aStopTime.hasArrivalTime()
					&& aStopTime.hasDepartureTime()) {
				return new Pair<>( // re-use provided stop times from another trip #frequencies
						aStopTime.getArrivalTimeMs(),
						aStopTime.getDepartureTimeMs()
				);
			}
			if (aStopTime.getStopSequence() < gStopTime.getStopSequence()) {
				if (aStopTime.hasDepartureTime()) {
					if (previousDepartureTime < 0
							|| previousDepartureTimeStopSequence < 0
							|| previousDepartureTimeStopSequence < aStopTime.getStopSequence()) {
						previousDepartureTime = aStopTime.getDepartureTime();
						previousDepartureTimeStopSequence = aStopTime.getStopSequence();
					}
				}
				if (aStopTime.hasArrivalTime()) {
					if (previousArrivalTime < 0
							|| previousArrivalTimeStopSequence < 0
							|| previousArrivalTimeStopSequence < aStopTime.getStopSequence()) {
						previousArrivalTime = aStopTime.getArrivalTime();
						previousArrivalTimeStopSequence = aStopTime.getStopSequence();
					}
				}
			} else if (aStopTime.getStopSequence() > gStopTime.getStopSequence()) {
				if (aStopTime.hasDepartureTime()) {
					if (nextDepartureTime < 0
							|| nextDepartureTimeStopSequence < 0
							|| nextDepartureTimeStopSequence > aStopTime.getStopSequence()) {
						nextDepartureTime = aStopTime.getDepartureTime();
						nextDepartureTimeStopSequence = aStopTime.getStopSequence();
					}
				}
				if (aStopTime.hasArrivalTime()) {
					if (nextArrivalTime < 0
							|| nextArrivalTimeStopSequence < 0
							|| nextArrivalTimeStopSequence > aStopTime.getStopSequence()) {
						nextArrivalTime = aStopTime.getArrivalTime();
						nextArrivalTimeStopSequence = aStopTime.getStopSequence();
					}
				}
			}
		}
		long previousArrivalTimeInMs = GTime.toMs(previousArrivalTime);
		long previousDepartureTimeInMs = GTime.toMs(previousDepartureTime);
		long nextArrivalTimeInMs = GTime.toMs(nextArrivalTime);
		long nextDepartureTimeInMs = GTime.toMs(nextDepartureTime);
		long arrivalTimeDiffInMs = nextArrivalTimeInMs - previousArrivalTimeInMs;
		long departureTimeDiffInMs = nextDepartureTimeInMs - previousDepartureTimeInMs;
		int arrivalNbStop = nextArrivalTimeStopSequence - previousArrivalTimeStopSequence;
		int departureNbStop = nextDepartureTimeStopSequence - previousDepartureTimeStopSequence;
		long arrivalTimeBetweenStopInMs = arrivalTimeDiffInMs / arrivalNbStop;
		long departureTimeBetweenStopInMs = departureTimeDiffInMs / departureNbStop;
		long arrivalTime = previousArrivalTimeInMs + (arrivalTimeBetweenStopInMs * (gStopTime.getStopSequence() - previousArrivalTimeStopSequence));
		long departureTime = previousDepartureTimeInMs + (departureTimeBetweenStopInMs * (gStopTime.getStopSequence() - previousDepartureTimeStopSequence));
		return new Pair<>(arrivalTime, departureTime);
	}

	@Override
	public int getStartTime(@NotNull GFrequency gFrequency) {
		return gFrequency.getStartTime();  // GTFS standard
	}

	@Override
	public int getEndTime(@NotNull GFrequency gFrequency) {
		return gFrequency.getEndTime();  // GTFS standard
	}

	protected static class Period {
		// TODO @Nullable
		Integer todayStringInt = null;
		// TODO @Nullable
		Integer startDate = null;
		// TODO @Nullable
		Integer endDate = null;

		@Override
		public String toString() {
			return Period.class.getSimpleName() + "{" //
					+ "todayStringInt: " + todayStringInt + ", " //
					+ "startDate: " + startDate + ", " //
					+ "endDate: " + endDate + ", " //
					+ "}";
		}
	}

	@Deprecated
	@NotNull
	public static HashSet<String> extractUsefulServiceIds(@NotNull String[] args, @NotNull DefaultAgencyTools agencyTools) {
		return extractUsefulServiceIds(args, agencyTools, false);
	}

	@Deprecated
	@NotNull
	public static HashSet<Integer> extractUsefulServiceIdInts(@NotNull String[] args, @NotNull DefaultAgencyTools agencyTools) {
		return extractUsefulServiceIdInts(args, agencyTools, false);
	}

	@Deprecated
	@NotNull
	public static HashSet<String> extractUsefulServiceIds(@NotNull String[] args, @NotNull DefaultAgencyTools agencyTools, boolean agencyFilter) {
		HashSet<Integer> serviceIds = extractUsefulServiceIdInts(args, agencyTools, agencyFilter);

		HashSet<String> serviceIdsS = new HashSet<>();
		for (Integer serviceId : serviceIds) {
			serviceIdsS.add(serviceId.toString());
		}
		return serviceIdsS;
	}

	@Nullable
	private static Period usefulPeriod = null;

	@SuppressWarnings("WeakerAccess")
	@NotNull
	public static HashSet<Integer> extractUsefulServiceIdInts(@NotNull String[] args, @NotNull DefaultAgencyTools agencyTools, boolean agencyFilter) {
		MTLog.log("Extracting useful service IDs...");
		usefulPeriod = new Period();
		SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
		boolean isCurrent = "current_".equalsIgnoreCase(args[2]);
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		boolean isCurrentOrNext = isCurrent || isNext;
		Calendar c = Calendar.getInstance();
		if (!isCurrentOrNext && TOMORROW) {
			c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		}
		usefulPeriod.todayStringInt = Integer.valueOf(DATE_FORMAT.format(c.getTime()));
		if (!isCurrentOrNext && OVERRIDE_DATE != null) {
			usefulPeriod.todayStringInt = OVERRIDE_DATE;
		}
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		List<GCalendar> gCalendars = gtfs.getAllCalendars();
		List<GCalendarDate> gCalendarDates = gtfs.getAllCalendarDates();
		printMinMaxDate(gCalendars, gCalendarDates);
		boolean hasCurrent = false;
		if (gCalendars.size() > 0) {
			parseCalendars(gCalendars, DATE_FORMAT, c, usefulPeriod, isCurrentOrNext); // CURRENT OR NEXT
		} else if (gCalendarDates.size() > 0) {
			parseCalendarDates(gCalendarDates, DATE_FORMAT, c, usefulPeriod, isCurrentOrNext); // CURRENT OR NEXT
		} else {
			throw new MTLog.Fatal("NO schedule available for %s! (1)", usefulPeriod.todayStringInt);
		}
		if (!isNext //
				&& (usefulPeriod.startDate == null || usefulPeriod.endDate == null)) {
			if (isCurrent) {
				MTLog.log("No CURRENT schedules for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt,
						usefulPeriod.startDate, usefulPeriod.endDate);
				System.exit(0); // keeping current schedule
				return null;
			}
			//noinspection ConstantConditions
			throw new MTLog.Fatal("NO schedule available for %s! (start:%s|end:%s) (isCurrent:%s|isNext:%s)", usefulPeriod.todayStringInt,
					usefulPeriod.startDate, usefulPeriod.endDate, isCurrent, isNext);
		}
		if (usefulPeriod.todayStringInt != null && usefulPeriod.startDate != null && usefulPeriod.endDate != null) {
			hasCurrent = true;
		}
		MTLog.log("Generated on %s | Schedules from %s to %s.", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
		if (isNext) {
			if (hasCurrent //
					&& !diffLowerThan(DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate, MAX_NEXT_LOOKUP_IN_DAYS)) {
				MTLog.log("Skipping NEXT schedules... (%d days from %s to %s)", //
						TimeUnit.MILLISECONDS.toDays(diffInMs(DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate)), //
						usefulPeriod.todayStringInt, //
						usefulPeriod.endDate);
				usefulPeriod.todayStringInt = null; // reset
				usefulPeriod.startDate = null; // reset
				usefulPeriod.endDate = null; // reset
				//noinspection UnusedAssignment // FIXME
				gtfs = null;
				return new HashSet<>(); // non-null = service IDs
			}
			MTLog.log("Looking for NEXT schedules...");
			if (hasCurrent) {
				usefulPeriod.todayStringInt = incDateDays(DATE_FORMAT, c, usefulPeriod.endDate, 1); // start from next to current last date
			}
			usefulPeriod.startDate = null; // reset
			usefulPeriod.endDate = null; // reset
			if (gCalendars.size() > 0) {
				parseCalendars(gCalendars, DATE_FORMAT, c, usefulPeriod, false); // NEXT
			} else if (gCalendarDates.size() > 0) {
				parseCalendarDates(gCalendarDates, DATE_FORMAT, c, usefulPeriod, false); // NEXT
			}
			if (usefulPeriod.startDate == null || usefulPeriod.endDate == null) {
				MTLog.log("NO NEXT schedule available for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt, usefulPeriod.startDate,
						usefulPeriod.endDate);
				usefulPeriod.todayStringInt = null; // reset
				usefulPeriod.startDate = null; // reset
				usefulPeriod.endDate = null; // reset
				//noinspection UnusedAssignment // FIXME
				gtfs = null;
				return new HashSet<>(); // non-null = service IDs
			}
			MTLog.log("Generated on %s | NEXT Schedules from %s to %s.", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
		}
		HashSet<Integer> serviceIds = getPeriodServiceIds(usefulPeriod.startDate, usefulPeriod.endDate, gCalendars, gCalendarDates);
		improveUsefulPeriod(usefulPeriod, DATE_FORMAT, c, gCalendars, gCalendarDates);
		MTLog.log("Extracting useful service IDs... DONE");
		//noinspection UnusedAssignment // FIXME
		gtfs = null;
		return serviceIds;
	}

	private static void improveUsefulPeriod(@NotNull Period usefulPeriod, SimpleDateFormat DATE_FORMAT, Calendar c, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		if (gCalendars != null && gCalendars.size() > 0 //
				&& gCalendarDates != null && gCalendarDates.size() > 0) {
			boolean newDateFound;
			do {
				newDateFound = false;
				int minNewStartDate = incDateDays(DATE_FORMAT, c, usefulPeriod.startDate, -1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isBefore(usefulPeriod.startDate)) {
						if (gCalendarDate.isBetween(minNewStartDate, usefulPeriod.startDate)) {
							MTLog.log("new useful period start date '%s' because it's close to previous useful period start date '%s'.",
									gCalendarDate.getDate(), usefulPeriod.startDate);
							usefulPeriod.startDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
			do {
				newDateFound = false;
				int minNewEndDate = incDateDays(DATE_FORMAT, c, usefulPeriod.endDate, +1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isAfter(usefulPeriod.endDate)) {
						if (gCalendarDate.isBetween(usefulPeriod.endDate, minNewEndDate)) {
							MTLog.log("new useful end date '%s' because it's close to previous useful period end data '%s'.",
									gCalendarDate.getDate(), usefulPeriod.endDate);
							usefulPeriod.endDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
		}
	}

	private static void parseCalendarDates(List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean lookForward) {
		HashSet<Integer> todayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, p,
				lookForward ? 1 : 0, // min-size backward
				lookForward ? 0 : 1, // min-size forward
				1);
		if (todayServiceIds.isEmpty()) {
			MTLog.log("NO schedule available for %s in calendar dates.", p.todayStringInt);
			return;
		}
		refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
		while (true) {
			MTLog.log("Schedules from %s to %s... ", p.startDate, p.endDate);
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(p.startDate, p.endDate)) {
					if (!gCalendarDate.isServiceIdInts(todayServiceIds)) {
						MTLog.log("> new service ID from calendar date active on %s between %s and %s: '%s'", gCalendarDate.getDate(), p.startDate,
								p.endDate, gCalendarDate.getServiceIdInt());
						todayServiceIds.add(gCalendarDate.getServiceIdInt());
					}
				}
			}
			boolean newDates = refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
			if (newDates) {
				MTLog.log("> new start date '%s' & end date '%s' from calendar date active during service ID(s).", p.startDate, p.endDate);
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			HashSet<Integer> nextDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pNext, 0, 0, 0);
			refreshStartEndDatesFromCalendarDates(pNext, nextDayServiceIds, gCalendarDates);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("> new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			HashSet<Integer> previousDayServiceIds = findTodayServiceIds(gCalendarDates, DATE_FORMAT, c, pPrevious, 0, 0, 0);
			refreshStartEndDatesFromCalendarDates(pPrevious, previousDayServiceIds, gCalendarDates);
			if (lookForward // NOT next schedule, only current schedule can look behind
					&& pPrevious.startDate != null && pPrevious.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.startDate = pPrevious.startDate;
				MTLog.log("> new start date '%s' because previous day has own service ID(s)", p.endDate);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS)) {
				long nextPeriodCoverageInMs = pNext.startDate == null || pNext.endDate == null ? 0L :
						diffInMs(DATE_FORMAT, c,
								pNext.startDate, // morning
								pNext.endDate + 1 // midnight ≈ tomorrow
						);
				long previousPeriodCoverageInMs = pPrevious.startDate == null || pPrevious.endDate == null ? 0L :
						diffInMs(DATE_FORMAT, c,
								pPrevious.startDate, // morning
								pPrevious.endDate + 1 // midnight ≈ tomorrow
						);
				if (lookForward // NOT next schedule, only current schedule can look behind
						&& previousPeriodCoverageInMs > 0L
						&& (nextPeriodCoverageInMs <= 0L || previousPeriodCoverageInMs < nextPeriodCoverageInMs)) {
					p.startDate = incDateDays(DATE_FORMAT, c, p.startDate, -1); // start--
					MTLog.log("new start date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.startDate);
				} else {
					p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
					MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				}
				continue;
			}
			break;
		}
	}

	private static boolean refreshStartEndDatesFromCalendarDates(Period p, HashSet<Integer> todayServiceIds, List<GCalendarDate> gCalendarDates) {
		boolean newDates = false;
		for (GCalendarDate gCalendarDate : gCalendarDates) {
			if (gCalendarDate.isServiceIdInts(todayServiceIds)) {
				if (p.startDate == null || gCalendarDate.isBefore(p.startDate)) {
					p.startDate = gCalendarDate.getDate();
					newDates = true;
				}
				if (p.endDate == null || gCalendarDate.isAfter(p.endDate)) {
					p.endDate = gCalendarDate.getDate();
					newDates = true;
				}
			}
		}
		return newDates;
	}

	@NotNull
	private static HashSet<Integer> findTodayServiceIds(@NotNull List<GCalendarDate> gCalendarDates,
														SimpleDateFormat DATE_FORMAT,
														Calendar c,
														Period p,
														int minSizeBackward,
														int minSizeForward,
														int incDays) {
		HashSet<Integer> todayServiceIds = new HashSet<>();
		final int initialTodayStringInt = p.todayStringInt;
		while (true) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isDate(p.todayStringInt)) {
					if (!gCalendarDate.isServiceIdInts(todayServiceIds)) {
						todayServiceIds.add(gCalendarDate.getServiceIdInt());
					}
				}
			}
			if (todayServiceIds.size() < minSizeForward //
					&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MAX_LOOK_FORWARD_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, incDays);
				MTLog.log("new today because not enough service today: %s (initial today: %s, min: %s)", p.todayStringInt, initialTodayStringInt,
						minSizeForward);
				continue;
			} else if (todayServiceIds.size() < minSizeBackward //
					&& diffLowerThan(DATE_FORMAT, c, p.todayStringInt, initialTodayStringInt, MAX_LOOK_BACKWARD_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, -incDays);
				MTLog.log("new today because not enough service today: %s (initial today: %s, min: %s)", p.todayStringInt, initialTodayStringInt,
						minSizeBackward);
				continue;
			}
			break;
		}
		return todayServiceIds;
	}

	static void parseCalendars(List<GCalendar> gCalendars, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean keepToday) {
		final int initialTodayStringInt = p.todayStringInt;
		boolean newDates;
		while (true) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.containsDate(p.todayStringInt)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log("new start date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						//noinspection UnusedAssignment // FIXME
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log("new end date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						//noinspection UnusedAssignment // FIXME
						newDates = true;
					}
				}
			}
			if (!keepToday //
					&& (p.startDate == null || p.endDate == null) //
					&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MAX_LOOK_FORWARD_IN_DAYS)) {
				p.todayStringInt = incDateDays(DATE_FORMAT, c, p.todayStringInt, 1);
				MTLog.log("new today because no service today: %s (initial today: %s)", p.todayStringInt, initialTodayStringInt);
				//noinspection UnnecessaryContinue
				continue;
			} else {
				break;
			}
		}
		if (p.startDate == null || p.endDate == null) {
			MTLog.log("NO schedule available for %s in calendars. (start:%s|end:%s)", p.todayStringInt, p.startDate, p.endDate);
			return;
		}
		while (true) {
			MTLog.log("Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isOverlapping(p.startDate, p.endDate)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.log("new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.log("new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			findDayServiceIdsPeriod(gCalendars, pNext);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			findDayServiceIdsPeriod(gCalendars, pPrevious);
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS)) {
				long nextPeriodCoverageInMs = pNext.startDate == null || pNext.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pNext.startDate, pNext.endDate);
				long previousPeriodCoverageInMs = pPrevious.startDate == null || pPrevious.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate);
				if (keepToday // NOT next schedule, only current schedule can look behind
						&& previousPeriodCoverageInMs > 0L && previousPeriodCoverageInMs < nextPeriodCoverageInMs) {
					p.startDate = incDateDays(DATE_FORMAT, c, p.startDate, -1); // start--
					MTLog.log("new start date because coverage lower than %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.startDate);
				} else {
					p.endDate = incDateDays(DATE_FORMAT, c, p.endDate, 1); // end++
					MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				}
				continue;
			}
			break;
		}
	}

	static void findDayServiceIdsPeriod(List<GCalendar> gCalendars, Period p) {
		boolean newDates;
		while (true) {
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.containsDate(p.todayStringInt)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.logDebug("findDayServiceIdsPeriod() > new start date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.logDebug("findDayServiceIdsPeriod() > new end date from calendar active on %s: %s (was: %s)", p.todayStringInt, gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
		if (p.startDate == null || p.endDate == null) {
			MTLog.logDebug("findDayServiceIdsPeriod() > NO schedule available for %s in calendars. (start:%s|end:%s)", p.todayStringInt, p.startDate, p.endDate);
			MTLog.logDebugMethodEnd("findDayServiceIdsPeriod");
			return;
		}
		while (true) {
			MTLog.logDebug("findDayServiceIdsPeriod() > Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isOverlapping(p.startDate, p.endDate)) {
					if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
						MTLog.logDebug("findDayServiceIdsPeriod() > new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getStartDate(), p.startDate);
						p.startDate = gCalendar.getStartDate();
						newDates = true;
					}
					if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
						MTLog.logDebug("findDayServiceIdsPeriod() > new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate,
								gCalendar.getEndDate(), p.endDate);
						p.endDate = gCalendar.getEndDate();
						newDates = true;
					}
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
		MTLog.logDebugMethodEnd("findDayServiceIdsPeriod");
	}

	@NotNull
	private static HashSet<Integer> getPeriodServiceIds(Integer startDate, Integer endDate, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		HashSet<Integer> serviceIdInts = new HashSet<>();
		if (gCalendars != null) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isInside(startDate, endDate)) {
					if (!gCalendar.isServiceIdInts(serviceIdInts)) {
						//noinspection deprecation
						MTLog.log("new service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
						serviceIdInts.add(gCalendar.getServiceIdInt());
					}
				}
			}
		}
		if (gCalendarDates != null) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(startDate, endDate)) {
					if (!gCalendarDate.isServiceIdInts(serviceIdInts)) {
						if (gCalendarDate.getExceptionType() == GCalendarDatesExceptionType.SERVICE_REMOVED) {
							//noinspection deprecation
							MTLog.log("ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate,
									gCalendarDate.getServiceId());
							continue;
						}
						//noinspection deprecation
						MTLog.log("new service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
						serviceIdInts.add(gCalendarDate.getServiceIdInt());
					}
				}
			}
		}
		MTLog.log("Service IDs: %s.", GIDs.toStringPlus(serviceIdInts));
		return serviceIdInts;
	}

	private static void printMinMaxDate(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		Period p = new Period();
		if (gCalendars != null && gCalendars.size() > 0) {
			for (GCalendar gCalendar : gCalendars) {
				if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
					p.startDate = gCalendar.getStartDate();
				}
				if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
					p.endDate = gCalendar.getEndDate();
				}
			}
		}
		if (gCalendarDates != null && gCalendarDates.size() > 0) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (p.startDate == null || gCalendarDate.isBefore(p.startDate)) {
					p.startDate = gCalendarDate.getDate();
				}
				if (p.endDate == null || gCalendarDate.isAfter(p.endDate)) {
					p.endDate = gCalendarDate.getDate();
				}
			}
		}
		MTLog.log("Schedule available from %s to %s.", p.startDate, p.endDate);
	}

	private static int incDateDays(SimpleDateFormat dateFormat, Calendar calendar, int dateInt, int numberOfDays) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(dateInt)));
			calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
			return Integer.parseInt(dateFormat.format(calendar.getTime()));
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while increasing end date!");
		}
	}

	private static boolean diffLowerThan(SimpleDateFormat dateFormat, Calendar calendar, int startDateInt, int endDateInt, int diffInDays) {
		try {
			return diffInMs(dateFormat, calendar, startDateInt, endDateInt) < TimeUnit.DAYS.toMillis(diffInDays);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while checking date difference!");
		}
	}

	private static long diffInMs(SimpleDateFormat dateFormat, Calendar calendar, int startDateInt, int endDateInt) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(startDateInt)));
			long startDateInMs = calendar.getTimeInMillis();
			calendar.setTime(dateFormat.parse(String.valueOf(endDateInt)));
			long endDateInMs = calendar.getTimeInMillis();
			return endDateInMs - startDateInMs;
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while checking date difference!");
		}
	}

	@Deprecated
	protected static boolean excludeUselessCalendar(@NotNull GCalendar gCalendar, @Nullable HashSet<String> serviceIdsS) {
		HashSet<Integer> serviceIds = null;
		if (serviceIdsS != null) {
			serviceIds = new HashSet<>();
			for (String serviceIdS : serviceIdsS) {
				serviceIds.add(Integer.valueOf(serviceIdS));
			}
		}
		return excludeUselessCalendarInt(gCalendar, serviceIds);
	}

	@SuppressWarnings("WeakerAccess")
	protected static boolean excludeUselessCalendarInt(@NotNull GCalendar gCalendar, @Nullable HashSet<Integer> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gCalendar.isServiceIdInts(serviceIds);
			//noinspection RedundantIfStatement
			if (!knownServiceId) {
				return EXCLUDE;
			}
		}
		return KEEP;
	}

	@Deprecated
	protected static boolean excludeUselessCalendarDate(@NotNull GCalendarDate gCalendarDate, @Nullable HashSet<String> serviceIdsS) {
		HashSet<Integer> serviceIds = null;
		if (serviceIdsS != null) {
			serviceIds = new HashSet<>();
			for (String serviceIdS : serviceIdsS) {
				serviceIds.add(Integer.valueOf(serviceIdS));
			}
		}
		return excludeUselessCalendarDateInt(gCalendarDate, serviceIds);
	}

	@SuppressWarnings("WeakerAccess")
	protected static boolean excludeUselessCalendarDateInt(@NotNull GCalendarDate gCalendarDate, @Nullable HashSet<Integer> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gCalendarDate.isServiceIdInts(serviceIds);
			if (!knownServiceId) {
				return EXCLUDE;
			}
		}
		if (usefulPeriod != null) {
			if (gCalendarDate.getExceptionType() == GCalendarDatesExceptionType.SERVICE_ADDED) {
				if (gCalendarDate.isBefore(usefulPeriod.startDate) //
						|| gCalendarDate.isAfter(usefulPeriod.endDate)) {
					MTLog.log("Exclude calendar date \"%s\" because it's out of the useful period (start:%s|end:%s).", gCalendarDate,
							usefulPeriod.startDate, usefulPeriod.endDate);
					return EXCLUDE;
				}
			}
		}
		return KEEP;
	}

	@Deprecated
	protected static boolean excludeUselessTrip(@NotNull GTrip gTrip,
												@Nullable HashSet<String> serviceIdsS) {
		HashSet<Integer> serviceIds = null;
		if (serviceIdsS != null) {
			serviceIds = new HashSet<>();
			for (String serviceIdS : serviceIdsS) {
				serviceIds.add(Integer.valueOf(serviceIdS));
			}
		}
		return excludeUselessTripInt(gTrip, serviceIds);
	}

	@SuppressWarnings("WeakerAccess")
	protected static boolean excludeUselessTripInt(@NotNull GTrip gTrip,
												   @Nullable HashSet<Integer> serviceIds) {
		if (serviceIds != null) {
			boolean knownServiceId = gTrip.isServiceIdInts(serviceIds);
			//noinspection RedundantIfStatement
			if (!knownServiceId) {
				return EXCLUDE;
			}
		}
		return KEEP;
	}
}
