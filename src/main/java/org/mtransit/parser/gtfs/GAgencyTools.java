package org.mtransit.parser.gtfs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.Pair;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionCardinalType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MDirection;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("unused")
public interface GAgencyTools {

	boolean EXCLUDE = true;
	boolean KEEP = false;

	List<Locale> LANG_EN = Collections.singletonList(Locale.ENGLISH);
	List<Locale> LANG_FR = Collections.singletonList(Locale.FRENCH);
	List<Locale> LANG_EN_FR = Arrays.asList(Locale.ENGLISH, Locale.FRENCH);
	List<Locale> LANG_FR_EN = Arrays.asList(Locale.FRENCH, Locale.ENGLISH);

	int getThreadPoolSize();

	@Deprecated // TO BE REMOVED
	boolean defaultExcludeEnabled();

	boolean defaultStringsCleanerEnabled();

	boolean excludingAll();

	void addSupportedLanguage(@Nullable String supportedLanguage);

	/**
	 * @return sorted supported languages (1st = primary language)
	 */
	@Nullable
	List<Locale> getSupportedLanguages();

	@Deprecated // TO BE REMOVED
	void setAgencyName(@Nullable String agencyName);

	@Deprecated // TO BE REMOVED
	@NotNull
	String getAgencyName();

	@NotNull
	String getAgencyColor();

	boolean defaultAgencyColorEnabled();

	@Nullable
	String fixColor(@Nullable String color);

	@NotNull
	String getAgencyColor(@NotNull GAgency gAgency, @NotNull GSpec gSpec);

	@Nullable
	String getAgencyId();

	@NotNull
	Integer getAgencyRouteType();

	@Nullable
	Integer getAgencyExtendedRouteType();

	@NotNull
	Integer getOriginalAgencyRouteType();

	boolean excludeAgencyNullable(@Nullable GAgency gAgency);

	boolean excludeAgency(@NotNull GAgency gAgency);

	@NotNull
	String cleanServiceId(@NotNull String serviceIdString); // currently only used in "mt.data.M..." classes before exporting to file

	@Nullable
	String getServiceIdCleanupRegex();

	boolean verifyServiceIdsUniqueness();

	// ROUTE
	@NotNull
	String cleanRouteOriginalId(@NotNull String routeId);

	long getRouteId(@NotNull GRoute gRoute);

	@Nullable
	Long convertRouteIdFromShortNameNotSupported(@NotNull String routeShortName);

	@Nullable
	Long convertRouteIdNextChars(@NotNull String nextChars);

	@Nullable
	Long convertRouteIdPreviousChars(@NotNull String previousChars);

	boolean defaultRouteIdEnabled();

	boolean useRouteShortNameForRouteId();

	boolean useRouteIdForRouteShortName();

	@Nullable
	String getRouteIdCleanupRegex();

	boolean verifyRouteIdsUniqueness();

	@NotNull
	String getRouteShortName(@NotNull GRoute gRoute);

	@NotNull
	String cleanRouteShortName(@NotNull String routeShortName);

	@NotNull
	String provideMissingRouteShortName(@NotNull GRoute gRoute);

	boolean defaultRouteLongNameEnabled();

	boolean tryRouteDescForMissingLongName();

	@NotNull
	String getRouteLongName(@NotNull GRoute gRoute);

	@NotNull
	String cleanRouteLongName(@NotNull String routeLongName);

	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	boolean allowGTFSIdOverride();

	boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge);

	@Nullable
	String getRouteColor(@NotNull GRoute gRoute);

	@Nullable
	String getRouteColor(@NotNull GRoute gRoute, @NotNull MAgency agency);

	@Nullable
	String provideMissingRouteColor(@NotNull GRoute gRoute);

	boolean excludeRouteNullable(@Nullable GRoute gRoute);

	boolean excludeRoute(@NotNull GRoute gRoute);

	// TRIP
	void setDirectionHeadsign(@NotNull MRoute mRoute, @NotNull MDirection mDirection, @NotNull GTrip gTrip, @NotNull GSpec gtfs);

	@NotNull
	String cleanTripHeadsign(@NotNull String tripHeadsign);

	@Nullable
	String getTripIdCleanupRegex();

	boolean verifyTripIdsUniqueness();

	@NotNull String cleanTripOriginalId(@NotNull String gTripId);

	boolean directionSplitterEnabled(long routeId);

	boolean directionOverrideId(long routeId);

	@NotNull
	String cleanDirectionHeadsign(int directionId, boolean fromStopName, @NotNull String directionHeadSign);

	@Deprecated
	@NotNull
	String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign);

	boolean directionFinderEnabled();

	boolean directionFinderEnabled(long routeId, @NotNull GRoute gRoute);

	boolean directionHeadSignsDescriptive(@NotNull Map<Integer, String> directionHeadSigns);

	boolean allowDuplicateKeyError();

	boolean allowNonDescriptiveHeadSigns(long routeId);

	@Nullable
	String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2);

	@Nullable
	String mergeComplexDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2);

	boolean directionHeadSignDescriptive(@NotNull String directionHeadSign);

	@Deprecated
	int getDirectionType();

	@NotNull
	List<Integer> getDirectionTypes();

	@Nullable
	MDirectionCardinalType convertDirection(@Nullable String headSign);

	@Deprecated
	boolean mergeHeadsign(@NotNull MDirection mDirection, @NotNull MDirection mDirectionToMerge);

	boolean mergeHeadsign(@NotNull MDirection mDirection, @NotNull MDirection mDirectionToMerge, @NotNull GRoute gRoute);

	boolean excludeTripNullable(@Nullable GTrip gTrip);

	boolean excludeTrip(@NotNull GTrip gTrip);

	@NotNull
	String provideMissingTripHeadSign(@NotNull GTrip gTrip);

	void setStopTimesHasPickupTypeNotRegular(boolean notRegular);

	boolean stopTimesHasPickupTypeNotRegular();

	void setStopTimesHasDropOffTypeNotRegular(boolean notRegular);

	boolean stopTimesHasDropOffTypeNotRegular();

	@SuppressWarnings("DeprecatedIsStillUsed") // TODO migrate agencies parser
	@Deprecated
	void setForceStopTimeFirstNoDropOffLastNoPickupType(boolean force);

	@SuppressWarnings("DeprecatedIsStillUsed") // TODO migrate agencies parser
	@Deprecated
	boolean forceStopTimeFirstNoDropOffLastNoPickupType();

	void setForceStopTimeLastNoPickupType(boolean force);

	boolean forceStopTimeLastNoPickupType();

	void setForceStopTimeFirstNoDropOffType(boolean force);

	boolean forceStopTimeFirstNoDropOffType();

	boolean excludeStopTime(@NotNull GStopTime gStopTime);

	// STOP
	int getStopId(@NotNull GStop gStop);

	@Nullable
	Integer convertStopIdFromCodeNotSupported(@NotNull String stopCode);

	boolean useStopCodeForStopId();

	@Nullable
	String getStopIdCleanupRegex();

	boolean verifyStopIdsUniqueness();

	@NotNull
	String cleanStopName(@NotNull String gStopName);

	@NotNull
	String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign);

	@NotNull
	String cleanStopHeadSign(@NotNull String stopHeadsign);

	@NotNull
	String getStopCode(@NotNull GStop gStop);

	@Deprecated
	@Nullable
	String getStopOriginalId(@NotNull GStop gStop);

	boolean excludeStopNullable(@Nullable GStop gStop);

	boolean excludeStop(@NotNull GStop gStop);

	@NotNull
	String cleanStopOriginalId(@NotNull String gStopIdString);

	// CALENDAR
	boolean excludeCalendar(@NotNull GCalendar gCalendar);

	// CALENDAR DATE
	boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDate);

	// SCHEDULE
	@NotNull
	Pair<Integer, Integer> getTimes(@NotNull GStopTime gStopTime,
									@NotNull List<GStopTime> tripStopTimes,
									@NotNull SimpleDateFormat timeFormat);

	// FREQUENCY
	int getStartTime(@NotNull GFrequency gFrequency);

	int getEndTime(@NotNull GFrequency gFrequency);
}
