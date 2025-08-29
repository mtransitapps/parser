package org.mtransit.parser.mt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.CollectionUtils;
import org.mtransit.commons.FeatureFlags;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.db.GTFSDataBase;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFieldTypes;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GIDs;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MCalendarExceptionType;
import org.mtransit.parser.mt.data.MDirectionCardinalType;
import org.mtransit.parser.mt.data.MFrequency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MDirection;
import org.mtransit.parser.mt.data.MDirectionStop;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.Callable;

public class GenerateMObjectsTask implements Callable<MSpec> {

	@NotNull
	private final GAgencyTools agencyTools;
	private final long routeId;
	@NotNull
	private final GSpec globalGTFS;
	@NotNull
	private final Map<Integer, List<GStopTime>> routeGTripIdIntIdGStopTimes = new HashMap<>();
	@NotNull
	private final Map<Integer, List<GTripStop>> routeGTripIdIntGTripStops = new HashMap<>();

	GenerateMObjectsTask(long routeId, @NotNull GAgencyTools agencyTools, @NotNull GSpec gtfs) {
		this.routeId = routeId;
		this.agencyTools = agencyTools;
		this.globalGTFS = gtfs;
	}

	@NotNull
	@Override
	public MSpec call() {
		try {
			return doCall();
		} catch (Exception e) {
			throw new MTLog.Fatal(e, "%s: Error while parsing route!", this.routeId);
		}
	}

	@NotNull
	public List<GStopTime> getTripGStopTimes(int gTripIdInt) {
		return this.routeGTripIdIntIdGStopTimes.getOrDefault(gTripIdInt, Collections.emptyList());
	}

	private MSpec doCall() {
		long startAt = System.currentTimeMillis();
		MTLog.log("%s: processing... ", this.routeId);
		this.globalGTFS.add(this.routeId, this);
		HashMap<Integer, MAgency> mAgencies = new HashMap<>();
		HashSet<MServiceDate> mServiceDates = new HashSet<>();
		HashMap<String, MSchedule> mSchedules = new HashMap<>();
		HashMap<String, MFrequency> mFrequencies = new HashMap<>();
		HashMap<Long, MRoute> mRoutes = new HashMap<>();
		HashMap<Long, MDirection> mDirections = new HashMap<>();
		HashMap<String, MDirectionStop> allMDirectionStops = new HashMap<>();
		HashMap<Integer, MStop> mStops = new HashMap<>();
		HashSet<Integer> directionStopIds = new HashSet<>(); // the list of stop IDs used by directions
		HashSet<Integer> serviceIdInts = new HashSet<>();
		final GSpec routeGTFS = this.globalGTFS.getRouteGTFS(this.routeId);
		List<Integer> routeGTripsIntIds = new ArrayList<>();
		for (GRoute gRoute : routeGTFS.getRoutes(this.routeId)) {
			List<GTrip> routeGTrips = routeGTFS.getRouteTrips(gRoute.getRouteIdInt());
			for (GTrip gTrip : routeGTrips) {
				routeGTripsIntIds.add(gTrip.getTripIdInt());
			}
		}
		for (GStopTime gStopTime : GStopTime.from(GTFSDataBase.selectStopTimes(GIDs.getStrings(routeGTripsIntIds), null, null))) {
			List<GStopTime> gTripIdIntGStopTimes = routeGTripIdIntIdGStopTimes.get(gStopTime.getTripIdInt());
			if (gTripIdIntGStopTimes == null) {
				gTripIdIntGStopTimes = new ArrayList<>();
			}
			gTripIdIntGStopTimes.add(gStopTime);
			routeGTripIdIntIdGStopTimes.put(gStopTime.getTripIdInt(), gTripIdIntGStopTimes);
		}
		for (GTripStop gTripStop : DBUtils.selectTripStops(null, routeGTripsIntIds, null, null)) {
			List<GTripStop> gTripIdIntGTripStops = routeGTripIdIntGTripStops.get(gTripStop.getTripIdInt());
			if (gTripIdIntGTripStops == null) {
				gTripIdIntGTripStops = new ArrayList<>();
			}
			gTripIdIntGTripStops.add(gTripStop);
			routeGTripIdIntGTripStops.put(gTripStop.getTripIdInt(), gTripIdIntGTripStops);
		}
		MAgency mAgency;
		for (GAgency gAgency : routeGTFS.getAllAgencies()) {
			mAgency = new MAgency(gAgency, this.agencyTools, routeGTFS);
			final MAgency agencyWithSameID = mAgencies.get(mAgency.getIdInt());
			if (agencyWithSameID != null && !agencyWithSameID.equals(mAgency)) {
				MTLog.log("%s: Agency %s already in list!", this.routeId, mAgency.toStringPlus());
				MTLog.log("%s: %s", this.routeId, mAgency.toString());
				throw new MTLog.Fatal("%s: %s", this.routeId, agencyWithSameID.toString());
			}
			mAgencies.put(mAgency.getIdInt(), mAgency);
		}
		parseRDS(
				mSchedules,
				mFrequencies,
				mAgencies,
				mRoutes,
				mDirections,
				mStops,
				allMDirectionStops,
				directionStopIds,
				serviceIdInts,
				routeGTFS
		);
		final HashSet<Long> gCalendarDateServiceRemoved = new HashSet<>();
		for (GCalendarDate gCalendarDate : routeGTFS.getAllCalendarDates()) {
			if (!serviceIdInts.contains(gCalendarDate.getServiceIdInt())) {
				continue;
			}
			switch (gCalendarDate.getExceptionType()) {
			case SERVICE_REMOVED: // keep list of removed service for calendars processing
				if (FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) {
					mServiceDates.add(new MServiceDate(
							gCalendarDate.getServiceIdInt(),
							gCalendarDate.getDate(),
							MCalendarExceptionType.REMOVED
					));
				} else {
					gCalendarDateServiceRemoved.add(gCalendarDate.getUID());
				}
				break;
			case SERVICE_ADDED:
				mServiceDates.add(new MServiceDate(
						gCalendarDate.getServiceIdInt(),
						gCalendarDate.getDate(),
						FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE ? MCalendarExceptionType.ADDED : MCalendarExceptionType.DEFAULT
				));
				break;
			case SERVICE_DEFAULT:
				mServiceDates.add(new MServiceDate(
						gCalendarDate.getServiceIdInt(),
						gCalendarDate.getDate(),
						MCalendarExceptionType.DEFAULT
				));
				break;
			default:
				throw new MTLog.Fatal("%s: Unexpected calendar date exception type '%s'!", this.routeId, gCalendarDate.getExceptionType());
			}
		}
		if (!GSpec.ALL_CALENDARS_IN_CALENDAR_DATES) {
			//noinspection deprecation
			for (GCalendar gCalendar : routeGTFS.getAllCalendars()) {
				if (!serviceIdInts.contains(gCalendar.getServiceIdInt())) {
					continue;
				}
				for (GCalendarDate gCalendarDate : gCalendar.getDates()) {
					if (!FeatureFlags.F_EXPORT_SERVICE_EXCEPTION_TYPE) {
						if (gCalendarDateServiceRemoved.contains(gCalendarDate.getUID())) {
							continue; // service REMOVED at this date
						}
					}
					mServiceDates.add(new MServiceDate(
							gCalendarDate.getServiceIdInt(),
							gCalendarDate.getDate(),
							MCalendarExceptionType.DEFAULT
					));
				}
			}
		}
		MDirection mDirection;
		for (MSchedule mSchedule : mSchedules.values()) {
			mDirection = mDirections.get(mSchedule.getDirectionId());
			if (mDirection.getHeadsignType() == mSchedule.getHeadsignType() //
					&& StringUtils.equals(mDirection.getHeadsignValue(), mSchedule.getHeadsignValue())) {
				mSchedule.clearHeadsign();
			}
		}
		ArrayList<MAgency> mAgenciesList = new ArrayList<>(mAgencies.values());
		Collections.sort(mAgenciesList);
		ArrayList<MStop> mStopsList = new ArrayList<>(mStops.values());
		Collections.sort(mStopsList);
		ArrayList<MRoute> mRoutesList = new ArrayList<>(mRoutes.values());
		Collections.sort(mRoutesList);
		ArrayList<MDirection> mDirectionsList = new ArrayList<>(mDirections.values());
		Collections.sort(mDirectionsList);
		ArrayList<MDirectionStop> mDirectionStopsList = new ArrayList<>(allMDirectionStops.values());
		Collections.sort(mDirectionStopsList);
		setDirectionStopNoPickup(mDirectionStopsList, mSchedules.values());
		ArrayList<MServiceDate> mServiceDatesList = new ArrayList<>(mServiceDates);
		Collections.sort(mServiceDatesList);
		ArrayList<MFrequency> mFrequenciesList = new ArrayList<>(mFrequencies.values());
		Collections.sort(mFrequenciesList);
		TreeMap<Long, List<MFrequency>> mRouteFrequencies = new TreeMap<>();
		if (!mFrequenciesList.isEmpty()) {
			mRouteFrequencies.put(this.routeId, mFrequenciesList);
		}
		long firstTimestamp = -1L;
		long lastTimestamp = -1L;
		if (!mServiceDatesList.isEmpty()) {
			MServiceDate firstServiceDate = mServiceDatesList.get(0);
			final int maxCalendarDate = GFieldTypes.fromDateToInt(new Date()) + 10 * 10_000; // max 10 years IN THE FUTURE
			mServiceDatesList.removeIf(serviceDate ->
					serviceDate.getCalendarDate() > maxCalendarDate
			);
			MServiceDate lastServiceDate = mServiceDatesList.get(mServiceDatesList.size() - 1);
			int firstCalendarDate = firstServiceDate.getCalendarDate();
			int lastCalendarDate = lastServiceDate.getCalendarDate();
			int firstDeparture = -1;
			int lastDeparture = -1;
			MSchedule firstSchedule = null;
			MSchedule lastSchedule = null;
			for (MSchedule mSchedule : mSchedules.values()) {
				if (mSchedule.getServiceIdInt() == firstServiceDate.getServiceIdInt()) {
					if (firstSchedule == null
							|| mSchedule.getDeparture() < firstSchedule.getDeparture()) {
						firstSchedule = mSchedule;
					}
				}
				if (mSchedule.getServiceIdInt() == lastServiceDate.getServiceIdInt()) {
					if (lastSchedule == null
							|| lastSchedule.getDeparture() < mSchedule.getDeparture()) {
						lastSchedule = mSchedule;
					}
				}
			}
			//noinspection ConstantConditions // FIXME
			if (firstSchedule != null //
					&& (firstDeparture < 0 || firstSchedule.getDeparture() < firstDeparture)) {
				firstDeparture = firstSchedule.getDeparture();
			}
			//noinspection ConstantConditions // FIXME
			if (lastSchedule != null //
					&& (lastDeparture < 0 || lastDeparture < lastSchedule.getDeparture())) {
				lastDeparture = lastSchedule.getDeparture();
			}
			MFrequency firstFrequency = null;
			MFrequency lastFrequency = null;
			for (MFrequency mFrequency : mFrequenciesList) {
				if (mFrequency.getServiceIdInt() == firstServiceDate.getServiceIdInt()) {
					if (firstFrequency == null || mFrequency.getStartTime() < firstFrequency.getStartTime()) {
						firstFrequency = mFrequency;
					}
				}
				if (mFrequency.getServiceIdInt() == lastServiceDate.getServiceIdInt()) {
					if (lastFrequency == null || lastFrequency.getEndTime() < mFrequency.getEndTime()) {
						lastFrequency = mFrequency;
					}
				}
			}
			if (firstFrequency != null //
					&& (firstDeparture < -1 || firstFrequency.getStartTime() < firstDeparture)) {
				firstDeparture = firstFrequency.getStartTime();
			}
			if (lastFrequency != null //
					&& (lastDeparture < -1 || lastDeparture < lastFrequency.getEndTime())) {
				lastDeparture = lastFrequency.getEndTime();
			}
			final SimpleDateFormat DATE_TIME_FORMAT = GFieldTypes.makeDateAndTimeFormat();
			DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone(mAgenciesList.get(0).getTimezone()));
			try {
				firstTimestamp = GFieldTypes.toTimeStamp(DATE_TIME_FORMAT, firstCalendarDate, firstDeparture);
			} catch (Exception e) {
				throw new MTLog.Fatal(e, "Error while parsing dates '%s %s'!", firstCalendarDate, firstDeparture);
			}
			try {
				lastTimestamp = GFieldTypes.toTimeStamp(DATE_TIME_FORMAT, lastCalendarDate, lastDeparture);
			} catch (Exception e) {
				throw new MTLog.Fatal(e, "Error while parsing dates '%s %s'!", lastCalendarDate, lastDeparture);
			}
		}
		MSpec mRouteSpec = new MSpec(
				mAgenciesList,
				mStopsList,
				mRoutesList,
				mDirectionsList,
				mDirectionStopsList,
				mServiceDatesList,
				mRouteFrequencies,
				firstTimestamp,
				lastTimestamp
		);
		mRouteSpec.setSchedules(mSchedules.values());
		this.globalGTFS.remove(this.routeId);
		MTLog.log("%s: processing... DONE in %s.", this.routeId, org.mtransit.parser.Utils.getPrettyDuration(System.currentTimeMillis() - startAt));
		return mRouteSpec;
	}

	private void parseRDS(HashMap<String, MSchedule> mSchedules,
						  HashMap<String, MFrequency> mFrequencies,
						  HashMap<Integer, MAgency> mAgencies,
						  HashMap<Long, MRoute> mRoutes,
						  HashMap<Long, MDirection> mDirections,
						  HashMap<Integer, MStop> mStops,
						  HashMap<String, MDirectionStop> allMDirectionStops,
						  HashSet<Integer> directionStopIds,
						  HashSet<Integer> serviceIdInts,
						  GSpec routeGTFS) {
		boolean mergeSuccessful;
		HashMap<Long, String> mDirectionStopTimesHeadsign;
		HashMap<Long, ArrayList<MDirectionStop>> directionIdToMDirectionStops = new HashMap<>();
		HashSet<String> mDirectionHeadsignStrings;
		boolean headsignTypeString;
		boolean directionKeptNonDescriptiveHeadsign;
		final List<GRoute> gRoutes = routeGTFS.getRoutes(this.routeId);
		Map<Integer, String> gDirectionHeadSigns = null;
		if (this.agencyTools.directionSplitterEnabled(this.routeId)) {
			MDirectionSplitter.splitDirection(this.routeId, gRoutes, routeGTFS, this.agencyTools);
		}
		if (this.agencyTools.directionFinderEnabled()) {
			List<GTrip> routeGTrips = new ArrayList<>();
			for (GRoute gRoute : gRoutes) {
				if (this.agencyTools.directionFinderEnabled(this.routeId, gRoute)) {
					routeGTrips.addAll(routeGTFS.getRouteTrips(gRoute.getRouteIdInt()));
				}
			}
			gDirectionHeadSigns = MDirectionHeadSignFinder.findDirectionHeadSigns(this.routeId, routeGTrips, routeGTFS, this.agencyTools);
			MTLog.log("%s: Found GTFS direction head sign: %s.", this.routeId, gDirectionHeadSigns);
		}
		for (GRoute gRoute : gRoutes) {
			if (this.agencyTools.getRouteId(gRoute) != this.routeId) {
				continue;
			}
			final MAgency agency = gRoute.hasAgencyId() ? mAgencies.get(gRoute.getAgencyIdInt())
					// TODO ? : mAgencies.size() == 1 ? mAgencies.values().iterator().next()
					: mAgencies.values().iterator().next();
			//noinspection DiscouragedApi
			final MRoute mRoute = new MRoute(
					this.routeId,
					this.agencyTools.getRouteShortName(gRoute),
					this.agencyTools.getRouteLongName(gRoute),
					this.agencyTools.getRouteColor(gRoute, agency),
					gRoute.getRouteId(),
					gRoute.getRouteType(),
					this.agencyTools
			);
			final MRoute otherRoute = mRoutes.get(mRoute.getId());
			if (otherRoute != null && !mRoute.equals(otherRoute)) {
				mergeSuccessful = false;
				//noinspection deprecation // TODO remove method
				if (mRoute.equalsExceptLongName(otherRoute, this.agencyTools.allowGTFSIdOverride())) {
					mergeSuccessful = this.agencyTools.mergeRouteLongName(mRoute, otherRoute);
				}
				if (!mergeSuccessful) {
					MTLog.log("%s: Route %s already in list!", this.routeId, mRoute.getId());
					MTLog.log("%s: %s", this.routeId, mRoute.toString());
					throw new MTLog.Fatal("%s: %s.", this.routeId, otherRoute.toString());
				}
			}
			mRoutes.put(mRoute.getId(), mRoute);
			mDirectionStopTimesHeadsign = new HashMap<>();
			parseGTrips(
					mSchedules,
					mFrequencies,
					mDirections,
					mStops,
					serviceIdInts,
					mRoute,
					mDirectionStopTimesHeadsign,
					directionIdToMDirectionStops,
					gRoute,
					gDirectionHeadSigns,
					routeGTFS
			);
			mDirectionHeadsignStrings = new HashSet<>();
			headsignTypeString = false;
			for (MDirection mDirection : mDirections.values()) {
				if (mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STRING) {
					mDirectionHeadsignStrings.add(mDirection.getHeadsignValue());
					headsignTypeString = true;
				}
			}
			directionKeptNonDescriptiveHeadsign = false; // 1 direction can keep the same non descriptive head sign
			if (headsignTypeString && mDirectionHeadsignStrings.size() != mDirections.size()) {
				MTLog.log("%s: Non descriptive direction headsigns (%s different headsign(s) for %s directions)", this.routeId, mDirectionHeadsignStrings.size(), mDirections.size());
				for (MDirection mDirection : mDirections.values()) {
					MTLog.log("%s: mDirection: %s", this.routeId, mDirection);
					if (mDirectionStopTimesHeadsign.containsKey(mDirection.getId()) //
							&& !mDirection.getHeadsignValue().equals(mDirectionStopTimesHeadsign.get(mDirection.getId()))) {
						MTLog.log("%s: Replace direction headsign '%s' with stop times headsign '%s' (%s)", this.routeId, mDirection.getHeadsignValue(),
								mDirectionStopTimesHeadsign.get(mDirection.getId()), mDirection);
						mDirection.setHeadsignString(mDirectionStopTimesHeadsign.get(mDirection.getId()), mDirection.getHeadsignId());
					} else {
						if (directionKeptNonDescriptiveHeadsign
								&& !agencyTools.allowNonDescriptiveHeadSigns(this.routeId)) {
							MTLog.log("%s: Direction headsign string '%s' non descriptive! (%s)", this.routeId, mDirection.getHeadsignValue(), mDirection);
							MTLog.log("%s: direction headsigns: %s", this.routeId, mDirectionHeadsignStrings);
							MTLog.log("%s: direction: %s", this.routeId, mDirections);
							throw new MTLog.Fatal("");
						}
						MTLog.log("%s: Keeping non-descriptive direction headsign '%s' (%s)", this.routeId, mDirection.getHeadsignValue(), mDirection);
						directionKeptNonDescriptiveHeadsign = true; // last direction that can keep same head sign
					}
				}
			}
			for (MDirection mDirection : mDirections.values()) {
				if (mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STRING //
						&& StringUtils.isEmpty(mDirection.getHeadsignValue())) {
					MTLog.log("%s: Direction headsign string '%s' non descriptive! (%s)", this.routeId, mDirection.getHeadsignValue(), mDirection);
					MTLog.log("%s: %s", this.routeId, mDirectionHeadsignStrings);
					MTLog.log("%s: %s", this.routeId, mDirections);
					throw new MTLog.Fatal("");
				}
			}
		}
		for (ArrayList<MDirectionStop> mDirectionStops : directionIdToMDirectionStops.values()) {
			setMDirectionStopSequence(mDirectionStops);
			for (MDirectionStop mDirectionStop : mDirectionStops) {
				if (allMDirectionStops.containsKey(mDirectionStop.getUID())
						&& !allMDirectionStops.get(mDirectionStop.getUID()).equals(mDirectionStop)) {
					MTLog.log("%s: Different direction stop '%s' already in route list (%s != %s)!", this.routeId, mDirectionStop.getUID(), mDirectionStop.toString(),
							allMDirectionStops.get(mDirectionStop.getUID()).toString());
					continue;
				}
				allMDirectionStops.put(mDirectionStop.getUID(), mDirectionStop);
				directionStopIds.add(mDirectionStop.getStopId());
			}
		}
		fixRouteLongName(mRoutes, mDirections);
	}

	private void fixRouteLongName(HashMap<Long, MRoute> mRoutes, HashMap<Long, MDirection> mDirections) {
		if (!this.agencyTools.defaultRouteLongNameEnabled()) {
			return;
		}
		for (MRoute mRoute : mRoutes.values()) {
			if (!mRoute.getLongName().isEmpty()) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			for (MDirection mDirection : mDirections.values()) {
				if (mDirection.getRouteId() == mRoute.getId()) {
					if (mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STRING) {
						if (sb.length() > 0) {
							sb.append(" <> ");
						}
						sb.append(mDirection.getHeadsignValue());
					}
				}
			}
			if (sb.length() == 0) {
				final List<Locale> supportedLanguages = this.agencyTools.getSupportedLanguages();
				if (supportedLanguages != null && supportedLanguages.size() == 1) {
					final Locale supportedLanguage = supportedLanguages.get(0);
					if (Locale.ENGLISH.equals(supportedLanguage)) {
						sb.append("Route ").append(mRoute.getShortNameOrDefault());
					} else if (Locale.FRENCH.equals(supportedLanguage)) {
						sb.append("Ligne ").append(mRoute.getShortNameOrDefault());
					} else {
						throw new MTLog.Fatal("%s: Unsupported language '%s'!", this.routeId, supportedLanguage);
					}
				}
			}
			if (sb.length() == 0) {
				throw new MTLog.Fatal("%s: Unsupported default route long name for route '%s'!", this.routeId, mRoute);
			}
			mRoute.setLongName(sb.toString());
			mRoutes.put(mRoute.getId(), mRoute);
		}
	}

	private void parseGTrips(HashMap<String, MSchedule> mSchedules,
							 HashMap<String, MFrequency> mFrequencies,
							 HashMap<Long, MDirection> mDirections,
							 HashMap<Integer, MStop> mStops,
							 HashSet<Integer> serviceIdInts,
							 MRoute mRoute,
							 HashMap<Long, String> mDirectionStopTimesHeadsign,
							 HashMap<Long, ArrayList<MDirectionStop>> directionIdToMDirectionStops,
							 GRoute gRoute,
							 Map<Integer, String> gDirectionHeadSigns,
							 GSpec routeGTFS) {
		boolean mergeSuccessful;
		HashMap<Long, HashSet<String>> mergedDirectionIdToMDirectionStops = new HashMap<>();
		HashMap<Long, Pair<Integer, String>> originalDirectionHeadsign;
		ArrayList<MDirection> splitDirections;
		HashMap<Long, HashMap<String, MDirectionStop>> splitDirectionStops;
		ArrayList<MDirectionStop> cDirectionStopsList;
		ArrayList<MDirectionStop> mDirectionStopsList;
		Integer serviceIdInt;
		HashMap<Long, String> splitDirectionStopTimesHeadsign;
		final List<GTrip> routeGTrips = GTrip.longestFirst(
				routeGTFS.getRouteTrips(gRoute.getRouteIdInt()),
				this.routeGTripIdIntGTripStops::get
		);
		///noinspection DiscouragedApi
		final String gRouteId = gRoute.getRouteId();
		MTLog.log("%s: parsing %d trips for route ID '%s'... ", this.routeId, routeGTrips.size(), gRouteId);
		int g = 0;
		for (GTrip gTrip : routeGTrips) {
			if (gTrip.getRouteIdInt() != gRoute.getRouteIdInt()) {
				throw new MTLog.Fatal("%s: Should not happen!", this.routeId); // continue; // SKIP
			}
			splitDirections = new ArrayList<>(Collections.singleton(new MDirection(mRoute.getId())));
			originalDirectionHeadsign = new HashMap<>();
			for (MDirection mDirection : splitDirections) {
				this.agencyTools.setDirectionHeadsign(mRoute, mDirection, gTrip, routeGTFS);
				Pair<Integer, String> originalDirectionHeadSignTypeAndValue = new Pair<>(mDirection.getHeadsignType(), mDirection.getHeadsignValue());
				long originalDirectionHeadSignId = mDirection.getId();
				if (splitDirections.size() == 1 // not split-ed
						&& gDirectionHeadSigns != null) { // direction finder enabled
					final String directionHeadSign = gDirectionHeadSigns.get(gTrip.getDirectionIdOrDefault());
					if (mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STRING) {
						if (directionHeadSign != null && !directionHeadSign.isEmpty()) {
							mDirection.setHeadsignString(directionHeadSign, mDirection.getHeadsignId());
							originalDirectionHeadSignId = mDirection.getId();
						}
					}
					if (this.agencyTools.getDirectionTypes().contains(MDirection.HEADSIGN_TYPE_DIRECTION)) {
						final MDirectionCardinalType direction = this.agencyTools.convertDirection(directionHeadSign);
						if (direction != null) {
							mDirection.setHeadsignDirection(direction);
							originalDirectionHeadSignId = mDirection.getId();
							if (StringUtils.equals(directionHeadSign, originalDirectionHeadSignTypeAndValue.second)) {
								originalDirectionHeadSignTypeAndValue = new Pair<>(mDirection.getHeadsignType(), mDirection.getHeadsignValue());
							}
						}
					}
				}
				originalDirectionHeadsign.put(originalDirectionHeadSignId, originalDirectionHeadSignTypeAndValue);
			}
			for (MDirection mDirection : splitDirections) {
				final MDirection currentDirection = mDirections.get(mDirection.getId());
				if (currentDirection != null && !currentDirection.equals(mDirection)) {
					mergeSuccessful = false;
					if (mDirection.equalsExceptHeadsignValue(currentDirection)) {
						mergeSuccessful = this.agencyTools.mergeHeadsign(mDirection, currentDirection, gRoute);
					}
					if (!mergeSuccessful) {
						throw new MTLog.Fatal("%s: Different direction %s already in list (%s != %s)", this.routeId, mDirection.getId(), mDirection, currentDirection);
					}
				}
			}
			serviceIdInt = gTrip.getServiceIdInt();
			parseFrequencies(mFrequencies, gTrip, splitDirections, serviceIdInt, routeGTFS);
			splitDirectionStops = new HashMap<>();
			splitDirectionStopTimesHeadsign = parseGTripStops(
					mSchedules,
					serviceIdInts,
					mStops,
					gRoute,
					gTrip,
					originalDirectionHeadsign,
					splitDirections,
					serviceIdInt,
					splitDirectionStops,
					routeGTFS
			);
			for (MDirection mDirection : splitDirections) {
				if (!splitDirectionStops.containsKey(mDirection.getId())) {
					continue;
				}
				mDirectionStopsList = new ArrayList<>(splitDirectionStops.get(mDirection.getId()).values());
				Collections.sort(mDirectionStopsList);
				setMDirectionStopSequence(mDirectionStopsList);
				String mDirectionStopListString = mDirectionStopsList.toString();
				if (mergedDirectionIdToMDirectionStops.containsKey(mDirection.getId())
						&& mergedDirectionIdToMDirectionStops.get(mDirection.getId()).contains(mDirectionStopListString)) {
					continue;
				}
				cDirectionStopsList = directionIdToMDirectionStops.get(mDirection.getId());
				if (cDirectionStopsList != null) {
					if (!CollectionUtils.equalsList(mDirectionStopsList, cDirectionStopsList)) {
						if (MDirectionStop.containsStopIds(cDirectionStopsList, mDirectionStopsList)) {
							MTLog.logDebug("%s: Skip merge because current direction stops list contains other list.", this.routeId, MDirectionStop.printDirectionStops(cDirectionStopsList));
						} else {
							MTLog.log("%s: Need to merge direction ID '%s' stops lists (sizes: %d in %d).", this.routeId, mDirection.getId(), mDirectionStopsList.size(), cDirectionStopsList.size());
							if (Constants.DEBUG) {
								MTLog.logDebug("%s: - current stops list > %s.", this.routeId, MDirectionStop.printDirectionStops(cDirectionStopsList));
								MTLog.logDebug("%s: - new stops list > %s.", this.routeId, MDirectionStop.printDirectionStops(mDirectionStopsList));
							}
							final ArrayList<MDirectionStop> resultDirectionStopsList = setMDirectionStopSequence(
									mergeMyDirectionStopLists(mDirection.getId(), mDirectionStopsList, cDirectionStopsList)
							);
							directionIdToMDirectionStops.put(mDirection.getId(), resultDirectionStopsList);
							if (Constants.DEBUG) {
								MTLog.logDebug("%s: - result stops list > %s.", this.routeId, MDirectionStop.printDirectionStops(resultDirectionStopsList));
							}
						}
					}
				} else { // just use it
					directionIdToMDirectionStops.put(mDirection.getId(), mDirectionStopsList);
				}
				if (!mergedDirectionIdToMDirectionStops.containsKey(mDirection.getId())) {
					mergedDirectionIdToMDirectionStops.put(mDirection.getId(), new HashSet<>());
				}
				mergedDirectionIdToMDirectionStops.get(mDirection.getId()).add(mDirectionStopListString);
			}
			for (MDirection mDirection : splitDirections) {
				if (!splitDirectionStops.containsKey(mDirection.getId())) {
					continue;
				}
				String directionStopTimesHeadsign = splitDirectionStopTimesHeadsign.get(mDirection.getId());
				if (mDirection.getHeadsignType() == MDirection.HEADSIGN_TYPE_STRING
						&& directionStopTimesHeadsign != null
						&& !directionStopTimesHeadsign.isEmpty()) {
					if (mDirectionStopTimesHeadsign.containsKey(mDirection.getId())) {
						if (!mDirectionStopTimesHeadsign.get(mDirection.getId()).equals(directionStopTimesHeadsign)) {
							if (!mDirectionStopTimesHeadsign.get(mDirection.getId()).contains(directionStopTimesHeadsign)) {
								MTLog.log("%s: Trip stop times head-sign different for same direction ID ('%s'!='%s')", this.routeId,
										directionStopTimesHeadsign, mDirectionStopTimesHeadsign.get(mDirection.getId()));
							}
							mDirectionStopTimesHeadsign.put(mDirection.getId(),
									MDirection.mergeHeadsignValue(
											mDirectionStopTimesHeadsign.get(mDirection.getId()),
											directionStopTimesHeadsign));
						}
					} else {
						mDirectionStopTimesHeadsign.put(mDirection.getId(), directionStopTimesHeadsign);
					}
				}
			}
			for (MDirection mDirection : splitDirections) {
				if (!splitDirectionStops.containsKey(mDirection.getId())) {
					continue;
				}
				mDirections.put(mDirection.getId(), mDirection);
			}
			if (g++ % 10 == 0) { // LOG
				MTLog.logPOINT(); // LOG
			} // LOG
		}
		MTLog.log("%s: parsing %d trips for route ID '%s'... DONE", this.routeId, routeGTrips.size(), gRouteId);
	}

	private HashMap<Long, String> parseGTripStops(HashMap<String, MSchedule> mSchedules,
												  HashSet<Integer> serviceIdInts,
												  HashMap<Integer, MStop> mStops,
												  GRoute gRoute,
												  GTrip gTrip,
												  HashMap<Long, Pair<Integer, String>> originalDirectionHeadsign,
												  ArrayList<MDirection> splitDirections,
												  Integer serviceIdInt,
												  HashMap<Long, HashMap<String, MDirectionStop>> splitDirectionStops,
												  GSpec routeGTFS) {
		HashMap<Long, String> splitDirectionStopTimesHeadSign = new HashMap<>();
		int mStopId;
		GStop gStop;
		MDirectionStop mDirectionStop;
		long mDirectionId;
		String directionStopTimesHeadsign;
		Pair<Long[], Integer[]> mDirectionsAndStopSequences;
		HashMap<String, Integer> addedMDirectionIdAndGStopIds = new HashMap<>();
		final List<GTripStop> gTripStops = this.routeGTripIdIntGTripStops.get(gTrip.getTripIdInt());
		if (gTripStops == null) {
			return splitDirectionStopTimesHeadSign;
		}
		for (GTripStop gTripStop : gTripStops) {
			if (gTripStop.getTripIdInt() != gTrip.getTripIdInt()) {
				continue;
			}
			gStop = routeGTFS.getStop(gTripStop.getStopIdInt());
			if (gStop == null) { // was excluded previously
				continue;
			}
			mStopId = this.agencyTools.getStopId(gStop);
			this.gStopsCache.put(mStopId, gStop);
			if (mStopId < 0) {
				//noinspection DiscouragedApi
				throw new MTLog.Fatal("%s: Can't find GTFS stop ID (%s) '%s' from trip ID '%s' (%s)", this.routeId, mStopId, gTripStop.getStopIdInt(),
						gTripStop.getTripId(), gStop.toStringPlus(true));
			}
			mDirectionsAndStopSequences = new Pair<>(
					new Long[]{splitDirections.get(0).getId()},
					new Integer[]{gTripStop.getStopSequence()}
			);
			for (int i = 0; i < mDirectionsAndStopSequences.first.length; i++) {
				mDirectionId = mDirectionsAndStopSequences.first[i];
				mDirectionStop = new MDirectionStop(mDirectionId, mStopId, mDirectionsAndStopSequences.second[i]);
				if (!splitDirectionStops.containsKey(mDirectionId)) {
					splitDirectionStops.put(mDirectionId, new HashMap<>());
				}
				final HashMap<String, MDirectionStop> uuidToDirectionStops = splitDirectionStops.get(mDirectionId);
				final MDirectionStop sameUuidDirectionStop = uuidToDirectionStops.get(mDirectionStop.getUID());
				if (sameUuidDirectionStop != null) {
					if (!sameUuidDirectionStop.equalsExceptStopSequence(mDirectionStop)) {
						throw new MTLog.Fatal("%s: Different direction '%s' stop '%s' already in list (%s != %s)!",
								this.routeId,
								mDirectionStop.getDirectionId(),
								mDirectionStop.getStopId(),
								mDirectionStop.toString(),
								sameUuidDirectionStop.toString());
					} else {
						MTLog.logDebug("%s: Same direction '%s' stop '%s' already in list (sequence %s != %s).",
								this.routeId,
								mDirectionStop.getDirectionId(),
								mDirectionStop.getStopId(),
								mDirectionStop.getStopSequence(),
								sameUuidDirectionStop.getStopSequence());
					}
				} else {
					uuidToDirectionStops.put(mDirectionStop.getUID(), mDirectionStop);
				}
				directionStopTimesHeadsign = splitDirectionStopTimesHeadSign.get(mDirectionId);
				if (!originalDirectionHeadsign.containsKey(mDirectionId)) {
					throw new MTLog.Fatal("%s: Unexpected direction head-sign ID '%s'! (%s)", this.routeId, mDirectionId, originalDirectionHeadsign);
				}
				directionStopTimesHeadsign = parseGStopTimes(
						mSchedules,
						mDirectionId,
						serviceIdInt,
						originalDirectionHeadsign.get(mDirectionId).first,
						originalDirectionHeadsign.get(mDirectionId).second,
						directionStopTimesHeadsign,
						gRoute,
						gTrip,
						gTripStop,
						mStopId,
						addedMDirectionIdAndGStopIds
				);
				splitDirectionStopTimesHeadSign.put(mDirectionId, directionStopTimesHeadsign);
				serviceIdInts.add(serviceIdInt);
			}
			if (!mStops.containsKey(mStopId)) {
				//noinspection DiscouragedApi
				mStops.put(
						mStopId,
						new MStop(mStopId,
								this.agencyTools.getStopCode(gStop),
								this.agencyTools.cleanStopName(gStop.getStopName()),
								gStop.getStopLat(),
								gStop.getStopLong(),
								gStop.getWheelchairBoarding().getId(),
								gStop.getStopId(),
								this.agencyTools
						));
			}
		}
		return splitDirectionStopTimesHeadSign;
	}

	private final SimpleDateFormat TIME_FORMAT = GFieldTypes.makeTimeFormat();

	private String parseGStopTimes(HashMap<String, MSchedule> mSchedules,
								   long mDirectionId,
								   Integer serviceIdInt,
								   int originalDirectionHeadsignType,
								   @Nullable String originalDirectionHeadsignValue,
								   String directionStopTimesHeadsign,
								   @NotNull GRoute gRoute,
								   @NotNull GTrip gTrip,
								   @NotNull GTripStop gTripStop,
								   int mStopId,
								   HashMap<String, Integer> addedMDirectionIdAndGStopIds) {
		MSchedule mSchedule;
		String stopHeadsign;
		boolean noPickup;
		String directionIdStopId;
		List<GStopTime> gTripStopTimes = getTripGStopTimes(gTripStop.getTripIdInt());
		int lastStopSequence = -1;
		GStopTime lastStopTime = null;
		for (int i = 0; i < gTripStopTimes.size(); i++) {
			GStopTime gStopTime = gTripStopTimes.get(i);
			if (gStopTime.getStopSequence() < lastStopSequence) {
				MTLog.log("%s: Stop sequence out of order (%s => '%s')!", this.routeId, lastStopSequence, gStopTime);
				throw new MTLog.Fatal("%s: Stop sequence out of order ([%s] => [%s])!", this.routeId, lastStopTime, gStopTime);
			}
			lastStopSequence = gStopTime.getStopSequence();
			lastStopTime = gStopTime;
			noPickup = false;
			if (gStopTime.getTripIdInt() != gTripStop.getTripIdInt() //
					|| gStopTime.getStopIdInt() != gTripStop.getStopIdInt() //
					|| gStopTime.getStopSequence() != gTripStop.getStopSequence()) {
				continue;
			}
			if (gStopTime.getPickupType() == GPickupType.NO_PICKUP) { // last stop of the trip
				noPickup = true;
			}
			directionIdStopId = String.valueOf(mDirectionId) + gStopTime.getTripIdInt() + gStopTime.getStopIdInt();
			if (noPickup) {
				if (addedMDirectionIdAndGStopIds.containsKey(directionIdStopId) //
						&& addedMDirectionIdAndGStopIds.get(directionIdStopId) != gStopTime.getStopSequence()) {
					// TODO later, when UI can display multiple times same stop/POI & schedules are affected to a specific sequence, keep both
				}
			}
			mSchedule = new MSchedule(
					this.routeId,
					serviceIdInt,
					mDirectionId,
					mStopId,
					this.agencyTools.getTimes(
							gStopTime,
							gTripStopTimes,
							TIME_FORMAT
					),
					gStopTime.getTripIdInt(),
					gTrip.getWheelchairAccessible().getId()
			);
			if (mSchedules.containsKey(mSchedule.getUID()) //
					&& !mSchedules.get(mSchedule.getUID()).isSameServiceRDSDeparture(mSchedule)) {
				throw new MTLog.Fatal("%s: Different schedule %s (%s) already in list (%s != %s)!",
						this.routeId,
						mSchedule.getUID(),
						mSchedules.get(mSchedule.getUID()).getUID(),
						mSchedule.toStringPlus(),
						mSchedules.get(mSchedule.getUID()).toStringPlus());
			}
			if (noPickup) {
				mSchedule.setHeadsign(MDirection.HEADSIGN_TYPE_NO_PICKUP, null);
			} else if (gStopTime.hasStopHeadsign()) {
				stopHeadsign = this.agencyTools.cleanStopHeadSign(gRoute, gTrip, gStopTime, gStopTime.getStopHeadsignOrDefault());
				mSchedule.setHeadsign(MDirection.HEADSIGN_TYPE_STRING, stopHeadsign);
				directionStopTimesHeadsign = setDirectionStopTimesHeadsign(directionStopTimesHeadsign, stopHeadsign);
			} else {
				if (!StringUtils.isBlank(originalDirectionHeadsignValue)) {
					mSchedule.setHeadsign(originalDirectionHeadsignType, originalDirectionHeadsignValue);
				}
			}
			mSchedules.put(mSchedule.getUID(), mSchedule);
			addedMDirectionIdAndGStopIds.put(directionIdStopId, gStopTime.getStopSequence());
		}
		return directionStopTimesHeadsign;
	}

	private void parseFrequencies(HashMap<String, MFrequency> mFrequencies,
								  GTrip gTrip,
								  ArrayList<MDirection> splitDirections,
								  Integer serviceIdInt,
								  GSpec routeGTFS) {
		MFrequency mFrequency;
		for (GFrequency gFrequency : routeGTFS.getFrequencies(gTrip.getTripIdInt())) {
			if (gFrequency.getTripIdInt() != gTrip.getTripIdInt()) {
				continue;
			}
			for (MDirection mDirection : splitDirections) {
				mFrequency = new MFrequency(
						serviceIdInt,
						mDirection.getId(),
						this.agencyTools.getStartTime(gFrequency),
						this.agencyTools.getEndTime(gFrequency),
						gFrequency.getHeadwaySecs()
				);
				if (mFrequencies.containsKey(mFrequency.getUID()) && !mFrequencies.get(mFrequency.getUID()).equals(mFrequency)) {
					throw new MTLog.Fatal("%s: Different frequency %s already in list!\n- %s\n- %s \n", this.routeId, mFrequency.getUID(), mFrequency.toStringPlus(),
							mFrequencies.get(mFrequency.getUID()).toStringPlus());
				}
				mFrequencies.put(mFrequency.getUID(), mFrequency);
			}
		}
	}

	private static String setDirectionStopTimesHeadsign(String directionStopTimesHeadsign, String stopHeadSign) {
		if (directionStopTimesHeadsign == null) {
			directionStopTimesHeadsign = stopHeadSign;
		} else if (Constants.EMPTY.equals(directionStopTimesHeadsign)) { // disabled
			// nothing to do
		} else if (!directionStopTimesHeadsign.equals(stopHeadSign)) {
			directionStopTimesHeadsign = Constants.EMPTY; // disable
		}
		return directionStopTimesHeadsign;
	}

	private void setDirectionStopNoPickup(@NotNull ArrayList<MDirectionStop> mDirectionStopsList,
										  @NotNull Collection<MSchedule> mSchedules) {
		for (MDirectionStop directionStop : mDirectionStopsList) {
			boolean noPickup = false;
			for (MSchedule schedule : mSchedules) {
				if (schedule.getDirectionId() != directionStop.getDirectionId()) {
					continue;
				}
				if (schedule.getStopId() != directionStop.getStopId()) {
					continue;
				}
				if (schedule.isNoPickup()) {
					noPickup = true;
					//noinspection UnnecessaryContinue
					continue;
				} else {
					noPickup = false;
					break;
				}
			}
			directionStop.setNoPickup(noPickup);
		}
		// SKIP (descent only set on the stop time schedule level
	}

	@NotNull
	private ArrayList<MDirectionStop> mergeMyDirectionStopLists(long directionId, @NotNull ArrayList<MDirectionStop> list1, @NotNull ArrayList<MDirectionStop> list2) {
		final String logTag = this.routeId + ": " + directionId;
		ArrayList<MDirectionStop> newList = new ArrayList<>();
		HashSet<Integer> newListStopIds = new HashSet<>();
		HashSet<Integer> list1StopIds = new HashSet<>();
		for (MDirectionStop ts1 : list1) {
			list1StopIds.add(ts1.getStopId());
		}
		HashSet<Integer> list2StopIds = new HashSet<>();
		for (MDirectionStop ts2 : list2) {
			list2StopIds.add(ts2.getStopId());
		}
		MDirectionStop ts1, ts2;
		boolean s2InL1, s1InL2;
		boolean lastInL1, lastInL2;
		GStop lastGStop, ts1GStop, ts2GStop;
		GStop commonGStop, previousTs1GStop, previousTs2GStop;
		double ts1Distance, ts2Distance;
		double previousTs1Distance, previousTs2Distance;
		MDirectionStop[] commonStopAndPrevious;
		int i1 = 0, i2 = 0;
		MDirectionStop last = null;
		//noinspection ForLoopReplaceableByWhile
		for (; i1 < list1.size() && i2 < list2.size(); ) {
			ts1 = list1.get(i1);
			ts2 = list2.get(i2);
			if (newListStopIds.contains(ts1.getStopId())) {
				MTLog.logDebug("%s: Skipped %s because already in the merged list (1).", logTag, ts1.toStringSameDirection());
				i1++; // skip this stop because already in the merged list
				continue;
			}
			if (newListStopIds.contains(ts2.getStopId())) {
				MTLog.logDebug("%s: Skipped %s because already in the merged list (2).", logTag, ts2.toStringSameDirection());
				i2++; // skip this stop because already in the merged list
				continue;
			}
			if (ts1.getStopId() == ts2.getStopId()) {
				if (!ts1.equalsExceptStopSequence(ts2)) { // not loosing no pickup info (TODO really? added 2022-12-03)
					throw new MTLog.Fatal("%s: Trying to merge different direction stop for same stop %s VS %s.", logTag, ts1.toStringSameDirection(), ts2.toStringSameDirection());
				}
				// MTLog.logDebug("%s: Merged same stop %s (1=2).", logTag, ts1.toStringSimple());
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				i2++;
				continue;
			}
			// find next match
			// look for stop in other list
			s2InL1 = list1StopIds.contains(ts2.getStopId());
			s1InL2 = list2StopIds.contains(ts1.getStopId());
			if (s2InL1 && !s1InL2) {
				MTLog.logDebug("%s: Merged stop %s from 1st list NOT present in 2nd list (1).", logTag, ts1.toStringSameDirection());
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			}
			if (!s2InL1 && s1InL2) {
				MTLog.logDebug("%s: Merged stop %s from 2nd list NOT present in 1st list (2).", logTag, ts2.toStringSameDirection());
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
				continue;
			}
			// MANUAL MERGE
			if (last != null) {
				lastInL1 = list1StopIds.contains(last.getStopId());
				lastInL2 = list2StopIds.contains(last.getStopId());
				if (lastInL1 && !lastInL2) {
					MTLog.log(this.routeId
							+ ": pick t:" + ts1.getDirectionId() + ">s:" + ts1.getStopId()
							+ " in same list w/ s:" + last.getStopId()
							+ " instead of t:" + ts2.getDirectionId() + ">s:" + ts2.getStopId());
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				}
				if (!lastInL1 && lastInL2) {
					MTLog.log(this.routeId
							+ ": pick t:" + ts2.getDirectionId() + ">s:" + ts2.getStopId()
							+ " in same list w/ s:" + last.getStopId()
							+ " instead of t:" + ts1.getDirectionId() + ">s:" + ts1.getStopId());
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
					continue;
				}
			}
			ts1GStop = this.gStopsCache.get(ts1.getStopId());
			ts2GStop = this.gStopsCache.get(ts2.getStopId());
			if (last != null) {
				lastGStop = this.gStopsCache.get(last.getStopId());
				ts1GStop = this.gStopsCache.get(ts1.getStopId());
				ts2GStop = this.gStopsCache.get(ts2.getStopId());
				ts1Distance = findDistance(lastGStop.getStopLat(), lastGStop.getStopLong(), ts1GStop.getStopLat(), ts1GStop.getStopLong());
				ts2Distance = findDistance(lastGStop.getStopLat(), lastGStop.getStopLong(), ts2GStop.getStopLat(), ts2GStop.getStopLong());
				if (ts1Distance < ts2Distance) {
					MTLog.log(this.routeId
							+ ": pick t:" + ts1.getDirectionId() + ">" + ts1GStop.toStringPlus()
							+ " closest (" + (ts2Distance - ts1Distance) + ") to " + lastGStop.toStringPlus()
							+ " instead of t:" + ts2.getDirectionId() + ">" + ts2GStop.toStringPlus());
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
				} else {
					MTLog.log(this.routeId
							+ ": pick t:" + ts2.getDirectionId() + ">" + ts2GStop.toStringPlus()
							+ " closest (" + (ts1Distance - ts2Distance) + ") to " + lastGStop.toStringPlus()
							+ " instead of t:" + ts1.getDirectionId() + ">" + ts1GStop.toStringPlus());
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
				}
				continue;
			}
			// try to find 1rst common stop
			commonStopAndPrevious = findFirstCommonStop(list1, list2);
			if (commonStopAndPrevious.length >= 3) {
				commonGStop = this.gStopsCache.get(commonStopAndPrevious[0].getStopId());
				previousTs1GStop = this.gStopsCache.get(ts1.getStopId());
				previousTs2GStop = this.gStopsCache.get(ts2.getStopId());
				previousTs1Distance = findDistance(commonGStop.getStopLat(), commonGStop.getStopLong(), previousTs1GStop.getStopLat(),
						previousTs1GStop.getStopLong());
				previousTs2Distance = findDistance(commonGStop.getStopLat(), commonGStop.getStopLong(), previousTs2GStop.getStopLat(),
						previousTs2GStop.getStopLong());
				MTLog.log(this.routeId + ": Resolved using 1st common stop direction ID:" + ts1.getDirectionId() + ", stop IDs:"
						+ ts1.getStopId() + "," + ts2.getStopId() + " ("
						+ commonStopAndPrevious[1].getStopId() + ":" + previousTs1Distance + ", "
						+ commonStopAndPrevious[2].getStopId() + ":" + previousTs2Distance + ")");
				if (previousTs1Distance > previousTs2Distance) {
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
				} else {
					newList.add(ts2);
					newListStopIds.add(ts2.getStopId());
					last = ts2;
					i2++;
				}
				continue;
			}

			if (ts1GStop.getStopLat() < ts2GStop.getStopLat() || ts1GStop.getStopLong() < ts2GStop.getStopLong()) {
				MTLog.logDebug("%s: Merged stop %s using arbitrary lat long (1).", logTag, ts1.toStringSameDirection());
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
			} else {
				MTLog.logDebug("%s: Merged stop %s using arbitrary lat long (2).", logTag, ts2.toStringSameDirection());
				newList.add(ts2);
				newListStopIds.add(ts2.getStopId());
				last = ts2;
				i2++;
			}
			//noinspection UnnecessaryContinue
			continue;
		}
		// add remaining stops
		if (i1 < list1.size()) {
			MTLog.logDebug("%s: Merged remaining %s stops from (1).", logTag, list1.size());
		}
		//noinspection ForLoopReplaceableByWhile
		for (; i1 < list1.size(); ) {
			newList.add(list1.get(i1++));
		}
		if (i2 < list2.size()) {
			MTLog.logDebug("%s: Merged remaining %s stops from (2).", logTag, list2.size());
		}
		//noinspection ForLoopReplaceableByWhile
		for (; i2 < list2.size(); ) {
			newList.add(list2.get(i2++));
		}
		return newList;
	}

	@NotNull
	private ArrayList<MDirectionStop> setMDirectionStopSequence(@NotNull ArrayList<MDirectionStop> mDirectionStops) {
		for (int i = 0; i < mDirectionStops.size(); i++) {
			final int newSequence = i + 1;
			final MDirectionStop mDirectionStop = mDirectionStops.get(i);
			if (mDirectionStop.getStopSequence() == newSequence) {
				continue;
			}
			mDirectionStop.setStopSequence(newSequence);
		}
		return mDirectionStops;
	}

	private MDirectionStop[] findFirstCommonStop(ArrayList<MDirectionStop> l1, ArrayList<MDirectionStop> l2) {
		MDirectionStop previousTs1 = null;
		MDirectionStop previousTs2;
		MDirectionStop[] commonStopAndPrevious;
		for (MDirectionStop tts1 : l1) {
			previousTs2 = null;
			for (MDirectionStop tts2 : l2) {
				if (tts1.getStopId() == tts2.getStopId()) {
					if (previousTs1 == null || previousTs2 == null) {
						MTLog.log(this.routeId + ": findFirstCommonStop() > Common stop found '" + tts1.getStopId()
								+ "' but no previous stop! Looking for next common stop...");
					} else {
						commonStopAndPrevious = new MDirectionStop[3];
						commonStopAndPrevious[0] = tts1;
						commonStopAndPrevious[1] = previousTs1;
						commonStopAndPrevious[2] = previousTs2;
						return commonStopAndPrevious;
					}
				}
				previousTs2 = tts2;
			}
			previousTs1 = tts1;
		}
		return new MDirectionStop[0];
	}

	private final float[] results = new float[2];

	private float findDistance(double lat1, double lon1, double lat2, double lon2) {
		computeDistanceAndBearing(lat1, lon1, lat2, lon2, results);
		return results[0];
	}

	private final HashMap<Integer, GStop> gStopsCache = new HashMap<>();

	// https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/location/java/android/location/Location.java
	private static void computeDistanceAndBearing(double lat1, double lon1, double lat2, double lon2, float[] results) {
		// Based on https://www.ngs.noaa.gov/PUBS_LIB/inverse.pdf
		// using the "Inverse Formula" (section 4)

		int MAXITERS = 20;
		// Convert lat/long to radians
		lat1 *= Math.PI / 180.0;
		lat2 *= Math.PI / 180.0;
		lon1 *= Math.PI / 180.0;
		lon2 *= Math.PI / 180.0;

		double a = 6378137.0; // WGS84 major axis
		double b = 6356752.3142; // WGS84 semi-major axis
		double f = (a - b) / a;
		double aSqMinusBSqOverBSq = (a * a - b * b) / (b * b);

		double L = lon2 - lon1;
		double A = 0.0;
		double U1 = Math.atan((1.0 - f) * Math.tan(lat1));
		double U2 = Math.atan((1.0 - f) * Math.tan(lat2));

		double cosU1 = Math.cos(U1);
		double cosU2 = Math.cos(U2);
		double sinU1 = Math.sin(U1);
		double sinU2 = Math.sin(U2);
		double cosU1cosU2 = cosU1 * cosU2;
		double sinU1sinU2 = sinU1 * sinU2;

		double sigma = 0.0;
		double deltaSigma = 0.0;
		double cosSqAlpha;
		double cos2SM;
		double cosSigma;
		double sinSigma;
		double cosLambda = 0.0;
		double sinLambda = 0.0;

		double lambda = L; // initial guess
		for (int iter = 0; iter < MAXITERS; iter++) {
			double lambdaOrig = lambda;
			cosLambda = Math.cos(lambda);
			sinLambda = Math.sin(lambda);
			double t1 = cosU2 * sinLambda;
			double t2 = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda;
			double sinSqSigma = t1 * t1 + t2 * t2; // (14)
			sinSigma = Math.sqrt(sinSqSigma);
			cosSigma = sinU1sinU2 + cosU1cosU2 * cosLambda; // (15)
			sigma = Math.atan2(sinSigma, cosSigma); // (16)
			double sinAlpha = (sinSigma == 0) ? 0.0 : cosU1cosU2 * sinLambda / sinSigma; // (17)
			cosSqAlpha = 1.0 - sinAlpha * sinAlpha;
			cos2SM = (cosSqAlpha == 0) ? 0.0 : cosSigma - 2.0 * sinU1sinU2 / cosSqAlpha; // (18)

			double uSquared = cosSqAlpha * aSqMinusBSqOverBSq; // defn
			A = 1 + (uSquared / 16384.0) * // (3)
					(4096.0 + uSquared * (-768 + uSquared * (320.0 - 175.0 * uSquared)));
			double B = (uSquared / 1024.0) * // (4)
					(256.0 + uSquared * (-128.0 + uSquared * (74.0 - 47.0 * uSquared)));
			double C = (f / 16.0) * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha)); // (10)
			double cos2SMSq = cos2SM * cos2SM;
			deltaSigma = B
					* sinSigma
					* // (6)
					(cos2SM + (B / 4.0)
							* (cosSigma * (-1.0 + 2.0 * cos2SMSq) - (B / 6.0) * cos2SM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SMSq)));

			lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SM + C * cosSigma * (-1.0 + 2.0 * cos2SM * cos2SM))); // (11)

			double delta = (lambda - lambdaOrig) / lambda;
			if (Math.abs(delta) < 1.0e-12) {
				break;
			}
		}

		float distance = (float) (b * A * (sigma - deltaSigma));
		results[0] = distance;
		if (results.length > 1) {
			float initialBearing = (float) Math.atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda);
			initialBearing *= (float) (180.0 / Math.PI);
			results[1] = initialBearing;
			if (results.length > 2) {
				float finalBearing = (float) Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
				finalBearing *= (float) (180.0 / Math.PI);
				results[2] = finalBearing;
			}
		}
	}
}
