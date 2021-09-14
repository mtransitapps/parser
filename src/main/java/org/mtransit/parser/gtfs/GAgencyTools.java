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
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"RedundantSuppression", "unused"})
public interface GAgencyTools {

	boolean EXCLUDE = true;
	boolean KEEP = false;

	int getThreadPoolSize();

	boolean defaultExcludeEnabled();

	boolean excludingAll();

	@NotNull
	String getAgencyName();

	@NotNull
	String getAgencyColor();

	boolean defaultAgencyColorEnabled();

	@Nullable
	String fixColor(@Nullable String color);

	@NotNull
	String getAgencyColor(@NotNull GAgency gAgency, @NotNull GSpec gSpec);

	@NotNull
	Integer getAgencyRouteType();

	boolean excludeAgencyNullable(@Nullable GAgency gAgency);

	boolean excludeAgency(@NotNull GAgency gAgency);

	@NotNull
	String cleanServiceId(@NotNull String serviceIdString);

	// ROUTE
	long getRouteId(@NotNull GRoute gRoute);

	@Nullable
	String getRouteShortName(@NotNull GRoute gRoute);

	@NotNull
	String getRouteLongName(@NotNull GRoute gRoute);

	@NotNull
	String cleanRouteLongName(@NotNull String routeLongName);

	boolean mergeRouteLongName(@NotNull MRoute mRoute, @NotNull MRoute mRouteToMerge);

	@Nullable
	String getRouteColor(@NotNull GRoute gRoute);

	@Nullable
	String getRouteColor(@NotNull GRoute gRoute, @NotNull MAgency agency);

	boolean excludeRouteNullable(@Nullable GRoute gRoute);

	boolean excludeRoute(@NotNull GRoute gRoute);

	// TRIP
	void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs);

	@NotNull
	String cleanTripHeadsign(@NotNull String tripHeadsign);

	boolean directionSplitterEnabled();

	boolean directionSplitterEnabled(long routeId);

	@NotNull
	String cleanDirectionHeadsign(boolean fromStopName, @NotNull String directionHeadSign);

	boolean directionFinderEnabled();

	boolean directionFinderEnabled(long routeId, @NotNull GRoute gRoute);

	boolean directionHeadSignsDescriptive(@NotNull Map<Integer, String> directionHeadSigns);

	boolean allowNonDescriptiveHeadSigns(long routeId);

	@Nullable
	String selectDirectionHeadSign(@Nullable String headSign1, @Nullable String headSign2);

	boolean directionHeadSignDescriptive(@NotNull String directionHeadSign);

	@Deprecated
	int getDirectionType();

	@NotNull
	List<Integer> getDirectionTypes();

	@Nullable
	MDirectionType convertDirection(@Nullable String headSign);

	boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge);

	boolean excludeTripNullable(@Nullable GTrip gTrip);

	boolean excludeTrip(@NotNull GTrip gTrip);

	@NotNull
	String provideMissingTripHeadSign(@NotNull GTrip gTrip);

	boolean excludeStopTime(@NotNull GStopTime gStopTime);

	// STOP
	int getStopId(@NotNull GStop gStop);

	@NotNull
	String cleanStopName(@NotNull String gStopName);

	@NotNull
	String cleanStopHeadSign(@NotNull GRoute gRoute, @NotNull GTrip gTrip, @NotNull GStopTime gStopTime, @NotNull String stopHeadsign);

	@NotNull
	String cleanStopHeadSign(@NotNull String stopHeadsign);

	@NotNull
	String getStopCode(@NotNull GStop gStop);

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
									@NotNull SimpleDateFormat mDateFormat);

	// FREQUENCY
	int getStartTime(@NotNull GFrequency gFrequency);

	int getEndTime(@NotNull GFrequency gFrequency);
}
