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
import org.mtransit.parser.gtfs.data.GFieldTypes;
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
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MRouteSNToIDConverter;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MTrip;

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

@SuppressWarnings({"RedundantSuppression", "unused", "WeakerAccess"})
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

	private static final long MAX_CALENDAR_DATE_COVERAGE_RATIO = 10;

	private static final int MIN_PREVIOUS_NEXT_ADDED_DAYS = 2;

	public static final boolean EXPORT_PATH_ID;
	@Deprecated
	public static final boolean EXPORT_ORIGINAL_ID = false;

	static {
		EXPORT_PATH_ID = false;
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
		if (args.length < 3) {
			throw new MTLog.Fatal("Invalid number(%d) of arguments! (%s)", args.length, Arrays.asList(args));
		}
		MTLog.log("Generating %s data...", getAgencyLabel());
		MTLog.logDebug("Args [%d]: %s.", args.length, Arrays.asList(args));
		if (defaultExcludeEnabled()) {
			this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		}
		if (excludingAll()) {
			MGenerator.dumpFiles(this, null, args[0], args[1], args[2], true);
			return;
		}
		long start = System.currentTimeMillis();
		GSpec gtfs = GReader.readGtfsZipFile(args[0], this, false, false);
		gtfs.cleanupStops();
		gtfs.cleanupExcludedData();
		gtfs.cleanupStopTimesPickupDropOffTypes(this);
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
		MGenerator.dumpFiles(this, mSpec, args[0], args[1], args[2], false);
		MTLog.log("Generating %s data... DONE in %s.", getAgencyLabel(), Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@NotNull
	private String getAgencyLabel() {
		return getAgencyName() + " " + getAgencyType();
	}

	@NotNull
	private final List<Locale> supportedLanguages = new ArrayList<>();

	@Override
	public void addSupportedLanguage(@Nullable String supportedLanguage) {
		if (supportedLanguage == null) {
			return;
		}
		final Locale supportedLocale = Locale.forLanguageTag(supportedLanguage);
		if (this.supportedLanguages.contains(supportedLocale)) {
			return;
		}
		this.supportedLanguages.add(supportedLocale);
	}

	@Nullable
	@Override
	public List<Locale> getSupportedLanguages() {
		if (!this.supportedLanguages.isEmpty()) {
			return this.supportedLanguages;
		}
		return null; // no-op
	}

	@Nullable
	public Locale getFirstLanguage() {
		final List<Locale> supportedLanguages = getSupportedLanguages();
		return supportedLanguages == null || supportedLanguages.size() < 1 ? null : supportedLanguages.get(0);
	}

	@NotNull
	public Locale getFirstLanguageNN() {
		final Locale firstLanguage = getFirstLanguage();
		if (firstLanguage == null) {
			throw new MTLog.Fatal("NEED TO PROVIDE SUPPORTED LANGUAGES");
		}
		return firstLanguage;
	}

	@Nullable
	private String agencyName = null;

	@Override
	public void setAgencyName(@Nullable String agencyName) {
		this.agencyName = agencyName;
	}

	@NotNull
	public String getAgencyName() {
		if (this.agencyName != null) {
			return this.agencyName;
		}
		MTLog.log("Agency name not provided!");
		return "Agency Name";
	}

	@NotNull
	private String getAgencyType() {
		final int type = getAgencyRouteType();
		if (GRouteType.isSameType(type, MAgency.ROUTE_TYPE_LIGHT_RAIL)) {
			return "light rail";
		} else if (GRouteType.isSameType(type, MAgency.ROUTE_TYPE_SUBWAY)) {
			return "subway";
		} else if (GRouteType.isSameType(type, MAgency.ROUTE_TYPE_TRAIN)) {
			return "train";
		} else if (GRouteType.isSameType(type, MAgency.ROUTE_TYPE_BUS)) {
			return "bus";
		} else if (GRouteType.isSameType(type, MAgency.ROUTE_TYPE_FERRY)) {
			return "ferry";
		}
		MTLog.log("getAgencyType() > Unexpected agency route type '%s'!", type);
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

	@Override
	public boolean defaultAgencyColorEnabled() {
		return false; // OPT-IN feature
	}

	@Nullable
	@Override
	public String fixColor(@Nullable String color) {
		if (color == null || color.trim().isEmpty()) {
			return null;
		}
		return ColorUtils.darkenIfTooLight(color);
	}

	@NotNull
	@Override
	public String getAgencyColor() {
		throw new MTLog.Fatal("AGENCY COLOR NOT PROVIDED");
	}

	@NotNull
	@Override
	public String getAgencyColor(@NotNull GAgency gAgency, @NotNull GSpec gSpec) {
		if (defaultAgencyColorEnabled()) {
			final String pickFromRoutesFixed = fixColor(
					MAgency.pickColorFromRoutes(gAgency, gSpec)
			);
			if (pickFromRoutesFixed != null) {
				return pickFromRoutesFixed;
			}
		}
		return getAgencyColor();
	}

	@Nullable
	@Override
	public String getAgencyId() {
		return null; // no filter
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		throw new MTLog.Fatal("AGENCY ROUTE TYPE NOT PROVIDED");
	}

	@NotNull
	@Override
	public Integer getOriginalAgencyRouteType() {
		return getAgencyRouteType();
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
		//noinspection deprecation
		if (getAgencyId() != null && gAgency.isDifferentAgency(getAgencyId())) {
			return EXCLUDE;
		}
		return KEEP;
	}

	@NotNull
	@Override
	public String cleanServiceId(@NotNull String serviceId) {
		return serviceId;
	}

	@NotNull
	@Override
	public String cleanRouteOriginalId(@NotNull String gRouteId) {
		return gRouteId;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		try {
			//noinspection deprecation
			final String routeIdS = useRouteShortNameForRouteId() ? cleanRouteShortName(getRouteShortName(gRoute)) : cleanRouteOriginalId(gRoute.getRouteId());
			if (defaultRouteIdEnabled()
					&& !CharUtils.isDigitsOnly(routeIdS)) {
				return MRouteSNToIDConverter.convert(
						routeIdS,
						this::convertRouteIdFromShortNameNotSupported,
						this::convertRouteIdNextChars,
						this::convertRouteIdPreviousChars
				);
			}
			return Long.parseLong(routeIdS);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while extracting route ID from %s!", gRoute);
		}
	}

	@Nullable
	@Override
	public Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName) {
		return null;
	}

	@Nullable
	@Override
	public Long convertRouteIdNextChars(@NotNull String nextChars) {
		return null;
	}

	@Nullable
	@Override
	public Long convertRouteIdPreviousChars(@NotNull String previousChars) {
		return null;
	}

	@Override
	public boolean defaultRouteIdEnabled() {
		return false; // OPT-IN feature
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return false; // OPT-IN feature
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		final String routeShortName = gRoute.getRouteShortName();
		if (org.mtransit.commons.StringUtils.isEmpty(routeShortName)) {
			return provideMissingRouteShortName(gRoute);
		}
		return cleanRouteShortName(routeShortName);
	}

	@NotNull
	@Override
	public String cleanRouteShortName(@NotNull String routeShortName) {
		if (org.mtransit.commons.StringUtils.isEmpty(routeShortName)) {
			throw new MTLog.Fatal("No default route short name for %s!", routeShortName);
		}
		return routeShortName.trim();
	}

	@NotNull
	@Override
	public String provideMissingRouteShortName(@NotNull GRoute gRoute) {
		throw new MTLog.Fatal("No default route short name for %s!", gRoute.toStringPlus());
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return false; // OPT-IN feature
	}

	@Override
	public boolean tryRouteDescForMissingLongName() {
		return false; // OPT-IN feature
	}

	@NotNull
	@Override
	public String getRouteLongName(@NotNull GRoute gRoute) {
		String routeLongName = gRoute.getRouteLongNameOrDefault();
		if (tryRouteDescForMissingLongName()
				&& org.mtransit.commons.StringUtils.isEmpty(routeLongName)) {
			routeLongName = gRoute.getRouteDescOrDefault();
		}
		if (defaultRouteLongNameEnabled()
				&& org.mtransit.commons.StringUtils.isEmpty(routeLongName)) {
			return routeLongName; // empty route long name OK
		}
		return cleanRouteLongName(routeLongName);
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
	public boolean allowGTFSIdOverride() {
		return false; // if true, breaks GTFS-RT
	}

	@Override
	public boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge) {
		return mRoute.mergeLongName(mRouteToMerge);
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
		final String routeColorFixed = fixColor(gRoute.getRouteColor());
		if (routeColorFixed == null) {
			return provideMissingRouteColor(gRoute); // use agency color
		}
		if (getAgencyColor().equalsIgnoreCase(routeColorFixed)) {
			return provideMissingRouteColor(gRoute); // use agency color
		}
		return routeColorFixed;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute, @NotNull MAgency agency) {
		if (defaultAgencyColorEnabled()) {
			final String routeColorFixed = fixColor(gRoute.getRouteColor());
			if (routeColorFixed == null) {
				return provideMissingRouteColor(gRoute); // use agency color
			}
			if (agency.getColor().equalsIgnoreCase(routeColorFixed)) {
				return provideMissingRouteColor(gRoute); // use agency color
			}
			return routeColorFixed;
		}
		//noinspection deprecation
		return getRouteColor(gRoute);
	}

	@Nullable
	@Override
	public String provideMissingRouteColor(@NotNull GRoute gRoute) {
		return null;  // use agency color
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
		if (getOriginalAgencyRouteType() == null) {
			throw new MTLog.Fatal("ERROR: unspecified agency route type '%s'!", getOriginalAgencyRouteType());
		}
		if (GRouteType.isUnknown(gRoute.getRouteType())) {
			throw new MTLog.Fatal("ERROR: unexpected route type '%s'!", gRoute.getRouteType());
		}
		if (!GRouteType.isSameType(getOriginalAgencyRouteType(), gRoute.getRouteType())) {
			MTLog.logDebug("Route excluded because of different type: %s != %s (%s)", getOriginalAgencyRouteType(), gRoute.getRouteType(), gRoute.toStringPlus());
			return EXCLUDE;
		}
		//noinspection deprecation
		if (getAgencyId() != null && gRoute.isDifferentAgency(getAgencyId())) {
			//noinspection deprecation
			MTLog.logDebug("Route excluded because of different agency: %s != %s (%s)", getAgencyId(), gRoute.getAgencyId(), gRoute.toStringPlus());
			return EXCLUDE;
		}
		// MTLog.logDebug("Route NOT excluded: %s", gRoute.toStringPlus());
		return KEEP;
	}

	@NotNull
	@Override
	public String provideMissingTripHeadSign(@NotNull GTrip gTrip) {
		return gTrip.getTripHeadsignOrDefault();
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
	public boolean directionSplitterEnabled(long routeId) {
		return false; // OPT-IN feature
	}

	@Override
	public boolean directionOverrideId(long routeId) {
		return false;
	}

	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		//noinspection deprecation
		return cleanDirectionHeadsign(fromStopName, directionHeadSign);
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
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
	public boolean allowNonDescriptiveHeadSigns(long routeId) {
		return false; // this is bad, some transit agency data can NOT be fixed :(
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

	@Nullable
	@Override
	public String mergeComplexDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2) {
		return null; // pick a direction headsign (OR null)
	}

	static boolean directionHeadSignsDescriptiveS(@NotNull Map<Integer, String> directionHeadSigns) {
		if (directionHeadSigns.isEmpty()) {
			return true; // nothing is descriptive
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
				case "north / nord": // i18b
				case "n / nord": // i18b
					return MDirectionType.NORTH;
				case "southbound":
				case "south":
				case "sud":
				case "sb":
				case "s":
				case "sb / south":
				case "south / sud": // i18n
				case "s / sud": // i18n
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

	private boolean stopTimesHasPickupTypeNotRegular = false;  // opt-in feature

	@Override
	public void setStopTimesHasPickupTypeNotRegular(boolean hasNotRegular) {
		this.stopTimesHasPickupTypeNotRegular = hasNotRegular;
	}

	@Override
	public boolean stopTimesHasPickupTypeNotRegular() {
		return this.stopTimesHasPickupTypeNotRegular;
	}

	private boolean stopTimesHasDropOffTypeNotRegular = false;  // opt-in feature

	@Override
	public void setStopTimesHasDropOffTypeNotRegular(boolean hasNotRegular) {
		this.stopTimesHasDropOffTypeNotRegular = hasNotRegular;
	}

	@Override
	public boolean stopTimesHasDropOffTypeNotRegular() {
		return this.stopTimesHasDropOffTypeNotRegular;
	}

	@SuppressWarnings("DeprecatedIsStillUsed") // TODO migrate agencies parser
	@Deprecated
	@Override
	public void setForceStopTimeFirstNoDropOffLastNoPickupType(boolean force) {
		setForceStopTimeLastNoPickupType(force);
		setForceStopTimeFirstNoDropOffType(force);
	}

	@SuppressWarnings("DeprecatedIsStillUsed") // TODO migrate agencies parser
	@Deprecated
	@Override
	public boolean forceStopTimeFirstNoDropOffLastNoPickupType() {
		return false; // opt-in feature
	}

	private boolean forceStopTimeFirstNoPickupType = false;  // opt-in feature

	@Override
	public void setForceStopTimeLastNoPickupType(boolean force) {
		this.forceStopTimeFirstNoPickupType = force;
	}

	@Override
	public boolean forceStopTimeLastNoPickupType() {
		//noinspection deprecation // TODO migrate agencies parser
		if (forceStopTimeFirstNoDropOffLastNoPickupType()) {
			return true;
		}
		return this.forceStopTimeFirstNoPickupType; // opt-in feature
	}

	private boolean forceStopTimeFirstNoDropOff = false;  // opt-in feature

	@Override
	public void setForceStopTimeFirstNoDropOffType(boolean force) {
		this.forceStopTimeFirstNoDropOff = force;
	}

	@Override
	public boolean forceStopTimeFirstNoDropOffType() {
		//noinspection deprecation // TODO migrate agencies parser
		if (forceStopTimeFirstNoDropOffLastNoPickupType()) {
			return true;
		}
		return this.forceStopTimeFirstNoDropOff; // opt-in feature
	}

	@Override
	public boolean excludeStopTime(@NotNull GStopTime gStopTime) {
		// https://gtfs.org/schedule/best-practices/#stop_timestxt
		return GPickupType.NO_PICKUP == gStopTime.getPickupType() //
				&& GDropOffType.NO_DROP_OFF == gStopTime.getDropOffType();
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

	@NotNull
	@Override
	public String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign) {
		return cleanStopHeadSign(stopHeadsign);
	}

	@NotNull
	@Override
	public String cleanStopHeadSign(@NotNull String stopHeadsign) {
		return cleanTripHeadsign(stopHeadsign);
	}

	@NotNull
	@Override
	public String getStopCode(@NotNull GStop gStop) {
		return gStop.getStopCode();
	}

	@Deprecated
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
			throw new MTLog.Fatal(e, "Error while extracting stop ID from %s!", gStop.toStringPlus());
		}
	}

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopId) {
		return gStopId;
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

	@NotNull
	@Override
	public Pair<Integer, Integer> getTimes(@NotNull GStopTime gStopTime,
										   @NotNull List<GStopTime> tripStopTimes,
										   @NotNull SimpleDateFormat timeFormat) {
		if (!gStopTime.hasArrivalTime() || !gStopTime.hasDepartureTime()) {
			return extractTimes(gStopTime, tripStopTimes, timeFormat);
		} else {
			return new Pair<>(
					TimeUtils.cleanExtraSeconds(gStopTime.getArrivalTime()),
					TimeUtils.cleanExtraSeconds(gStopTime.getDepartureTime()));
		}
	}

	@NotNull
	private static Pair<Integer, Integer> extractTimes(GStopTime gStopTime,
													   @NotNull List<GStopTime> tripStopTimes,
													   SimpleDateFormat timeFormat) {
		try {
			Pair<Long, Long> timesInMs = extractTimeInMs(gStopTime, tripStopTimes);
			long arrivalTimeInMs = timesInMs.first;
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(arrivalTimeInMs);
			int arrivalTime = Integer.parseInt(timeFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				arrivalTime += 24_00_00;
			}
			long departureTimeInMs = timesInMs.second;
			calendar.setTimeInMillis(departureTimeInMs);
			int departureTime = Integer.parseInt(timeFormat.format(calendar.getTime()));
			if (calendar.get(Calendar.DAY_OF_YEAR) > 1) {
				departureTime += 24_00_00;
			}
			return new Pair<>(
					TimeUtils.cleanExtraSeconds(arrivalTime),
					TimeUtils.cleanExtraSeconds(departureTime)
			);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while interpolating times for %s!", gStopTime.toStringPlus(true));
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
		if (previousArrivalTime == -1 || previousDepartureTime == -1) {
			//noinspection deprecation
			MTLog.log("Trip ID '%s' stops: ", gStopTime.getTripId());
			for (GStopTime aStopTime : tripStopTimes) {
				MTLog.log("- %s", aStopTime);
			}
			//noinspection deprecation
			throw new MTLog.Fatal("Invalid stop time trip ID '%s' > no previous stop for %s!", gStopTime.getTripId(), gStopTime);
		}
		long previousArrivalTimeInMs = GTime.toMs(previousArrivalTime);
		long previousDepartureTimeInMs = GTime.toMs(previousDepartureTime);
		if (nextArrivalTime == -1 || nextDepartureTime == -1) {
			//noinspection deprecation
			MTLog.log("Trip ID '%s' stops: ", gStopTime.getTripId());
			for (GStopTime aStopTime : tripStopTimes) {
				MTLog.log("- %s", aStopTime);
			}
			//noinspection deprecation
			throw new MTLog.Fatal("Invalid stop time trip ID '%s' > no next stop for %s!", gStopTime.getTripId(), gStopTime);
		}
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

	@Nullable
	private static Period usefulPeriod = null;

	@SuppressWarnings("WeakerAccess")
	@NotNull
	public static HashSet<Integer> extractUsefulServiceIdInts(@NotNull String[] args, @NotNull DefaultAgencyTools agencyTools, boolean agencyFilter) {
		MTLog.log("Extracting useful service IDs...");
		usefulPeriod = new Period();
		boolean isCurrent = "current_".equalsIgnoreCase(args[2]);
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		boolean isCurrentOrNext = isCurrent || isNext;
		Calendar c = Calendar.getInstance();
		if (!isCurrentOrNext && TOMORROW) {
			c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		}
		usefulPeriod.todayStringInt = Integer.valueOf(GFieldTypes.DATE_FORMAT.format(c.getTime()));
		if (!isCurrentOrNext && OVERRIDE_DATE != null) {
			usefulPeriod.todayStringInt = OVERRIDE_DATE;
		}
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		List<GCalendar> gCalendars = gtfs.getAllCalendars();
		List<GCalendarDate> gCalendarDates = gtfs.getAllCalendarDates();
		final Period entirePeriod = getEntirePeriodMinMaxDate(gCalendars, gCalendarDates);
		MTLog.log("Schedule available from %s to %s.", entirePeriod.startDate, entirePeriod.endDate);
		boolean hasCurrent = false;
		if (gCalendars.size() > 0) {
			parseCalendars(gCalendars, gCalendarDates, GFieldTypes.DATE_FORMAT, c, usefulPeriod, isCurrentOrNext); // CURRENT
		} else if (gCalendarDates.size() > 0) {
			parseCalendarDates(gCalendarDates, c, usefulPeriod, isCurrentOrNext); // CURRENT
		} else {
			throw new MTLog.Fatal("NO schedule available for %s! (1)", usefulPeriod.todayStringInt);
		}
		if (!isNext //
				&& (usefulPeriod.startDate == null || usefulPeriod.endDate == null)) {
			if (isCurrent) {
				MTLog.log("No CURRENT schedules for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
				System.exit(0); // keeping current schedule
				return null;
			}
			//noinspection ConstantConditions
			throw new MTLog.Fatal("NO schedule available for %s! (start:%s|end:%s) (isCurrent:%s|isNext:%s)", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate, isCurrent, isNext);
		}
		if (usefulPeriod.todayStringInt != null && usefulPeriod.startDate != null && usefulPeriod.endDate != null) {
			hasCurrent = true;
		}
		MTLog.log("Generated on %s | Schedules from %s to %s.", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
		if (isNext) {
			if (hasCurrent //
					&& !diffLowerThan(GFieldTypes.DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate, MAX_NEXT_LOOKUP_IN_DAYS)) {
				MTLog.log("Skipping NEXT schedules... (%d days from %s to %s)", //
						TimeUnit.MILLISECONDS.toDays(diffInMs(GFieldTypes.DATE_FORMAT, c, usefulPeriod.todayStringInt, usefulPeriod.endDate)), //
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
				usefulPeriod.todayStringInt = incDateDays(GFieldTypes.DATE_FORMAT, c, usefulPeriod.endDate, 1); // start from next to current last date
			}
			usefulPeriod.startDate = null; // reset
			usefulPeriod.endDate = null; // reset
			if (gCalendars.size() > 0) {
				parseCalendars(gCalendars, gCalendarDates, GFieldTypes.DATE_FORMAT, c, usefulPeriod, false); // NEXT
			} else if (gCalendarDates.size() > 0) {
				parseCalendarDates(gCalendarDates, c, usefulPeriod, false); // NEXT
			}
			if (usefulPeriod.startDate == null || usefulPeriod.endDate == null) {
				MTLog.log("NO NEXT schedule available for %s. (start:%s|end:%s)", usefulPeriod.todayStringInt, usefulPeriod.startDate, usefulPeriod.endDate);
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
		improveUsefulPeriod(usefulPeriod, c, gCalendars, gCalendarDates);
		MTLog.log("Extracting useful service IDs... DONE");
		//noinspection UnusedAssignment // FIXME
		gtfs = null;
		return serviceIds;
	}

	private static void improveUsefulPeriod(@NotNull Period usefulPeriod, Calendar c, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		if (gCalendars != null && gCalendars.size() > 0 //
				&& gCalendarDates != null && gCalendarDates.size() > 0) {
			boolean newDateFound;
			do {
				newDateFound = false;
				int minNewStartDate = incDateDays(GFieldTypes.DATE_FORMAT, c, usefulPeriod.startDate, -1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isBefore(usefulPeriod.startDate)) {
						if (gCalendarDate.isBetween(minNewStartDate, usefulPeriod.startDate)) {
							MTLog.log("new useful period start date '%s' because it's close to previous useful period start date '%s'.", gCalendarDate.getDate(), usefulPeriod.startDate);
							usefulPeriod.startDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
			do {
				newDateFound = false;
				int minNewEndDate = incDateDays(GFieldTypes.DATE_FORMAT, c, usefulPeriod.endDate, +1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isAfter(usefulPeriod.endDate)) {
						if (gCalendarDate.isBetween(usefulPeriod.endDate, minNewEndDate)) {
							MTLog.log("new useful end date '%s' because it's close to previous useful period end data '%s'.", gCalendarDate.getDate(), usefulPeriod.endDate);
							usefulPeriod.endDate = gCalendarDate.getDate();
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
		}
	}

	private static void parseCalendarDates(List<GCalendarDate> gCalendarDates, Calendar c, Period p, boolean lookForward) {
		HashSet<Integer> todayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, p,
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
						MTLog.log("> new service ID from calendar date active on %s between %s and %s: '%s'", gCalendarDate.getDate(), p.startDate, p.endDate, gCalendarDate.getServiceIdInt());
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
			pNext.todayStringInt = incDateDays(GFieldTypes.DATE_FORMAT, c, p.endDate, 1);
			HashSet<Integer> nextDayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, pNext, 0, 0, 0);
			refreshStartEndDatesFromCalendarDates(pNext, nextDayServiceIds, gCalendarDates);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(GFieldTypes.DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("> new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(GFieldTypes.DATE_FORMAT, c, p.startDate, -1);
			HashSet<Integer> previousDayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, pPrevious, 0, 0, 0);
			refreshStartEndDatesFromCalendarDates(pPrevious, previousDayServiceIds, gCalendarDates);
			if (lookForward // NOT next schedule, only current schedule can look behind
					&& pPrevious.startDate != null && pPrevious.endDate != null
					&& diffLowerThan(GFieldTypes.DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.startDate = pPrevious.startDate;
				MTLog.log("> new start date '%s' because previous day has own service ID(s)", p.startDate);
				continue;
			}
			if (diffLowerThan(GFieldTypes.DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS)) {
				long currentPeriodCoverageInMs = p.startDate == null || p.endDate == null ? 0L :
						diffInMs(GFieldTypes.DATE_FORMAT, c,
								p.startDate, // morning
								p.endDate + 1 // midnight ≈ tomorrow
						);
				long nextPeriodCoverageInMs = pNext.startDate == null || pNext.endDate == null ? 0L :
						diffInMs(GFieldTypes.DATE_FORMAT, c,
								pNext.startDate, // morning
								pNext.endDate + 1 // midnight ≈ tomorrow
						);
				long previousPeriodCoverageInMs = pPrevious.startDate == null || pPrevious.endDate == null ? 0L :
						diffInMs(GFieldTypes.DATE_FORMAT, c,
								pPrevious.startDate, // morning
								pPrevious.endDate + 1 // midnight ≈ tomorrow
						);
				long previousToCurrent = previousPeriodCoverageInMs / currentPeriodCoverageInMs;
				long nextToCurrent = nextPeriodCoverageInMs / currentPeriodCoverageInMs;
				if (lookForward // NOT next schedule, only current schedule can look behind
						&& previousPeriodCoverageInMs > 0L
						&& (nextPeriodCoverageInMs <= 0L || previousPeriodCoverageInMs < nextPeriodCoverageInMs)
						&& previousToCurrent < MAX_CALENDAR_DATE_COVERAGE_RATIO) {
					p.startDate = incDateDays(GFieldTypes.DATE_FORMAT, c, p.startDate, -1); // start--
					MTLog.log("new start date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.startDate);
				} else if (nextToCurrent < MAX_CALENDAR_DATE_COVERAGE_RATIO) {
					p.endDate = incDateDays(GFieldTypes.DATE_FORMAT, c, p.endDate, 1); // end++
					MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.endDate);
				} else {
					MTLog.log("coverage lower than %s days but would add too many days (p: %sx, n: %sx)", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, nextToCurrent, previousToCurrent);
					break;
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
	private static HashSet<Integer> findCalendarDatesTodayServiceIds(@NotNull List<GCalendarDate> gCalendarDates,
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
					&& diffLowerThan(GFieldTypes.DATE_FORMAT, c, initialTodayStringInt, p.todayStringInt, MAX_LOOK_FORWARD_IN_DAYS)) {
				p.todayStringInt = incDateDays(GFieldTypes.DATE_FORMAT, c, p.todayStringInt, incDays);
				MTLog.log("new today because not enough service today: %s (initial today: %s, min: %s)", p.todayStringInt, initialTodayStringInt, minSizeForward);
				continue;
			} else if (todayServiceIds.size() < minSizeBackward //
					&& diffLowerThan(GFieldTypes.DATE_FORMAT, c, p.todayStringInt, initialTodayStringInt, MAX_LOOK_BACKWARD_IN_DAYS)) {
				p.todayStringInt = incDateDays(GFieldTypes.DATE_FORMAT, c, p.todayStringInt, -incDays);
				MTLog.log("new today because not enough service today: %s (initial today: %s, min: %s)", p.todayStringInt, initialTodayStringInt, minSizeBackward);
				continue;
			}
			break;
		}
		return todayServiceIds;
	}

	static void parseCalendars(@NotNull List<GCalendar> gCalendars, @Nullable List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean lookBackward) {
		findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);
		if (p.startDate == null || p.endDate == null) {
			MTLog.log("NO schedule available for %s in calendars. (start:%s|end:%s)", p.todayStringInt, p.startDate, p.endDate);
			return;
		}
		boolean newDates;
		while (true) {
			MTLog.log("Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.isOverlapping(p.startDate, p.endDate)) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection deprecation
					MTLog.logDebug("parseCalendars() > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
				if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
					MTLog.log("new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate, gCalendar.getStartDate(), p.startDate);
					p.startDate = gCalendar.getStartDate();
					newDates = true;
				}
				if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
					MTLog.log("new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate, gCalendar.getEndDate(), p.endDate);
					p.endDate = gCalendar.getEndDate();
					newDates = true;
				}
			}
			if (newDates) {
				continue;
			}
			Period pNext = new Period();
			pNext.todayStringInt = incDateDays(DATE_FORMAT, c, p.endDate, 1);
			findDayServiceIdsPeriod(gCalendars, gCalendarDates, pNext);
			if (pNext.startDate != null && pNext.endDate != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.startDate, pNext.endDate, MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.endDate = pNext.endDate;
				MTLog.log("new end date '%s' because next day has own service ID(s)", p.endDate);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.todayStringInt = incDateDays(DATE_FORMAT, c, p.startDate, -1);
			findDayServiceIdsPeriod(gCalendars, gCalendarDates, pPrevious);
			if (diffLowerThan(DATE_FORMAT, c, p.startDate, p.endDate, MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS)) {
				long nextPeriodCoverageInMs = pNext.startDate == null || pNext.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pNext.startDate, pNext.endDate);
				long previousPeriodCoverageInMs = pPrevious.startDate == null || pPrevious.endDate == null ? 0L : diffInMs(DATE_FORMAT, c, pPrevious.startDate, pPrevious.endDate);
				if (lookBackward // NOT next schedule, only current schedule can look behind
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

	static void findCalendarsTodayPeriod(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean lookBackward) {
		final int initialTodayStringInt = p.todayStringInt;
		boolean newDates;
		final Period entirePeriod = getEntirePeriodMinMaxDate(gCalendars, gCalendarDates);
		while (true) {
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.containsDate(p.todayStringInt)) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection deprecation
					MTLog.logDebug("findCalendarsTodayPeriod() > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
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
			if (p.startDate == null || p.endDate == null) {
				if (lookBackward) { // #CURRENT
					if (entirePeriod.endDate < p.todayStringInt) {
						p.todayStringInt = entirePeriod.endDate;
						MTLog.log("earlier today because no service: %s (initial today: %s)", p.todayStringInt, initialTodayStringInt);
						continue;
					}
				} else { // #NEXT
					if (entirePeriod.startDate > p.todayStringInt) {
						p.todayStringInt = entirePeriod.startDate;
						MTLog.log("latter today because no service: %s (initial today: %s)", p.todayStringInt, initialTodayStringInt);
						continue;
					}
				}
			}
			break;
		}
	}

	static void findDayServiceIdsPeriod(List<GCalendar> gCalendars, @Nullable List<GCalendarDate> gCalendarDates, Period p) {
		boolean newDates;
		while (true) {
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.containsDate(p.todayStringInt)) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection deprecation
					MTLog.logDebug("findDayServiceIdsPeriod() > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
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
			if (newDates) {
				continue;
			}
			break;
		}
		if (p.startDate == null || p.endDate == null) {
			MTLog.logDebug("findDayServiceIdsPeriod() > NO schedule available for %s in calendars. (start:%s|end:%s)", p.todayStringInt, p.startDate, p.endDate);
			return;
		}
		while (true) {
			MTLog.logDebug("findDayServiceIdsPeriod() > Schedules from %s to %s... ", p.startDate, p.endDate);
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.isOverlapping(p.startDate, p.endDate)) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection deprecation
					MTLog.logDebug("findDayServiceIdsPeriod() > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
				if (p.startDate == null || gCalendar.startsBefore(p.startDate)) {
					MTLog.logDebug("findDayServiceIdsPeriod() > new start date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate, gCalendar.getStartDate(), p.startDate);
					p.startDate = gCalendar.getStartDate();
					newDates = true;
				}
				if (p.endDate == null || gCalendar.endsAfter(p.endDate)) {
					MTLog.logDebug("findDayServiceIdsPeriod() > new end date from calendar active between %s and %s: %s (was: %s)", p.startDate, p.endDate, gCalendar.getEndDate(), p.endDate);
					p.endDate = gCalendar.getEndDate();
					newDates = true;
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
	}

	@NotNull
	private static HashSet<Integer> getPeriodServiceIds(Integer startDate, Integer endDate, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		HashSet<Integer> serviceIdInts = new HashSet<>();
		if (gCalendars != null) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isInside(startDate, endDate)) {
					if (gCalendar.isServiceIdInts(serviceIdInts)) {
						continue;
					}
					if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates, startDate, endDate)) {
						//noinspection deprecation
						MTLog.logDebug("getPeriodServiceIds() > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate, gCalendar.getServiceId());
						continue;
					}
					//noinspection deprecation
					MTLog.log("new service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
					serviceIdInts.add(gCalendar.getServiceIdInt());
				}
			}
		}
		if (gCalendarDates != null) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(startDate, endDate)) {
					if (gCalendarDate.isServiceIdInts(serviceIdInts)) {
						continue;
					}
					if (gCalendarDate.getExceptionType() == GCalendarDatesExceptionType.SERVICE_REMOVED) {
						//noinspection deprecation
						MTLog.log("ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate, gCalendarDate.getServiceId());
						continue;
					}
					//noinspection deprecation
					MTLog.log("new service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
					serviceIdInts.add(gCalendarDate.getServiceIdInt());
				}
			}
		}
		MTLog.log("Service IDs: %s.", GIDs.toStringPlus(serviceIdInts));
		return serviceIdInts;
	}

	private static Period getEntirePeriodMinMaxDate(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		final Period p = new Period();
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
		return p;
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
