package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CharUtils;
import org.mtransit.commons.CleanUtils;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.CommonsApp;
import org.mtransit.commons.GTFSCommons;
import org.mtransit.commons.StringsCleaner;
import org.mtransit.parser.config.Configs;
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
import org.mtransit.parser.mt.MDataChangedManager;
import org.mtransit.parser.mt.MGenerator;
import org.mtransit.parser.mt.MReader;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionCardinalType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MRouteSNToIDConverter;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MServiceId;
import org.mtransit.parser.mt.data.MServiceIds;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MDirection;
import org.mtransit.parser.mt.data.MString;
import org.mtransit.parser.mt.data.MStrings;
import org.mtransit.parser.mt.data.MTripId;
import org.mtransit.parser.mt.data.MTripIds;
import org.mtransit.parser.mt.data.MVerify;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DefaultAgencyTools implements GAgencyTools {

	static {
		CommonsApp.setup(false);
	}

	@SuppressWarnings("WeakerAccess")
	protected static final boolean EXCLUDE = true;
	@SuppressWarnings("WeakerAccess")
	protected static final boolean KEEP = false;

	private static final int MAX_NEXT_LOOKUP_IN_DAYS = 60;

	private static final int MAX_LOOK_BACKWARD_IN_DAYS = 10 * 365; // used for CURRENT schedule from calendar_dates.txt all in the past
	private static final int MAX_LOOK_FORWARD_IN_DAYS = 60;

	private static final int MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS = 5; // = 6 days
	private static final int MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS = 14;

	// 2024-04-09: 10x -> 3x because merging 2 schedule can create very bad schedule info #GRTbus
	private static final long MAX_CALENDAR_DATE_COVERAGE_RATIO = 3;

	private static final int MIN_PREVIOUS_NEXT_ADDED_DAYS = 2; //  = 1 day

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

	private static final SimpleDateFormat DATE_FORMAT = GFieldTypes.makeDateFormat();

	@SuppressWarnings({"unused", "WeakerAccess"})
	public int getTodayDateInt() {
		return Integer.parseInt(DATE_FORMAT.format(Calendar.getInstance().getTime()));
	}

	public static void main(@NotNull String[] args) {
		new DefaultAgencyTools().start(args);
		// throw new MTLog.Fatal("NEED TO IMPLEMENT MAIN METHOD"); // UNTIL WE HAVE FULLY MIGRATED TO JSON CONFIG FILE
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	public void start(@NotNull String[] args) {
		if (args.length < 3) {
			throw new MTLog.Fatal("Invalid number(%d) of arguments! (%s)", args.length, Arrays.asList(args));
		}
		MTLog.log("Reading configuration...");
		Configs.load();
		MTLog.log("Generating data...");
		MTLog.logDebug("Args [%d]: %s.", args.length, Arrays.asList(args));
		final List<MServiceDate> lastServiceDates = MReader.loadServiceDates(args[2]);
		final List<MTripId> lastTripIds = MReader.loadTripIds(args[2]);
		final List<MServiceId> lastServiceIds = MReader.loadServiceIds(args[2]);
		final List<MString> lastStrings = MReader.loadStrings(args[2]);
		MServiceIds.addAll(lastServiceIds);
		MTripIds.addAll(lastTripIds);
		MStrings.addAll(lastStrings);
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true, lastServiceDates);
		final String inputUrl = args.length >= 5 ? args[4] : null;
		if (excludingAll()) {
			MGenerator.dumpFiles(this, null, args[0], args[1], args[2], inputUrl, true);
			return;
		}
		final long start = System.currentTimeMillis();
		final GSpec gtfs = GReader.readGtfsZipFile(args[0], this, false, false);
		MDataChangedManager.avoidCalendarDatesDataChanged(lastServiceDates, gtfs);
		gtfs.cleanupStops();
		gtfs.cleanupExcludedData();
		gtfs.cleanupStopTimesPickupDropOffTypes(this);
		gtfs.generateTripStops();
		if (args.length >= 4 && Boolean.parseBoolean(args[3])) {
			gtfs.generateStopTimesFromFrequencies(this);
		}
		gtfs.splitByRouteId(this);
		gtfs.clearRawData();
		final MSpec mSpec = MGenerator.generateMSpec(gtfs, this);
		if (Constants.SKIP_FILE_DUMP) {
			return; // DEBUG
		}
		MGenerator.dumpFiles(this, mSpec, args[0], args[1], args[2], inputUrl, false);
		MVerify.verify(mSpec, this); // after dump files to have all values
		MTLog.log("Generating data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
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

	@SuppressWarnings("WeakerAccess")
	@Nullable
	public Locale getFirstLanguage() {
		final List<Locale> supportedLanguages = getSupportedLanguages();
		return supportedLanguages == null || supportedLanguages.isEmpty() ? null : supportedLanguages.get(0);
	}

	@SuppressWarnings("WeakerAccess")
	@NotNull
	public Locale getFirstLanguageNN() {
		final Locale firstLanguage = getFirstLanguage();
		if (firstLanguage == null) {
			throw new MTLog.Fatal("NEED TO PROVIDE SUPPORTED LANGUAGES");
		}
		return firstLanguage;
	}

	@Override
	public void setAgencyName(@Nullable String agencyName) {
	}

	@NotNull
	public String getAgencyName() {
		return "Agency Name";
	}

	@SuppressWarnings("unused")
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

	@SuppressWarnings("removal")
	@Override
	public boolean defaultExcludeEnabled() {
		return true;
	}

	@Override
	public boolean defaultStringsCleanerEnabled() {
		if (Configs.getAgencyConfig() != null) {
			return Configs.getAgencyConfig().getDefaultStringsCleanerEnabled();
		}
		return false; // OPT-IN feature
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
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
		if (Configs.getAgencyConfig() != null) {
			return Configs.getAgencyConfig().getDefaultColor();
		}
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
		if (Configs.getAgencyConfig() != null) {
			return Configs.getAgencyConfig().getTargetRouteTypeId();
		}
		throw new MTLog.Fatal("AGENCY ROUTE TYPE NOT PROVIDED");
	}

	@Nullable
	public Integer getAgencyExtendedRouteType() {
		return null; // optional
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
		//noinspection DiscouragedApi
		if (getAgencyId() != null && gAgency.isDifferentAgency(getAgencyId())) {
			return EXCLUDE;
		}
		return KEEP;
	}

	private final Map<String, String> serviceIdToCleanupServiceId = new HashMap<>();

	@NotNull
	@Override
	public String cleanServiceId(@NotNull String gServiceId) {
		final String cleanServiceId = GTFSCommons.cleanOriginalId(gServiceId, getServiceIdCleanupPattern());
		serviceIdToCleanupServiceId.put(gServiceId, cleanServiceId);
		return cleanServiceId;
	}

	@Nullable
	@Override
	public String getServiceIdCleanupRegex() {
		if (Configs.getAgencyConfig() != null) {
			return Configs.getAgencyConfig().getServiceIdCleanupRegex();
		}
		return null; // OPT-IN feature
	}

	@Nullable
	private Pattern serviceIdCleanupPattern = null;

	private boolean serviceIdCleanupPatternSet = false;

	private @Nullable Pattern getServiceIdCleanupPattern() {
		if (this.serviceIdCleanupPattern == null && !serviceIdCleanupPatternSet) {
			this.serviceIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getServiceIdCleanupRegex());
			this.serviceIdCleanupPatternSet = true;
		}
		return this.serviceIdCleanupPattern;
	}

	@Override
	public boolean verifyServiceIdsUniqueness() {
		if (Configs.getAgencyConfig() != null) {
			if (Configs.getAgencyConfig().getServiceIdNotUniqueAllowed()) return false;
		}
		return getServiceIdCleanupRegex() != null; // OPT-IN feature
	}

	@NotNull
	public Map<String, String> getServiceIdToCleanupServiceId() {
		return serviceIdToCleanupServiceId;
	}

	private final Map<String, String> routeIdToCleanupRouteId = new HashMap<>();

	@NotNull
	@Override
	public String cleanRouteOriginalId(@NotNull String gRouteId) {
		if (Configs.getRouteConfig().getRouteIdCleanMerged()) {
			gRouteId = CleanUtils.cleanMergedID(gRouteId);
		}
		final String cleanRouteId = GTFSCommons.cleanOriginalId(gRouteId, getRouteIdCleanupPattern());
		this.routeIdToCleanupRouteId.put(gRouteId, cleanRouteId);
		return cleanRouteId;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		try {
			//noinspection DiscouragedApi
			final String routeIdS =
					useRouteShortNameForRouteId() ? cleanRouteShortName(getRouteShortName(gRoute))
							: gRoute.getRouteId();
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
		return Configs.getRouteConfig().convertRouteIdFromShortNameNotSupported(routeShortName);
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
		return Configs.getRouteConfig().getDefaultRouteIdEnabled();
	}

	@Override
	public boolean useRouteShortNameForRouteId() {
		return Configs.getRouteConfig().getUseRouteShortNameForRouteId();
	}

	@Override
	public boolean useRouteIdForRouteShortName() {
		return Configs.getRouteConfig().getUseRouteIdForRouteShortName();
	}

	@Nullable
	@Override
	public String getRouteIdCleanupRegex() {
		return Configs.getRouteConfig().getRouteIdCleanupRegex();
	}

	@Override
	public boolean verifyRouteIdsUniqueness() {
		if (Configs.getRouteConfig().getRouteIdNotUniqueAllowed()) return false;
		return getRouteIdCleanupRegex() != null; // OPT-IN feature
	}

	@NotNull
	public Map<String, String> getRouteIdToCleanupRouteId() {
		return this.routeIdToCleanupRouteId;
	}

	@Nullable
	private Pattern routeIdCleanupPattern = null;

	private boolean routeIdCleanupPatternSet = false;

	@Nullable
	private Pattern getRouteIdCleanupPattern() {
		if (this.routeIdCleanupPattern == null && !routeIdCleanupPatternSet) {
			this.routeIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getRouteIdCleanupRegex());
			this.routeIdCleanupPatternSet = true;
		}
		return this.routeIdCleanupPattern;
	}

	@NotNull
	@Override
	public String getRouteShortName(@NotNull GRoute gRoute) {
		//noinspection DiscouragedApi
		final String routeShortName =
				useRouteIdForRouteShortName() ? gRoute.getRouteId()
						: gRoute.getRouteShortName();
		if (org.mtransit.commons.StringUtils.isEmpty(routeShortName)) {
			return provideMissingRouteShortName(gRoute);
		}
		return cleanRouteShortName(routeShortName);
	}

	@NotNull
	@Override
	public String cleanRouteShortName(@NotNull String routeShortName) {
		if (org.mtransit.commons.StringUtils.isEmpty(routeShortName)) {
			throw new MTLog.Fatal("No default route short name for '%s'!", routeShortName);
		}
		routeShortName = Configs.getRouteConfig().cleanRouteShortName(routeShortName);
		return routeShortName.trim();
	}

	@NotNull
	@Override
	public String provideMissingRouteShortName(@NotNull GRoute gRoute) {
		if (Configs.getRouteConfig().getUseRouteLongNameForMissingRouteShortName()) {
			return gRoute.getRouteLongNameOrDefault();
		}
		throw new MTLog.Fatal("No default route short name for %s!", gRoute.toStringPlus());
	}

	@Override
	public boolean defaultRouteLongNameEnabled() {
		return Configs.getRouteConfig().getDefaultRouteLongNameEnabled();
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
		routeLongName = Configs.getRouteConfig().cleanRouteLongName(routeLongName);
		if (defaultStringsCleanerEnabled()) {
			return StringsCleaner.cleanRouteLongName(routeLongName, getSupportedLanguages());
		}
		return org.mtransit.commons.CleanUtils.cleanLabel(getFirstLanguageNN(), routeLongName);
	}

	@Deprecated
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
		return fixColor(Configs.getRouteConfig().getRouteColor(gRoute)); // or use agency color
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
			MTLog.logDebug("Route excluded because of different type: %s != %s (%s)", getOriginalAgencyRouteType(), gRoute.getRouteType(), gRoute.toStringShort());
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
	public void setDirectionHeadsign(@NotNull MRoute mRoute, @NotNull MDirection mDirection, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		final GRoute gRoute = gtfs.getRoute(gTrip.getRouteIdInt());
		if (gRoute == null) {
			//noinspection DiscouragedApi
			throw new MTLog.Fatal("Trying to set direction head-sign w/o valid GTFS route (ID: %s)", gTrip.getRouteId());
		}
		final boolean fromStopName = mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STOP_ID;
		if (directionFinderEnabled(mRoute.getId(), gRoute)) {
			mDirection.setHeadsignString(
					cleanDirectionHeadsign(gRoute, gTrip.getDirectionIdOrDefault(), fromStopName, gTrip.getTripHeadsignOrDefault()),
					gTrip.getDirectionIdOrDefault()
			);
			return;
		}
		if (gTrip.getDirectionId() == null || gTrip.getDirectionId() < 0 || gTrip.getDirectionId() > 1) {
			throw new MTLog.Fatal("Default agency implementation requires 'direction_id' field in '%s'!", gTrip.toStringPlus());
		}
		try {
			mDirection.setHeadsignString(
					cleanDirectionHeadsign(gRoute, gTrip.getDirectionIdOrDefault(), fromStopName, gTrip.getTripHeadsignOrDefault()),
					gTrip.getDirectionIdOrDefault()
			);
		} catch (NumberFormatException nfe) {
			throw new MTLog.Fatal(nfe, "Default agency implementation not possible!");
		}
	}

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = Configs.getRouteConfig().cleanTripHeadsign(tripHeadsign);
		if (defaultStringsCleanerEnabled()) {
			return StringsCleaner.cleanTripHeadsign(tripHeadsign, getSupportedLanguages(), Configs.getRouteConfig().getTripHeadsignRemoveVia());
		}
		return tripHeadsign;
	}

	@Nullable
	@Override
	public String getTripIdCleanupRegex() {
		return Configs.getRouteConfig().getTripIdCleanupRegex();
	}

	@Override
	public boolean verifyTripIdsUniqueness() {
		if (Configs.getRouteConfig().getTripIdNotUniqueAllowed()) return false;
		return getTripIdCleanupRegex() != null; // OPT-IN feature
	}

	@NotNull
	public Map<String, String> getTripIdToCleanupTripId() {
		return this.tripIdToCleanupTripId;
	}

	@Nullable
	private Pattern tripIdCleanupPattern = null;

	private boolean tripIdCleanupPatternSet = false;

	@Nullable
	private Pattern getTripIdCleanupPattern() {
		if (this.tripIdCleanupPattern == null && !tripIdCleanupPatternSet) {
			this.tripIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getTripIdCleanupRegex());
			this.tripIdCleanupPatternSet = true;
		}
		return this.tripIdCleanupPattern;
	}

	private final Map<String, String> tripIdToCleanupTripId = new HashMap<>();

	@Override
	public @NotNull String cleanTripOriginalId(@NotNull String gOriginalTripId) {
		final String cleanTripId = GTFSCommons.cleanOriginalId(gOriginalTripId, getTripIdCleanupPattern());
		this.tripIdToCleanupTripId.put(gOriginalTripId, cleanTripId);
		return cleanTripId;
	}

	@Override
	public void forgetOriginalTripId(@NotNull String gOriginalTripId) {
		this.tripIdToCleanupTripId.remove(gOriginalTripId);
	}

	@Override
	public boolean directionSplitterEnabled(long routeId) {
		return false; // OPT-IN feature
	}

	@Override
	public boolean directionOverrideId(long routeId) {
		return false;
	}

	@Override
	public boolean removeRouteLongNameFromDirectionHeadsign() {
		return Configs.getRouteConfig().getDirectionHeadsignRemoveRouteLongName();
	}

	/**
	 * @param directionId {@link org.mtransit.parser.gtfs.data.GDirectionId} (0 or 1 or missing/generated)
	 */
	@Override
	public @NotNull String cleanDirectionHeadsign(@Nullable GRoute gRoute, int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		if (gRoute != null && removeRouteLongNameFromDirectionHeadsign()) {
			if (directionHeadSign.equals(gRoute.getRouteLongNameOrDefault())) {
				directionHeadSign = "";
			}
		}
		//noinspection deprecation
		return cleanDirectionHeadsign(directionId, fromStopName, directionHeadSign);
	}

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	@NotNull
	@Override
	public String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign) {
		directionHeadSign = Configs.getRouteConfig().cleanDirectionHeadsign(directionHeadSign);
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
		return Configs.getRouteConfig().getDirectionFinderEnabled();
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
	public boolean allowDuplicateKeyError() {
		return false; // this is bad, probably wrong copy-paste -> duplicated lines
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
				&& deprecatedDirectionType != MDirection.HEADSIGN_TYPE_STRING) {
			return Arrays.asList(
					deprecatedDirectionType,
					MDirection.HEADSIGN_TYPE_STRING
			);
		}
		return Collections.singletonList(
				MDirection.HEADSIGN_TYPE_STRING // default = string
		);
	}

	@Nullable
	@Override
	public MDirectionCardinalType convertDirection(@Nullable String headSign) {
		if (headSign != null) {
			if (getDirectionTypes().contains(MDirection.HEADSIGN_TYPE_DIRECTION)) {
				final String tripHeadsignLC = headSign.toLowerCase(Locale.ENGLISH);
				switch (tripHeadsignLC) {
				case "eastbound":
				case "east":
				case "est":
				case "eb":
				case "e":
				case "eb / east":
					return MDirectionCardinalType.EAST;
				case "westbound":
				case "west":
				case "ouest":
				case "wb":
				case "w":
				case "o":
				case "wb / west":
					return MDirectionCardinalType.WEST;
				case "northbound":
				case "north":
				case "nord":
				case "nb":
				case "n":
				case "nb / north":
				case "north / nord": // i18b
				case "n / nord": // i18b
					return MDirectionCardinalType.NORTH;
				case "southbound":
				case "south":
				case "sud":
				case "sb":
				case "s":
				case "sb / south":
				case "south / sud": // i18n
				case "s / sud": // i18n
					return MDirectionCardinalType.SOUTH;
				}
				if (getDirectionTypes().size() == 1) { // only DIRECTION!
					throw new MTLog.Fatal("Unexpected direction for '%s'!", headSign);
				}
			}
		}
		return null; // no direction conversion by default
	}

	@Deprecated
	@Override
	public boolean mergeHeadsign(@NotNull MDirection mDirection, @NotNull MDirection mDirectionToMerge) {
		if (directionFinderEnabled()) {
			throw new MTLog.Fatal("Unexpected directions to merge: %s & %s!", mDirection, mDirectionToMerge);
		}
		return mDirection.mergeHeadsignValue(mDirectionToMerge);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MDirection mDirection, @NotNull MDirection mDirectionToMerge, @NotNull GRoute gRoute) {
		if (directionFinderEnabled(mDirection.getRouteId(), gRoute)) {
			throw new MTLog.Fatal("Unexpected directions to merge: %s & %s!", mDirection, mDirectionToMerge);
		}
		return mDirection.mergeHeadsignValue(mDirectionToMerge);
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
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return KEEP;
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDate) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDate, this.serviceIdInts);
		}
		return false;
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return false;
	}

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		if (defaultStringsCleanerEnabled()) {
			return StringsCleaner.cleanStopName(gStopName, getSupportedLanguages());
		}
		return org.mtransit.commons.CleanUtils.cleanLabel(getFirstLanguageNN(), gStopName);
	}

	@Override
	public boolean removeTripHeadsignFromStopHeadsign() {
		return Configs.getRouteConfig().getStopHeadsignRemoveTripHeadsign();
	}

	@Override
	public boolean removeRouteLongNameFromStopHeadsign() {
		return Configs.getRouteConfig().getStopHeadsignRemoveRouteLongName();
	}

	@NotNull
	@Override
	public String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign) {
		if (removeTripHeadsignFromStopHeadsign()) {
			if (stopHeadsign.equals(gTrip.getTripHeadsignOrDefault())) {
				stopHeadsign = "";
			}
		}
		if (removeRouteLongNameFromStopHeadsign()) {
			if (stopHeadsign.equals(gRoute.getRouteLongNameOrDefault())) {
				stopHeadsign = "";
			}
		}
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
			//noinspection DiscouragedApi
			final String stopIdS =
					useStopCodeForStopId() ? cleanStopOriginalId(getStopCode(gStop))
							: gStop.getStopId();
			if (!CharUtils.isDigitsOnly(stopIdS)) {
				final Integer stopIdSInt = convertStopIdFromCodeNotSupported(stopIdS);
				if (stopIdSInt != null) {
					return stopIdSInt;
				}
			}
			return Integer.parseInt(stopIdS);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while extracting stop ID from %s!", gStop.toStringPlus());
		}
	}

	@Nullable
	@Override
	public Integer convertStopIdFromCodeNotSupported(@NotNull String stopCode) {
		return Configs.getRouteConfig().convertStopIdFromCodeNotSupported(stopCode);
	}

	@Override
	public boolean useStopCodeForStopId() {
		return Configs.getRouteConfig().getUseStopCodeForStopId();
	}

	@Override
	public @Nullable String getStopIdCleanupRegex() {
		return Configs.getRouteConfig().getStopIdCleanupRegex();
	}

	@Override
	public boolean verifyStopIdsUniqueness() {
		if (Configs.getRouteConfig().getStopIdNotUniqueAllowed()) return false;
		return getStopIdCleanupRegex() != null; // OPT-IN feature
	}

	@NotNull
	public Map<String, String> getStopIdToCleanupStopId() {
		return this.stopIdToCleanupStopId;
	}

	@Nullable
	private Pattern stopIdCleanupPattern = null;

	private boolean stopIdCleanupPatternSet = false;

	@Nullable
	private Pattern getStopIdCleanupPattern() {
		if (this.stopIdCleanupPattern == null && !stopIdCleanupPatternSet) {
			this.stopIdCleanupPattern = GTFSCommons.makeIdCleanupPattern(getStopIdCleanupRegex());
			this.stopIdCleanupPatternSet = true;
		}
		return this.stopIdCleanupPattern;
	}

	private final Map<String, String> stopIdToCleanupStopId = new HashMap<>();

	@NotNull
	@Override
	public String cleanStopOriginalId(@NotNull String gStopOriginalId) {
		final String cleanStopId = GTFSCommons.cleanOriginalId(gStopOriginalId, getStopIdCleanupPattern());
		this.stopIdToCleanupStopId.put(gStopOriginalId, cleanStopId);
		return cleanStopId;
	}

	@Override
	public void forgetOriginalStopId(@NotNull String gStopOriginalId) {
		this.stopIdToCleanupStopId.remove(gStopOriginalId);
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
			//noinspection DiscouragedApi
			MTLog.log("Trip ID '%s' stops: ", gStopTime.getTripId());
			for (GStopTime aStopTime : tripStopTimes) {
				MTLog.log("- %s", aStopTime.toStringPlus(true));
			}
			//noinspection DiscouragedApi
			throw new MTLog.Fatal("Invalid stop time trip ID '%s' > no previous stop for %s!", gStopTime.getTripId(), gStopTime.toStringPlus(true));
		}
		long previousArrivalTimeInMs = GTime.toMs(previousArrivalTime);
		long previousDepartureTimeInMs = GTime.toMs(previousDepartureTime);
		if (nextArrivalTime == -1 || nextDepartureTime == -1) {
			//noinspection DiscouragedApi
			MTLog.log("Trip ID '%s' stops: ", gStopTime.getTripId());
			for (GStopTime aStopTime : tripStopTimes) {
				MTLog.log("- %s", aStopTime.toStringPlus(true));
			}
			//noinspection DiscouragedApi
			throw new MTLog.Fatal("Invalid stop time trip ID '%s' > no next stop for %s!", gStopTime.getTripId(), gStopTime.toStringPlus(true));
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

	@Nullable
	private static Period usefulPeriod = null;

	@SuppressWarnings("WeakerAccess")
	@NotNull
	public static HashSet<Integer> extractUsefulServiceIdInts(
			@NotNull String[] args,
			@NotNull DefaultAgencyTools agencyTools,
			boolean agencyFilter,
			@Nullable List<MServiceDate> lastServiceDates) {
		MTLog.log("Extracting useful service IDs...");
		usefulPeriod = new Period();
		boolean isCurrent = "current_".equalsIgnoreCase(args[2]);
		boolean isNext = "next_".equalsIgnoreCase(args[2]);
		boolean isCurrentOrNext = isCurrent || isNext;
		Calendar c = Calendar.getInstance();
		if (!isCurrentOrNext && TOMORROW) {
			c.add(Calendar.DAY_OF_MONTH, 1); // TOMORROW (too late to publish today's schedule)
		}
		usefulPeriod.setTodayStringInt(Integer.valueOf(DATE_FORMAT.format(c.getTime())));
		if (!isCurrentOrNext && OVERRIDE_DATE != null) {
			usefulPeriod.setTodayStringInt(OVERRIDE_DATE);
		}
		GSpec gtfs = GReader.readGtfsZipFile(args[0], agencyTools, !agencyFilter, agencyFilter);
		MDataChangedManager.avoidCalendarDatesDataChanged(lastServiceDates, gtfs);
		if (agencyFilter) {
			gtfs.cleanupExcludedServiceIds();
		}
		final List<GCalendar> gCalendars = gtfs.getAllCalendars();
		final List<GCalendarDate> gCalendarDates = gtfs.getAllCalendarDates();
		final Period entirePeriod = getEntirePeriodMinMaxDate(gCalendars, gCalendarDates);
		MTLog.log("* Entire schedule available: from %s to %s.", entirePeriod.getStartDate(), entirePeriod.getEndDate());
		MTLog.log("------------------------------");
		MTLog.log("* Looking for CURRENT schedules...");
		boolean hasCurrent = false;
		if (!gCalendars.isEmpty()) {
			parseCalendars(gCalendars, gCalendarDates, DATE_FORMAT, c, usefulPeriod, isCurrentOrNext); // CURRENT
		} else if (!gCalendarDates.isEmpty()) {
			parseCalendarDates(gCalendarDates, c, usefulPeriod, isCurrentOrNext); // CURRENT
		} else {
			throw new MTLog.Fatal("NO schedule available for %s! (1)", usefulPeriod.getTodayStringInt());
		}
		if (!isNext //
				&& (usefulPeriod.getStartDate() == null || usefulPeriod.getEndDate() == null)) {
			if (isCurrent) {
				MTLog.log("* No CURRENT schedules for %s. (start:%s|end:%s)", usefulPeriod.getTodayStringInt(), usefulPeriod.getStartDate(), usefulPeriod.getEndDate());
				if (MGenerator.checkDataFilesExists(args[2])) {
					System.exit(0); // keeping current schedule
				} else {
					throw new MTLog.Fatal("NO CURRENT schedules already available!");
				}
				return null;
			}
			//noinspection ConstantConditions
			throw new MTLog.Fatal("NO schedule available for %s! (start:%s|end:%s) (isCurrent:%s|isNext:%s)", usefulPeriod.getTodayStringInt(), usefulPeriod.getStartDate(), usefulPeriod.getEndDate(), isCurrent, isNext);
		}
		if (usefulPeriod.getTodayStringInt() != null && usefulPeriod.getStartDate() != null && usefulPeriod.getEndDate() != null) {
			hasCurrent = true;
		}
		MTLog.log("* Generated on %s | CURRENT Schedules from %s to %s.", usefulPeriod.getTodayStringInt(), usefulPeriod.getStartDate(), usefulPeriod.getEndDate());
		MTLog.log("------------------------------");
		if (isNext) {
			if (hasCurrent //
					&& !diffLowerThan(DATE_FORMAT, c, usefulPeriod.getTodayStringInt(), usefulPeriod.getEndDate(), MAX_NEXT_LOOKUP_IN_DAYS)) {
				MTLog.log("* Skipping NEXT schedules... (%d days from %s to %s)", //
						TimeUnit.MILLISECONDS.toDays(diffInMs(DATE_FORMAT, c, usefulPeriod.getTodayStringInt(), usefulPeriod.getEndDate())), //
						usefulPeriod.getTodayStringInt(), //
						usefulPeriod.getEndDate());
				usefulPeriod.setTodayStringInt(null); // reset
				usefulPeriod.setStartDate(null); // reset
				usefulPeriod.setEndDate(null); // reset
				//noinspection UnusedAssignment // FIXME
				gtfs = null;
				return new HashSet<>(); // non-null = service IDs
			}
			MTLog.log("* Looking for NEXT schedules...");
			if (hasCurrent) {
				usefulPeriod.setTodayStringInt(incDateDays(DATE_FORMAT, c, usefulPeriod.getEndDate(), 1)); // start from next to current last date
			} else { // reset today for next schedule
				usefulPeriod.setTodayStringInt(Integer.valueOf(DATE_FORMAT.format(Calendar.getInstance().getTime())));
			}
			usefulPeriod.setStartDate(null); // reset
			usefulPeriod.setEndDate(null); // reset
			if (!gCalendars.isEmpty()) {
				parseCalendars(gCalendars, gCalendarDates, DATE_FORMAT, c, usefulPeriod, false); // NEXT
			} else if (!gCalendarDates.isEmpty()) {
				parseCalendarDates(gCalendarDates, c, usefulPeriod, false); // NEXT
			}
			if (usefulPeriod.getStartDate() == null || usefulPeriod.getEndDate() == null) {
				MTLog.log("* NO NEXT schedule available for %s. (start:%s|end:%s)", usefulPeriod.getTodayStringInt(), usefulPeriod.getStartDate(), usefulPeriod.getEndDate());
				usefulPeriod.setTodayStringInt(null); // reset
				usefulPeriod.setStartDate(null); // reset
				usefulPeriod.setEndDate(null); // reset
				//noinspection UnusedAssignment // FIXME
				gtfs = null;
				return new HashSet<>(); // non-null = service IDs
			}
			MTLog.log("* Generated on %s | NEXT Schedules from %s to %s.", usefulPeriod.getTodayStringInt(), usefulPeriod.getStartDate(), usefulPeriod.getEndDate());
			MTLog.log("------------------------------");
		}
		final HashSet<Integer> serviceIds = getPeriodServiceIds(usefulPeriod.getStartDate(), usefulPeriod.getEndDate(), gCalendars, gCalendarDates);
		improveUsefulPeriod(usefulPeriod, c, gCalendars, gCalendarDates);
		MTLog.log("Extracting useful service IDs... DONE");
		//noinspection UnusedAssignment // FIXME
		gtfs = null;
		return serviceIds;
	}

	private static void improveUsefulPeriod(@NotNull Period usefulPeriod, Calendar c, List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		if (gCalendars != null && !gCalendars.isEmpty() //
				&& gCalendarDates != null && !gCalendarDates.isEmpty()) {
			boolean newDateFound;
			do {
				newDateFound = false;
				int minNewStartDate = incDateDays(DATE_FORMAT, c, usefulPeriod.getStartDate(), -1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isBefore(usefulPeriod.getStartDate())) {
						if (gCalendarDate.isBetween(minNewStartDate, usefulPeriod.getStartDate())) {
							MTLog.log("new useful period start date '%s' because it's close to previous useful period start date '%s'.", gCalendarDate.getDate(), usefulPeriod.getStartDate());
							usefulPeriod.setStartDate(gCalendarDate.getDate());
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
			do {
				newDateFound = false;
				int minNewEndDate = incDateDays(DATE_FORMAT, c, usefulPeriod.getEndDate(), +1);
				for (GCalendarDate gCalendarDate : gCalendarDates) {
					if (gCalendarDate.isAfter(usefulPeriod.getEndDate())) {
						if (gCalendarDate.isBetween(usefulPeriod.getEndDate(), minNewEndDate)) {
							MTLog.log("new useful end date '%s' because it's close to previous useful period end data '%s'.", gCalendarDate.getDate(), usefulPeriod.getEndDate());
							usefulPeriod.setEndDate(gCalendarDate.getDate());
							newDateFound = true;
							break;
						}
					}
				}
			} while (newDateFound);
		}
	}

	static void parseCalendarDates(List<GCalendarDate> gCalendarDates,
								   Calendar c,
								   Period p,
								   boolean lookBackward) {
		HashSet<Integer> todayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, p, lookBackward, 1, true);
		if (todayServiceIds.isEmpty()) {
			MTLog.log("NO schedule available for %s in calendar dates.", p.getTodayStringInt());
			return;
		}
		refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
		while (true) {
			MTLog.log("> Schedules from %s to %s... ", p.getStartDate(), p.getEndDate());
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isBetween(p.getStartDate(), p.getEndDate())) {
					if (!gCalendarDate.isServiceIdInts(todayServiceIds)) {
						//noinspection DiscouragedApi
						MTLog.log("> new service ID from calendar date active on %s between %s and %s: '%s'", gCalendarDate.getDate(), p.getStartDate(), p.getEndDate(), gCalendarDate.getServiceId());
						todayServiceIds.add(gCalendarDate.getServiceIdInt());
					}
				}
			}
			boolean newDates = refreshStartEndDatesFromCalendarDates(p, todayServiceIds, gCalendarDates);
			if (newDates) {
				MTLog.log("> new start date '%s' & end date '%s' from calendar date active during service ID(s).", p.getStartDate(), p.getEndDate());
				continue;
			}
			Period pNext = new Period();
			pNext.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getEndDate(), 1));
			HashSet<Integer> nextDayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, pNext, false, 0, false);
			refreshStartEndDatesFromCalendarDates(pNext, nextDayServiceIds, gCalendarDates);
			if (pNext.getStartDate() != null && pNext.getEndDate() != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.getStartDate(), pNext.getEndDate(), MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.setEndDate(pNext.getEndDate());
				MTLog.log("> new end date '%s' because next %s day has own service ID(s)", p.getEndDate(), MIN_PREVIOUS_NEXT_ADDED_DAYS);
				continue;
			}
			Period pPrevious = new Period();
			pPrevious.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getStartDate(), -1));
			HashSet<Integer> previousDayServiceIds = findCalendarDatesTodayServiceIds(gCalendarDates, c, pPrevious, false, 0, false);
			refreshStartEndDatesFromCalendarDates(pPrevious, previousDayServiceIds, gCalendarDates);
			if (lookBackward // NOT next schedule, only current schedule can look behind
					&& pPrevious.getStartDate() != null && pPrevious.getEndDate() != null
					&& diffLowerThan(DATE_FORMAT, c, pPrevious.getStartDate(), pPrevious.getEndDate(), MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.setStartDate(pPrevious.getStartDate());
				MTLog.log("> new start date '%s' because previous %s day has own service ID(s)", p.getStartDate(), MIN_PREVIOUS_NEXT_ADDED_DAYS);
				continue;
			}
			if (diffLowerThan(DATE_FORMAT, c, p.getStartDate(), p.getEndDate(), MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS)) {
				long currentPeriodCoverageInMs = p.getStartDate() == null || p.getEndDate() == null ? 0L :
						diffInMs(DATE_FORMAT, c,
								p.getStartDate(), // morning
								p.getEndDate() + 1 // midnight ≈ tomorrow
						);
				long nextPeriodCoverageInMs = pNext.getStartDate() == null || pNext.getEndDate() == null ? 0L :
						diffInMs(DATE_FORMAT, c,
								pNext.getStartDate(), // morning
								pNext.getEndDate() + 1 // midnight ≈ tomorrow
						);
				long previousPeriodCoverageInMs = pPrevious.getStartDate() == null || pPrevious.getEndDate() == null ? 0L :
						diffInMs(DATE_FORMAT, c,
								pPrevious.getStartDate(), // morning
								pPrevious.getEndDate() + 1 // midnight ≈ tomorrow
						);
				long previousToCurrent = previousPeriodCoverageInMs / currentPeriodCoverageInMs;
				long nextToCurrent = nextPeriodCoverageInMs / currentPeriodCoverageInMs;
				if (lookBackward // NOT next schedule, only current schedule can look behind
						&& previousPeriodCoverageInMs > 0L
						&& (nextPeriodCoverageInMs <= 0L || previousPeriodCoverageInMs < nextPeriodCoverageInMs)
						&& previousToCurrent < MAX_CALENDAR_DATE_COVERAGE_RATIO) {
					p.setStartDate(incDateDays(DATE_FORMAT, c, p.getStartDate(), -1)); // start--
					MTLog.log("new start date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.getStartDate());
				} else if (TimeUnit.MILLISECONDS.toDays(currentPeriodCoverageInMs) < MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS
						|| nextToCurrent < MAX_CALENDAR_DATE_COVERAGE_RATIO) {
					p.setEndDate(incDateDays(DATE_FORMAT, c, p.getEndDate(), 1)); // end++
					MTLog.log("new end date because coverage lower than %s days: %s", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, p.getEndDate());
				} else {
					MTLog.log("coverage lower than %s days but would add too many days (p: %sx, n: %sx)", MIN_CALENDAR_DATE_COVERAGE_TOTAL_IN_DAYS, nextToCurrent, previousToCurrent);
					break;
				}
				continue;
			}
			break;
		}
	}

	private static boolean refreshStartEndDatesFromCalendarDates(
			Period p,
			HashSet<Integer> serviceIds,
			List<GCalendarDate> gCalendarDates
	) {
		boolean newDates = false;
		for (GCalendarDate gCalendarDate : gCalendarDates) {
			if (gCalendarDate.isServiceIdInts(serviceIds)) {
				if ((p.getStartDate() == null || gCalendarDate.isBefore(p.getStartDate()))
				) {
					p.setStartDate(gCalendarDate.getDate());
					newDates = true;
				}
				if ((p.getEndDate() == null || gCalendarDate.isAfter(p.getEndDate()))
				) {
					p.setEndDate(gCalendarDate.getDate());
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
																	 boolean lookBackward,
																	 int minTodayServiceIdsSize,
																	 boolean canIncDays) {
		HashSet<Integer> todayServiceIds = new HashSet<>();
		final Integer initialTodayStringInt = p.getTodayStringInt();
		while (true) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (gCalendarDate.isDate(p.getTodayStringInt())) {
					if (!gCalendarDate.isServiceIdInts(todayServiceIds)) {
						todayServiceIds.add(gCalendarDate.getServiceIdInt());
					}
				}
			}
			if (canIncDays) {
				if (!lookBackward && todayServiceIds.size() < minTodayServiceIdsSize //
						&& diffLowerThan(DATE_FORMAT, c, initialTodayStringInt, p.getTodayStringInt(), MAX_LOOK_FORWARD_IN_DAYS)) {
					p.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getTodayStringInt(), 1));
					MTLog.log("new today in the future because not enough service today: %s (initial today: %s)", p.getTodayStringInt(), initialTodayStringInt);
					continue;
				} else if (lookBackward && todayServiceIds.size() < minTodayServiceIdsSize //
						&& diffLowerThan(DATE_FORMAT, c, p.getTodayStringInt(), initialTodayStringInt, MAX_LOOK_BACKWARD_IN_DAYS)) {
					p.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getTodayStringInt(), -1));
					MTLog.log("new today in the past because not enough service today: %s (initial today: %s)", p.getTodayStringInt(), initialTodayStringInt);
					continue;
				}
			}
			break;
		}
		return todayServiceIds;
	}

	static void parseCalendars(@NotNull List<GCalendar> gCalendars, @Nullable List<GCalendarDate> gCalendarDates, SimpleDateFormat DATE_FORMAT, Calendar c, Period p, boolean lookBackward) {
		findCalendarsTodayPeriod(gCalendars, gCalendarDates, p, lookBackward);
		if (p.getStartDate() == null || p.getEndDate() == null) {
			MTLog.log("[parse-calendars] > NO schedule available for %s in calendars. (start:%s|end:%s)", p.getTodayStringInt(), p.getStartDate(), p.getEndDate());
			return;
		}
		boolean newDates;
		while (true) {
			MTLog.log("[parse-calendars] > Schedules from '%s' to '%s'... ", p.getStartDate(), p.getEndDate());
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.isOverlapping(p.getStartDate(), p.getEndDate())) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection DiscouragedApi
					MTLog.logDebug("[parse-calendars] > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
				if (p.getStartDate() == null || gCalendar.startsBefore(p.getStartDate())) {
					//noinspection DiscouragedApi
					MTLog.log("[parse-calendars] > (today: %s) new start date '%s' from calendar (service:'%s'|start:%s|end:%s) active (was: %s)", p.getTodayStringInt(), gCalendar.getStartDate(), gCalendar.getServiceId(), gCalendar.getStartDate(), gCalendar.getEndDate(), p.getStartDate());
					p.setStartDate(gCalendar.getStartDate());
					newDates = true;
				}
				if (p.getEndDate() == null || gCalendar.endsAfter(p.getEndDate())) {
					//noinspection DiscouragedApi
					MTLog.log("[parse-calendars] > (today: %s) new end date '%s' from calendar (service:'%s'|start:%s|end:%s) active (was: %s)", p.getTodayStringInt(), gCalendar.getEndDate(), gCalendar.getServiceId(), gCalendar.getStartDate(), gCalendar.getEndDate(), p.getEndDate());
					p.setEndDate(gCalendar.getEndDate());
					newDates = true;
				}
			}
			if (newDates) {
				continue;
			}
			final Period pNext = new Period();
			pNext.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getEndDate(), 1));
			findDayServiceIdsPeriod(gCalendars, gCalendarDates, pNext);
			if (pNext.getStartDate() != null && pNext.getEndDate() != null
					&& diffLowerThan(DATE_FORMAT, c, pNext.getStartDate(), pNext.getEndDate(), MIN_PREVIOUS_NEXT_ADDED_DAYS)) {
				p.setEndDate(pNext.getEndDate());
				MTLog.log("[parse-calendars] > new end date '%s' because next day has own service ID(s)", p.getEndDate());
				continue;
			} else if (pNext.getStartDate() != null && pNext.getEndDate() != null) {
				MTLog.logDebug("[parse-calendars] > ignore next period because coverage '%s' not < %s days: %s",
						MTLog.formatDuration(diffInMs(DATE_FORMAT, c, pNext.getStartDate(), pNext.getEndDate())),
						MIN_PREVIOUS_NEXT_ADDED_DAYS, pNext.getTodayStringInt());
			}
			if (diffLowerThan(DATE_FORMAT, c, p.getStartDate(), p.getEndDate(), MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS)) {
				if (lookBackward) { // NOT next schedule, only current schedule can look behind
					final Period pPrevious = new Period();
					pPrevious.setTodayStringInt(incDateDays(DATE_FORMAT, c, p.getStartDate(), -1));
					findDayServiceIdsPeriod(gCalendars, gCalendarDates, pPrevious);
					long nextPeriodCoverageInMs = pNext.getStartDate() == null || pNext.getEndDate() == null ? 0L : diffInMs(DATE_FORMAT, c, pNext.getStartDate(), pNext.getEndDate());
					long previousPeriodCoverageInMs = pPrevious.getStartDate() == null || pPrevious.getEndDate() == null ? 0L : diffInMs(DATE_FORMAT, c, pPrevious.getStartDate(), pPrevious.getEndDate());
					if (previousPeriodCoverageInMs > 0L && previousPeriodCoverageInMs < nextPeriodCoverageInMs) {
						p.setStartDate(incDateDays(DATE_FORMAT, c, p.getStartDate(), -1)); // start--
						MTLog.log("[parse-calendars] > new start date because coverage lower than %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.getStartDate());
						continue;
					}
				}
				p.setEndDate(incDateDays(DATE_FORMAT, c, p.getEndDate(), 1)); // end++
				MTLog.log("[parse-calendars] > new end date because coverage not < %s days: %s", MIN_CALENDAR_COVERAGE_TOTAL_IN_DAYS, p.getEndDate());
				continue;
			}
			MTLog.logDebug("[parse-calendars] > stop here with good coverage from '%s' to '%s' (diff: %s)", p.getStartDate(), p.getEndDate(), MTLog.formatDuration(diffInMs(DATE_FORMAT, c, p.getStartDate(), p.getEndDate())));
			break;
		}
	}

	static void findCalendarsTodayPeriod(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates, Period p, boolean lookBackward) {
		final Integer initialTodayStringInt = p.getTodayStringInt();
		final Period entirePeriod = getEntirePeriodMinMaxDate(gCalendars, gCalendarDates);
		while (true) {
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.containsDate(p.getTodayStringInt())) {
					// //noinspection DiscouragedApi
					// MTLog.logDebug("[find-today-period] > (today:%s) SKIP outside service ID '%s' from calendar date active between '%s' and '%s'", p.getTodayStringInt(), gCalendar.getServiceId(), gCalendar.getStartDate(), gCalendar.getEndDate());
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection DiscouragedApi
					// MTLog.logDebug("[find-today-period] > (today:%s) SKIP removed service ID '%s' from calendar date active between '%s' and '%s'", p.getTodayStringInt(), gCalendar.getServiceId(), gCalendar.getStartDate(), gCalendar.getEndDate());
					continue;
				}
				if (p.getStartDate() == null || gCalendar.startsBefore(p.getStartDate())) {
					//noinspection DiscouragedApi
					MTLog.log("[find-today-period] > (today:%s) new start date '%s' from active calendar (service:'%s') (was: %s)", p.getTodayStringInt(), gCalendar.getStartDate(), gCalendar.getServiceId(), p.getStartDate());
					p.setStartDate(gCalendar.getStartDate());
				}
				if (p.getEndDate() == null || gCalendar.endsAfter(p.getEndDate())) {
					//noinspection DiscouragedApi
					MTLog.log("[find-today-period] > (today:%s) new end date '%s' from active calendar (service:'%s') (was: %s)", p.getTodayStringInt(), gCalendar.getEndDate(), gCalendar.getServiceId(), p.getEndDate());
					p.setEndDate(gCalendar.getEndDate());
				}
			}
			if (p.getStartDate() == null || p.getEndDate() == null) {
				if (lookBackward) { // #CURRENT
					if (entirePeriod.getEndDate() != null && p.getTodayStringInt() != null
							&& entirePeriod.getEndDate() < p.getTodayStringInt()) {
						p.setTodayStringInt(entirePeriod.getEndDate());
						MTLog.log("[find-today-period] > earlier today because no service: %s (initial today: %s)", p.getTodayStringInt(), initialTodayStringInt);
						continue;
					}
				} else { // #NEXT
					if (entirePeriod.getStartDate() != null && p.getTodayStringInt() != null
							&& entirePeriod.getStartDate() > p.getTodayStringInt()) {
						p.setTodayStringInt(entirePeriod.getStartDate());
						MTLog.log("[find-today-period] > latter today because no service: %s (initial today: %s)", p.getTodayStringInt(), initialTodayStringInt);
						continue;
					}
				}
			}
			break;
		}
	}

	static void findDayServiceIdsPeriod(List<GCalendar> gCalendars, @Nullable List<GCalendarDate> gCalendarDates, Period p) {
		final Set<Integer> serviceIdInts = new HashSet<>();
		boolean newDates;
		while (true) {
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.containsDate(p.getTodayStringInt())) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection DiscouragedApi
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
				if (p.getStartDate() == null || gCalendar.startsBefore(p.getStartDate())) {
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > new start date from calendar active on %s: %s (was: %s)", p.getTodayStringInt(), gCalendar.getStartDate(), p.getStartDate());
					p.setStartDate(gCalendar.getStartDate());
					serviceIdInts.add(gCalendar.getServiceIdInt());
					newDates = true;
				}
				if (p.getEndDate() == null || gCalendar.endsAfter(p.getEndDate())) {
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > new end date from calendar active on %s: %s (was: %s)", p.getTodayStringInt(), gCalendar.getEndDate(), p.getEndDate());
					p.setEndDate(gCalendar.getEndDate());
					serviceIdInts.add(gCalendar.getServiceIdInt());
					newDates = true;
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
		if (p.getStartDate() == null || p.getEndDate() == null) {
			logFindDayServiceIdsPeriod("[find-day-service-id-period] > NO schedule available for %s in calendars. (start:%s|end:%s)", p.getTodayStringInt(), p.getStartDate(), p.getEndDate());
			return;
		}
		while (true) {
			logFindDayServiceIdsPeriod("[find-day-service-id-period] > Schedules from %s to %s... ", p.getStartDate(), p.getEndDate());
			newDates = false;
			for (GCalendar gCalendar : gCalendars) {
				if (!gCalendar.isOverlapping(p.getStartDate(), p.getEndDate())) {
					continue;
				}
				if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates)) {
					//noinspection DiscouragedApi
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", gCalendar.getStartDate(), gCalendar.getEndDate(), gCalendar.getServiceId());
					continue;
				}
				if (p.getStartDate() == null || gCalendar.startsBefore(p.getStartDate())) {
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > new start date from calendar active between %s and %s: %s (was: %s)", p.getStartDate(), p.getEndDate(), gCalendar.getStartDate(), p.getStartDate());
					p.setStartDate(gCalendar.getStartDate());
					serviceIdInts.add(gCalendar.getServiceIdInt());
					newDates = true;
				}
				if (p.getEndDate() == null || gCalendar.endsAfter(p.getEndDate())) {
					logFindDayServiceIdsPeriod("[find-day-service-id-period] > new end date from calendar active between %s and %s: %s (was: %s)", p.getStartDate(), p.getEndDate(), gCalendar.getEndDate(), p.getEndDate());
					p.setEndDate(gCalendar.getEndDate());
					serviceIdInts.add(gCalendar.getServiceIdInt());
					newDates = true;
				}
			}
			if (newDates) {
				continue;
			}
			break;
		}
		logFindDayServiceIdsPeriod("[find-day-service-id-period] > schedule available for '%s' in calendars. (start:%s|end:%s) service IDs:[%s].", p.getTodayStringInt(), p.getStartDate(), p.getEndDate(), GIDs.toStringPlus(serviceIdInts));
	}

	private static void logFindDayServiceIdsPeriod(@NotNull String format, @Nullable Object... args) {
		//noinspection ConstantValue
		if (true) return; // DEBUG
		MTLog.logDebug(format, args);
	}

	@NotNull
	public static HashSet<Integer> getPeriodServiceIds(
			@Nullable Integer startDate,
			@Nullable Integer endDate,
			@Nullable List<GCalendar> gCalendars,
			@Nullable List<GCalendarDate> gCalendarDates) {
		HashSet<Integer> serviceIdInts = new HashSet<>();
		if (gCalendars != null) {
			for (GCalendar gCalendar : gCalendars) {
				if (gCalendar.isInside(startDate, endDate)) {
					if (gCalendar.isServiceIdInts(serviceIdInts)) {
						continue;
					}
					if (GCalendarDate.isServiceEntirelyRemoved(gCalendar, gCalendarDates, startDate, endDate)) {
						//noinspection DiscouragedApi
						MTLog.logDebug("[period-service-ids] > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate, gCalendar.getServiceId());
						continue;
					}
					//noinspection DiscouragedApi
					MTLog.log("[period-service-ids] > new service ID from calendar active between %s and %s: %s", startDate, endDate, gCalendar.getServiceId());
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
						//noinspection DiscouragedApi
						MTLog.log("[period-service-ids] > ignored service ID from calendar date active between %s and %s: %s (SERVICE REMOVED)", startDate, endDate, gCalendarDate.getServiceId());
						continue;
					}
					//noinspection DiscouragedApi
					MTLog.log("[period-service-ids] > new service ID from calendar date active between %s and %s: %s", startDate, endDate, gCalendarDate.getServiceId());
					serviceIdInts.add(gCalendarDate.getServiceIdInt());
				}
			}
		}
		MTLog.log("[period-service-ids] > Service IDs [%d]: %s.", serviceIdInts.size(), GIDs.toStringPlus(CollectionUtils.sorted(serviceIdInts)));
		return serviceIdInts;
	}

	private static Period getEntirePeriodMinMaxDate(List<GCalendar> gCalendars, List<GCalendarDate> gCalendarDates) {
		final Period p = new Period();
		if (gCalendars != null && !gCalendars.isEmpty()) {
			for (GCalendar gCalendar : gCalendars) {
				if (p.getStartDate() == null || gCalendar.startsBefore(p.getStartDate())) {
					p.setStartDate(gCalendar.getStartDate());
				}
				if (p.getEndDate() == null || gCalendar.endsAfter(p.getEndDate())) {
					p.setEndDate(gCalendar.getEndDate());
				}
			}
		}
		if (gCalendarDates != null && !gCalendarDates.isEmpty()) {
			for (GCalendarDate gCalendarDate : gCalendarDates) {
				if (p.getStartDate() == null || gCalendarDate.isBefore(p.getStartDate())) {
					p.setStartDate(gCalendarDate.getDate());
				}
				if (p.getEndDate() == null || gCalendarDate.isAfter(p.getEndDate())) {
					p.setEndDate(gCalendarDate.getDate());
				}
			}
		}
		return p;
	}

	public static int incDateDays(@NotNull SimpleDateFormat dateFormat,
								  @NotNull Calendar calendar,
								  @Nullable Integer dateInt,
								  int numberOfDays) {
		try {
			calendar.setTime(dateFormat.parse(String.valueOf(dateInt)));
			calendar.add(Calendar.DAY_OF_MONTH, numberOfDays);
			return Integer.parseInt(dateFormat.format(calendar.getTime()));
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while increasing end date!");
		}
	}

	public static boolean diffLowerThan(@NotNull SimpleDateFormat dateFormat,
										@NotNull Calendar calendar,
										@Nullable Integer startDateInt,
										@Nullable Integer endDateInt,
										int diffInDays) {
		try {
			return diffInMs(dateFormat, calendar, startDateInt, endDateInt) < TimeUnit.DAYS.toMillis(diffInDays);
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "Error while checking date difference!");
		}
	}

	private static long diffInMs(@NotNull SimpleDateFormat dateFormat,
								 @NotNull Calendar calendar,
								 @Nullable Integer startDateInt,
								 @Nullable Integer endDateInt) {
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
			if (gCalendarDate.getExceptionType() != GCalendarDatesExceptionType.SERVICE_REMOVED) {
				if (gCalendarDate.isBefore(usefulPeriod.getStartDate()) //
						|| gCalendarDate.isAfter(usefulPeriod.getEndDate())) {
					MTLog.log("Exclude calendar date \"%s\" because it's out of the useful period (start:%s|end:%s).", gCalendarDate,
							usefulPeriod.getStartDate(), usefulPeriod.getEndDate());
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
