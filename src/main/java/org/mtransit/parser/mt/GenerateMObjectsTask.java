package org.mtransit.parser.mt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.commons.StringUtils;
import org.mtransit.parser.Constants;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.db.DBUtils;
import org.mtransit.parser.gtfs.GAgencyTools;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GPickupType;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MFrequency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MSchedule;
import org.mtransit.parser.mt.data.MServiceDate;
import org.mtransit.parser.mt.data.MSpec;
import org.mtransit.parser.mt.data.MStop;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

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
	private final Map<Integer, List<GStopTime>> routeTripIdStopTimes = new HashMap<>();
	@NotNull
	private final Map<Integer, List<GTripStop>> routeTripIdTripStops = new HashMap<>();

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
	public List<GStopTime> getTripStopTimes(int tripIdInt) {
		return this.routeTripIdStopTimes.getOrDefault(tripIdInt, Collections.emptyList());
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
		HashMap<Long, MTrip> mTrips = new HashMap<>();
		HashMap<Long, MTripStop> allMTripStops = new HashMap<>();
		HashMap<Integer, MStop> mStops = new HashMap<>();
		HashSet<Integer> tripStopIds = new HashSet<>(); // the list of stop IDs used by trips
		HashSet<Integer> serviceIdInts = new HashSet<>();
		final GSpec routeGTFS = this.globalGTFS.getRouteGTFS(this.routeId);
		List<Integer> routeTripsIntIds = new ArrayList<>();
		for (GRoute gRoute : routeGTFS.getRoutes(this.routeId)) {
			List<GTrip> routeTrips = routeGTFS.getRouteTrips(gRoute.getRouteIdInt());
			for (GTrip gTrip : routeTrips) {
				routeTripsIntIds.add(gTrip.getTripIdInt());
			}
		}
		for (GStopTime gStopTime : DBUtils.selectStopTimes(null, routeTripsIntIds, null, null)) {
			List<GStopTime> tripIdStopTimes = routeTripIdStopTimes.get(gStopTime.getTripIdInt());
			if (tripIdStopTimes == null) {
				tripIdStopTimes = new ArrayList<>();
			}
			tripIdStopTimes.add(gStopTime);
			routeTripIdStopTimes.put(gStopTime.getTripIdInt(), tripIdStopTimes);
		}
		for (GTripStop gTripStop : DBUtils.selectTripStops(null, routeTripsIntIds, null, null)) {
			List<GTripStop> tripIdTripStops = routeTripIdTripStops.get(gTripStop.getTripIdInt());
			if (tripIdTripStops == null) {
				tripIdTripStops = new ArrayList<>();
			}
			tripIdTripStops.add(gTripStop);
			routeTripIdTripStops.put(gTripStop.getTripIdInt(), tripIdTripStops);
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
		parseRTS(
				mSchedules,
				mFrequencies,
				mAgencies,
				mRoutes,
				mTrips,
				mStops,
				allMTripStops,
				tripStopIds,
				serviceIdInts,
				routeGTFS
		);
		HashSet<Long> gCalendarDateServiceRemoved = new HashSet<>();
		for (GCalendarDate gCalendarDate : routeGTFS.getAllCalendarDates()) {
			if (!serviceIdInts.contains(gCalendarDate.getServiceIdInt())) {
				continue;
			}
			switch (gCalendarDate.getExceptionType()) {
			case SERVICE_REMOVED: // keep list of removed service for calendars processing
				gCalendarDateServiceRemoved.add(gCalendarDate.getUID());
				break;
			case SERVICE_ADDED:
				mServiceDates.add(new MServiceDate(
						gCalendarDate.getServiceIdInt(),
						gCalendarDate.getDate()
				));
				break;
			default:
				throw new MTLog.Fatal("%s: Unexpected calendar date exception type '%s'!", this.routeId, gCalendarDate.getExceptionType());
			}
		}
		for (GCalendar gCalendar : routeGTFS.getAllCalendars()) {
			if (!serviceIdInts.contains(gCalendar.getServiceIdInt())) {
				continue;
			}
			for (GCalendarDate gCalendarDate : gCalendar.getDates()) {
				if (gCalendarDateServiceRemoved.contains(gCalendarDate.getUID())) {
					continue; // service REMOVED at this date
				}
				mServiceDates.add(new MServiceDate(
						gCalendarDate.getServiceIdInt(),
						gCalendarDate.getDate()
				));
			}
		}
		MTrip mTrip;
		for (MSchedule mSchedule : mSchedules.values()) {
			mTrip = mTrips.get(mSchedule.getTripId());
			if (mTrip.getHeadsignType() == mSchedule.getHeadsignType() //
					&& StringUtils.equals(mTrip.getHeadsignValue(), mSchedule.getHeadsignValue())) {
				mSchedule.clearHeadsign();
			}
		}
		ArrayList<MAgency> mAgenciesList = new ArrayList<>(mAgencies.values());
		Collections.sort(mAgenciesList);
		ArrayList<MStop> mStopsList = new ArrayList<>(mStops.values());
		Collections.sort(mStopsList);
		ArrayList<MRoute> mRoutesList = new ArrayList<>(mRoutes.values());
		Collections.sort(mRoutesList);
		ArrayList<MTrip> mTripsList = new ArrayList<>(mTrips.values());
		Collections.sort(mTripsList);
		ArrayList<MTripStop> mTripStopsList = new ArrayList<>(allMTripStops.values());
		Collections.sort(mTripStopsList);
		setTripStopDescentOnly(mTripStopsList, mSchedules.values());
		ArrayList<MServiceDate> mServiceDatesList = new ArrayList<>(mServiceDates);
		Collections.sort(mServiceDatesList);
		ArrayList<MFrequency> mFrequenciesList = new ArrayList<>(mFrequencies.values());
		Collections.sort(mFrequenciesList);
		TreeMap<Long, ArrayList<MFrequency>> mRouteFrequencies = new TreeMap<>();
		if (mFrequenciesList.size() > 0) {
			mRouteFrequencies.put(this.routeId, mFrequenciesList);
		}
		long firstTimestamp = -1L;
		long lastTimestamp = -1L;
		if (mServiceDatesList.size() > 0) {
			MServiceDate firstServiceDate = mServiceDatesList.get(0);
			final int maxCalendarDate = firstServiceDate.getCalendarDate() + 10 * 10_000; // max 10 years
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
			SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HHmmss", Locale.ENGLISH);
			DATE_FORMAT.setTimeZone(TimeZone.getTimeZone(mAgenciesList.get(0).getTimezone()));
			try {
				Date firstDate = DATE_FORMAT.parse(firstCalendarDate + " " + String.format(Locale.ENGLISH, "%06d", firstDeparture));
				firstTimestamp = firstDate.getTime();
			} catch (Exception e) {
				throw new MTLog.Fatal(e, "Error while parsing dates '%s %s'!", firstCalendarDate, firstDeparture);
			}
			try {
				Date lastDate = DATE_FORMAT.parse(lastCalendarDate + " " + String.format(Locale.ENGLISH, "%06d", lastDeparture));
				lastTimestamp = lastDate.getTime();
			} catch (Exception e) {
				throw new MTLog.Fatal(e, "Error while parsing dates '%s %s'!", lastCalendarDate, lastDeparture);
			}
		}
		MSpec mRouteSpec = new MSpec(
				mAgenciesList,
				mStopsList,
				mRoutesList,
				mTripsList,
				mTripStopsList,
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

	private void parseRTS(HashMap<String, MSchedule> mSchedules,
						  HashMap<String, MFrequency> mFrequencies,
						  HashMap<Integer, MAgency> mAgencies,
						  HashMap<Long, MRoute> mRoutes,
						  HashMap<Long, MTrip> mTrips,
						  HashMap<Integer, MStop> mStops,
						  HashMap<Long, MTripStop> allMTripStops,
						  HashSet<Integer> tripStopIds,
						  HashSet<Integer> serviceIdInts,
						  GSpec routeGTFS) {
		boolean mergeSuccessful;
		HashMap<Long, String> mTripStopTimesHeadsign;
		HashMap<Long, ArrayList<MTripStop>> tripIdToMTripStops = new HashMap<>();
		HashSet<String> mTripHeadsignStrings;
		boolean headsignTypeString;
		boolean tripKeptNonDescriptiveHeadsign;
		final ArrayList<GRoute> gRoutes = routeGTFS.getRoutes(this.routeId);
		Map<Integer, String> gDirectionHeadSigns = null;
		if (this.agencyTools.directionSplitterEnabled(this.routeId)) {
			MDirectionSplitter.splitDirection(this.routeId, gRoutes, routeGTFS, this.agencyTools);
		}
		if (this.agencyTools.directionFinderEnabled()) {
			List<GTrip> gRouteTrips = new ArrayList<>();
			for (GRoute gRoute : gRoutes) {
				if (this.agencyTools.directionFinderEnabled(this.routeId, gRoute)) {
					gRouteTrips.addAll(routeGTFS.getRouteTrips(gRoute.getRouteIdInt()));
				}
			}
			gDirectionHeadSigns = MDirectionHeadSignFinder.findDirectionHeadSigns(this.routeId, gRouteTrips, routeGTFS, this.agencyTools);
			MTLog.log("%s: Found GTFS direction head sign: %s.", this.routeId, gDirectionHeadSigns);
		}
		for (GRoute gRoute : gRoutes) {
			if (this.agencyTools.getRouteId(gRoute) != this.routeId) {
				continue;
			}
			final MAgency agency = gRoute.hasAgencyId() ? mAgencies.get(gRoute.getAgencyIdInt())
					: mAgencies.size() == 1 ? mAgencies.values().iterator().next()
					: mAgencies.values().iterator().next();
			final MRoute mRoute = new MRoute(
					this.routeId,
					this.agencyTools.getRouteShortName(gRoute),
					this.agencyTools.getRouteLongName(gRoute),
					this.agencyTools.getRouteColor(gRoute, agency)
			);
			final MRoute otherRoute = mRoutes.get(mRoute.getId());
			if (otherRoute != null && !mRoute.equals(otherRoute)) {
				mergeSuccessful = false;
				if (mRoute.equalsExceptLongName(otherRoute)) {
					mergeSuccessful = this.agencyTools.mergeRouteLongName(mRoute, otherRoute);
				}
				if (!mergeSuccessful) {
					MTLog.log("%s: Route %s already in list!", this.routeId, mRoute.getId());
					MTLog.log("%s: %s", this.routeId, mRoute.toString());
					throw new MTLog.Fatal("%s: %s.", this.routeId, otherRoute.toString());
				}
			}
			mRoutes.put(mRoute.getId(), mRoute);
			mTripStopTimesHeadsign = new HashMap<>();
			parseTrips(
					mSchedules,
					mFrequencies,
					mTrips,
					mStops,
					serviceIdInts,
					mRoute,
					mTripStopTimesHeadsign,
					tripIdToMTripStops,
					gRoute,
					gDirectionHeadSigns,
					routeGTFS
			);
			mTripHeadsignStrings = new HashSet<>();
			headsignTypeString = false;
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
					mTripHeadsignStrings.add(mTrip.getHeadsignValue());
					headsignTypeString = true;
				}
			}
			// HashSet<String> mTripStopHeadsignStrings = new HashSet<>(mTripStopTimesHeadsign.values());
			tripKeptNonDescriptiveHeadsign = false; // 1 trip can keep the same non descriptive head sign
			if (headsignTypeString && mTripHeadsignStrings.size() != mTrips.size()) {
				MTLog.log("%s: Non descriptive trip headsigns (%s different headsign(s) for %s trips)", this.routeId, mTripHeadsignStrings.size(), mTrips.size());
				for (MTrip mTrip : mTrips.values()) {
					MTLog.log("%s: mTrip: %s", this.routeId, mTrip);
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId()) //
							&& !mTrip.getHeadsignValue().equals(mTripStopTimesHeadsign.get(mTrip.getId()))) {
						MTLog.log("%s: Replace trip headsign '%s' with stop times headsign '%s' (%s)", this.routeId, mTrip.getHeadsignValue(),
								mTripStopTimesHeadsign.get(mTrip.getId()), mTrip);
						mTrip.setHeadsignString(mTripStopTimesHeadsign.get(mTrip.getId()), mTrip.getHeadsignId());
					} else {
						if (tripKeptNonDescriptiveHeadsign
								&& !agencyTools.allowNonDescriptiveHeadSigns(this.routeId)) {
							MTLog.log("%s: Trip headsign string '%s' non descriptive! (%s)", this.routeId, mTrip.getHeadsignValue(), mTrip);
							MTLog.log("%s: trip headsigns: %s", this.routeId, mTripHeadsignStrings);
							MTLog.log("%s: trips: %s", this.routeId, mTrips);
							throw new MTLog.Fatal("");
						}
						MTLog.log("%s: Keeping non-descriptive trip headsign '%s' (%s)", this.routeId, mTrip.getHeadsignValue(), mTrip);
						tripKeptNonDescriptiveHeadsign = true; // last trip that can keep same head sign
					}
				}
			}
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING //
						&& StringUtils.isEmpty(mTrip.getHeadsignValue())) {
					MTLog.log("%s: Trip headsign string '%s' non descriptive! (%s)", this.routeId, mTrip.getHeadsignValue(), mTrip);
					MTLog.log("%s: %s", this.routeId, mTripHeadsignStrings);
					MTLog.log("%s: %s", this.routeId, mTrips);
					throw new MTLog.Fatal("");
				}
			}
		}
		for (ArrayList<MTripStop> mTripStops : tripIdToMTripStops.values()) {
			setMTripStopSequence(mTripStops);
			for (MTripStop mTripStop : mTripStops) {
				if (allMTripStops.containsKey(mTripStop.getUID())
						&& !allMTripStops.get(mTripStop.getUID()).equals(mTripStop)) {
					MTLog.log("%s: Different trip stop '%s' already in route list (%s != %s)!", this.routeId, mTripStop.getUID(), mTripStop.toString(),
							allMTripStops.get(mTripStop.getUID()).toString());
					continue;
				}
				allMTripStops.put(mTripStop.getUID(), mTripStop);
				tripStopIds.add(mTripStop.getStopId());
			}
		}
		fixRouteLongName(mRoutes, mTrips);
	}

	private void fixRouteLongName(HashMap<Long, MRoute> mRoutes, HashMap<Long, MTrip> mTrips) {
		if (!this.agencyTools.defaultRouteLongNameEnabled()) {
			return;
		}
		for (MRoute mRoute : mRoutes.values()) {
			if (!mRoute.getLongName().isEmpty()) {
				continue;
			}
			StringBuilder sb = new StringBuilder();
			for (MTrip mTrip : mTrips.values()) {
				if (mTrip.getRouteId() == mRoute.getId()) {
					if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
						if (sb.length() > 0) {
							sb.append(" <> ");
						}
						sb.append(mTrip.getHeadsignValue());
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

	private void parseTrips(HashMap<String, MSchedule> mSchedules,
							HashMap<String, MFrequency> mFrequencies,
							HashMap<Long, MTrip> mTrips,
							HashMap<Integer, MStop> mStops,
							HashSet<Integer> serviceIdInts,
							MRoute mRoute,
							HashMap<Long, String> mTripStopTimesHeadsign,
							HashMap<Long, ArrayList<MTripStop>> tripIdToMTripStops,
							GRoute gRoute,
							Map<Integer, String> gDirectionHeadSigns,
							GSpec routeGTFS) {
		boolean mergeSuccessful;
		HashMap<Long, HashSet<String>> mergedTripIdToMTripStops = new HashMap<>();
		HashMap<Long, Pair<Integer, String>> originalTripHeadsign;
		ArrayList<MTrip> splitTrips;
		HashMap<Long, HashMap<Long, MTripStop>> splitTripStops;
		ArrayList<MTripStop> cTripStopsList;
		ArrayList<MTripStop> mTripStopsList;
		Integer tripServiceIdInt;
		HashMap<Long, String> splitTripStopTimesHeadsign;
		final List<GTrip> gRouteTrips = GTrip.longestFirst(
				routeGTFS.getRouteTrips(gRoute.getRouteIdInt()),
				this.routeTripIdTripStops::get
		);
		//noinspection deprecation
		final String gRouteId = gRoute.getRouteId();
		MTLog.log("%s: parsing %d trips for route ID '%s'... ", this.routeId, gRouteTrips.size(), gRouteId);
		int g = 0;
		for (GTrip gTrip : gRouteTrips) {
			if (gTrip.getRouteIdInt() != gRoute.getRouteIdInt()) {
				throw new MTLog.Fatal("%s: Should not happen!", this.routeId); // continue; // SKIP
			}
			splitTrips = new ArrayList<>(Collections.singleton(new MTrip(mRoute.getId())));
			originalTripHeadsign = new HashMap<>();
			for (MTrip mTrip : splitTrips) {
				this.agencyTools.setTripHeadsign(mRoute, mTrip, gTrip, routeGTFS);
				final Pair<Integer, String> originalTripHeadSignTypeAndValue = new Pair<>(mTrip.getHeadsignType(), mTrip.getHeadsignValue());
				long originalTripHeadSignId = mTrip.getId();
				if (splitTrips.size() == 1 // not split-ed
						&& gDirectionHeadSigns != null) {
					final String directionHeadSign = gDirectionHeadSigns.get(gTrip.getDirectionIdOrDefault());
					if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING) {
						if (directionHeadSign != null && !directionHeadSign.isEmpty()) {
							mTrip.setHeadsignString(directionHeadSign, mTrip.getHeadsignId());
							originalTripHeadSignId = mTrip.getId();
						}
					}
					if (this.agencyTools.getDirectionTypes().contains(MTrip.HEADSIGN_TYPE_DIRECTION)) {
						final MDirectionType direction = this.agencyTools.convertDirection(directionHeadSign);
						if (direction != null) {
							mTrip.setHeadsignDirection(direction);
							originalTripHeadSignId = mTrip.getId();
						}
					}
				}
				originalTripHeadsign.put(originalTripHeadSignId, originalTripHeadSignTypeAndValue);
			}
			for (MTrip mTrip : splitTrips) {
				final MTrip currentTrip = mTrips.get(mTrip.getId());
				if (currentTrip != null && !currentTrip.equals(mTrip)) {
					mergeSuccessful = false;
					if (mTrip.equalsExceptHeadsignValue(currentTrip)) {
						mergeSuccessful = this.agencyTools.mergeHeadsign(mTrip, currentTrip);
					}
					if (!mergeSuccessful) {
						throw new MTLog.Fatal("%s: Different trip %s already in list (%s != %s)", this.routeId, mTrip.getId(), mTrip, currentTrip);
					}
				}
			}
			tripServiceIdInt = gTrip.getServiceIdInt();
			parseFrequencies(mFrequencies, gTrip, splitTrips, tripServiceIdInt, routeGTFS);
			splitTripStops = new HashMap<>();
			splitTripStopTimesHeadsign = parseTripStops(
					mSchedules,
					serviceIdInts,
					mStops,
					gRoute,
					gTrip,
					originalTripHeadsign,
					splitTrips,
					tripServiceIdInt,
					splitTripStops,
					routeGTFS
			);
			for (MTrip mTrip : splitTrips) {
				if (!splitTripStops.containsKey(mTrip.getId())) {
					continue;
				}
				mTripStopsList = new ArrayList<>(splitTripStops.get(mTrip.getId()).values());
				Collections.sort(mTripStopsList);
				setMTripStopSequence(mTripStopsList);
				String mTripStopListString = mTripStopsList.toString();
				if (mergedTripIdToMTripStops.containsKey(mTrip.getId())
						&& mergedTripIdToMTripStops.get(mTrip.getId()).contains(mTripStopListString)) {
					continue;
				}
				if (tripIdToMTripStops.containsKey(mTrip.getId())) {
					cTripStopsList = tripIdToMTripStops.get(mTrip.getId());
					if (!equalsMyTripStopLists(mTripStopsList, cTripStopsList)) {
						MTLog.log("%s: Need to merge trip ID '%s'.", this.routeId, mTrip.getId());
						tripIdToMTripStops.put(mTrip.getId(),
								setMTripStopSequence(
										mergeMyTripStopLists(mTripStopsList, cTripStopsList)
								)
						);
					}
				} else { // just use it
					tripIdToMTripStops.put(mTrip.getId(), mTripStopsList);
				}
				if (!mergedTripIdToMTripStops.containsKey(mTrip.getId())) {
					mergedTripIdToMTripStops.put(mTrip.getId(), new HashSet<>());
				}
				mergedTripIdToMTripStops.get(mTrip.getId()).add(mTripStopListString);
			}
			for (MTrip mTrip : splitTrips) {
				if (!splitTripStops.containsKey(mTrip.getId())) {
					continue;
				}
				String tripStopTimesHeadsign = splitTripStopTimesHeadsign.get(mTrip.getId());
				if (mTrip.getHeadsignType() == MTrip.HEADSIGN_TYPE_STRING
						&& tripStopTimesHeadsign != null
						&& tripStopTimesHeadsign.length() > 0) {
					if (mTripStopTimesHeadsign.containsKey(mTrip.getId())) {
						if (!mTripStopTimesHeadsign.get(mTrip.getId()).equals(tripStopTimesHeadsign)) {
							if (!mTripStopTimesHeadsign.get(mTrip.getId()).contains(tripStopTimesHeadsign)) {
								MTLog.log("%s: Trip stop times head-sign different for same trip ID ('%s'!='%s')", this.routeId,
										tripStopTimesHeadsign, mTripStopTimesHeadsign.get(mTrip.getId()));
							}
							mTripStopTimesHeadsign.put(mTrip.getId(),
									MTrip.mergeHeadsignValue(
											mTripStopTimesHeadsign.get(mTrip.getId()),
											tripStopTimesHeadsign));
						}
					} else {
						mTripStopTimesHeadsign.put(mTrip.getId(), tripStopTimesHeadsign);
					}
				}
			}
			for (MTrip mTrip : splitTrips) {
				if (!splitTripStops.containsKey(mTrip.getId())) {
					continue;
				}
				mTrips.put(mTrip.getId(), mTrip);
			}
			if (g++ % 10 == 0) { // LOG
				MTLog.logPOINT(); // LOG
			} // LOG
		}
		MTLog.log("%s: parsing %d trips for route ID '%s'... DONE", this.routeId, gRouteTrips.size(), gRouteId);
	}

	private HashMap<Long, String> parseTripStops(HashMap<String, MSchedule> mSchedules,
												 HashSet<Integer> serviceIdInts,
												 HashMap<Integer, MStop> mStops,
												 GRoute gRoute,
												 GTrip gTrip,
												 HashMap<Long, Pair<Integer, String>> originalTripHeadsign,
												 ArrayList<MTrip> splitTrips,
												 Integer tripServiceIdInt,
												 HashMap<Long, HashMap<Long, MTripStop>> splitTripStops,
												 GSpec routeGTFS) {
		HashMap<Long, String> splitTripStopTimesHeadSign = new HashMap<>();
		int mStopId;
		GStop gStop;
		MTripStop mTripStop;
		long mTripId;
		String tripStopTimesHeadsign;
		Pair<Long[], Integer[]> mTripsAndStopSequences;
		HashMap<String, Integer> addedMTripIdAndGStopIds = new HashMap<>();
		final List<GTripStop> tripStops = this.routeTripIdTripStops.get(gTrip.getTripIdInt());
		if (tripStops != null) {
			for (GTripStop gTripStop : tripStops) {
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
					throw new MTLog.Fatal("%s: Can't find GTFS stop ID (%s) '%s' from trip ID '%s' (%s)", this.routeId, mStopId, gTripStop.getStopIdInt(),
							gTripStop.getTripIdInt(), gStop);
				}
				mTripsAndStopSequences = new Pair<>(
						new Long[]{splitTrips.get(0).getId()},
						new Integer[]{gTripStop.getStopSequence()}
				);
				for (int i = 0; i < mTripsAndStopSequences.first.length; i++) {
					mTripId = mTripsAndStopSequences.first[i];
					mTripStop = new MTripStop(mTripId, mStopId, mTripsAndStopSequences.second[i]);
					if (!splitTripStops.containsKey(mTripId)) {
						splitTripStops.put(mTripId, new HashMap<>());
					}
					if (splitTripStops.get(mTripId).containsKey(mTripStop.getUID())) {
						if (!splitTripStops.get(mTripId).get(mTripStop.getUID()).equalsExceptStopSequence(mTripStop)) {
							throw new MTLog.Fatal("%s: Different slit trip stop '%s' already in list (%s != %s)!",
									this.routeId,
									mTripStop.getUID(),
									mTripStop.toString(),
									splitTripStops.get(mTripId).get(mTripStop.getUID()).toString());
						}
					} else {
						splitTripStops.get(mTripId).put(mTripStop.getUID(), mTripStop);
					}
					tripStopTimesHeadsign = splitTripStopTimesHeadSign.get(mTripId);
					if (!originalTripHeadsign.containsKey(mTripId)) {
						throw new MTLog.Fatal("%s: Unexpected trip head-sign ID '%s'! (%s)", this.routeId, mTripId, originalTripHeadsign);
					}
					tripStopTimesHeadsign = parseStopTimes(
							mSchedules,
							mTripId,
							tripServiceIdInt,
							originalTripHeadsign.get(mTripId).first,
							originalTripHeadsign.get(mTripId).second,
							tripStopTimesHeadsign,
							gRoute,
							gTrip,
							gTripStop,
							mStopId,
							addedMTripIdAndGStopIds
					);
					splitTripStopTimesHeadSign.put(mTripId, tripStopTimesHeadsign);
					serviceIdInts.add(tripServiceIdInt);
				}
				if (!mStops.containsKey(mStopId)) {
					mStops.put(
							mStopId,
							new MStop(mStopId,
									this.agencyTools.getStopCode(gStop),
									this.agencyTools.getStopOriginalId(gStop),
									this.agencyTools.cleanStopName(gStop.getStopName()),
									gStop.getStopLat(),
									gStop.getStopLong()
							));
				}
			}
		}
		return splitTripStopTimesHeadSign;
	}

	private final SimpleDateFormat M_TIME_FORMAT = MSpec.getNewTimeFormatInstance(); // not static - not sharable between threads!

	private String parseStopTimes(HashMap<String, MSchedule> mSchedules,
								  long mTripId,
								  Integer tripServiceIdInt,
								  int originalTripHeadsignType,
								  @Nullable String originalTripHeadsignValue,
								  String tripStopTimesHeadsign,
								  @NotNull GRoute gRoute,
								  @NotNull GTrip gTrip,
								  @NotNull GTripStop gTripStop,
								  int mStopId,
								  HashMap<String, Integer> addedMTripIdAndGStopIds) {
		MSchedule mSchedule;
		String stopHeadsign;
		boolean descentOnly;
		String tripIdStopId;
		List<GStopTime> gStopTimes = routeTripIdStopTimes.get(gTripStop.getTripIdInt());
		int lastStopSequence = -1;
		GStopTime lastStopTime = null;
		for (int i = 0; i < gStopTimes.size(); i++) {
			GStopTime gStopTime = gStopTimes.get(i);
			if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
				if (gStopTime.getStopSequence() < lastStopSequence) {
					MTLog.log("%s: Stop sequence out of order (%s => '%s')!", this.routeId, lastStopSequence, gStopTime);
					throw new MTLog.Fatal("%s: Stop sequence out of order ([%s] => [%s])!", this.routeId, lastStopTime, gStopTime);
				}
			}
			lastStopSequence = gStopTime.getStopSequence();
			lastStopTime = gStopTime;
			descentOnly = false;
			if (gStopTime.getTripIdInt() != gTripStop.getTripIdInt() //
					|| gStopTime.getStopIdInt() != gTripStop.getStopIdInt() //
					|| gStopTime.getStopSequence() != gTripStop.getStopSequence()) {
				continue;
			}
			if (gStopTime.getPickupType() == GPickupType.NO_PICKUP.ordinal() //
					|| i == gStopTimes.size() - 1) { // last stop of the trip
				descentOnly = true;
			}
			tripIdStopId = String.valueOf(mTripId) + gStopTime.getTripIdInt() + gStopTime.getStopIdInt();
			if (descentOnly) {
				if (addedMTripIdAndGStopIds.containsKey(tripIdStopId) //
						&& addedMTripIdAndGStopIds.get(tripIdStopId) != gStopTime.getStopSequence()) {
					if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
						// TODO later, when UI can display multiple times same stop/POI & schedules are affected to a specific sequence, keep both
					} else {
						MTLog.logDebug("%s: parseStopTimes() > SKIP same stop & descent only %s.", this.routeId, gStopTime.toStringPlus());
						continue;
					}
				}
			}
			mSchedule = new MSchedule(
					this.routeId,
					tripServiceIdInt,
					mTripId,
					mStopId,
					this.agencyTools.getTimes(
							gStopTime,
							this.routeTripIdStopTimes.get(gStopTime.getTripIdInt()),
							M_TIME_FORMAT
					),
					gStopTime.getTripIdInt()
			);
			if (mSchedules.containsKey(mSchedule.getUID()) //
					&& !mSchedules.get(mSchedule.getUID()).isSameServiceRTSDeparture(mSchedule)) {
				throw new MTLog.Fatal("%s: Different schedule %s (%s) already in list (%s != %s)!",
						this.routeId,
						mSchedule.getUID(),
						mSchedules.get(mSchedule.getUID()).getUID(),
						mSchedule.toStringPlus(),
						mSchedules.get(mSchedule.getUID()).toStringPlus());
			}
			if (DefaultAgencyTools.EXPORT_DESCENT_ONLY //
					&& descentOnly) {
				mSchedule.setHeadsign(MTrip.HEADSIGN_TYPE_DESCENT_ONLY, null);
			} else if (gStopTime.hasStopHeadsign()) {
				stopHeadsign = this.agencyTools.cleanStopHeadSign(gRoute, gTrip, gStopTime, gStopTime.getStopHeadsignOrDefault());
				mSchedule.setHeadsign(MTrip.HEADSIGN_TYPE_STRING, stopHeadsign);
				tripStopTimesHeadsign = setTripStopTimesHeadsign(tripStopTimesHeadsign, stopHeadsign);
			} else {
				if (!StringUtils.isBlank(originalTripHeadsignValue)) {
					mSchedule.setHeadsign(originalTripHeadsignType, originalTripHeadsignValue);
				}
			}
			mSchedules.put(mSchedule.getUID(), mSchedule);
			addedMTripIdAndGStopIds.put(tripIdStopId, gStopTime.getStopSequence());
		}
		return tripStopTimesHeadsign;
	}

	private static String setTripStopTimesHeadsign(String tripStopTimesHeadsign, String stopHeadSign) {
		if (tripStopTimesHeadsign == null) {
			tripStopTimesHeadsign = stopHeadSign;
		} else if (Constants.EMPTY.equals(tripStopTimesHeadsign)) { // disabled
			// nothing to do
		} else if (!tripStopTimesHeadsign.equals(stopHeadSign)) {
			tripStopTimesHeadsign = Constants.EMPTY; // disable
		}
		return tripStopTimesHeadsign;
	}

	private void parseFrequencies(HashMap<String, MFrequency> mFrequencies,
								  GTrip gTrip,
								  ArrayList<MTrip> splitTrips,
								  Integer tripServiceIdInt,
								  GSpec routeGTFS) {
		MFrequency mFrequency;
		for (GFrequency gFrequency : routeGTFS.getFrequencies(gTrip.getTripIdInt())) {
			if (gFrequency.getTripIdInt() != gTrip.getTripIdInt()) {
				continue;
			}
			for (MTrip mTrip : splitTrips) {
				mFrequency = new MFrequency(
						tripServiceIdInt,
						mTrip.getId(),
						this.agencyTools.getStartTime(gFrequency),
						this.agencyTools.getEndTime(gFrequency),
						gFrequency.getHeadwaySecs()
				);
				if (mFrequencies.containsKey(mFrequency.getUID()) && !mFrequencies.get(mFrequency.getUID()).equals(mFrequency)) {
					throw new MTLog.Fatal("%s: Different frequency %s already in list (%s != %s)\n!", this.routeId, mFrequency.getUID(), mFrequency.toString(),
							mFrequencies.get(mFrequency.getUID()).toString());
				}
				mFrequencies.put(mFrequency.getUID(), mFrequency);
			}
		}
	}

	private void setTripStopDescentOnly(@NotNull ArrayList<MTripStop> mTripStopsList,
										@NotNull Collection<MSchedule> mSchedules) {
		if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
			for (MTripStop tripStop : mTripStopsList) {
				boolean isDescentOnly = false;
				for (MSchedule schedule : mSchedules) {
					if (schedule.getTripId() != tripStop.getTripId()) {
						continue;
					}
					if (schedule.getStopId() != tripStop.getStopId()) {
						continue;
					}
					if (schedule.isDescentOnly()) {
						isDescentOnly = true;
						//noinspection UnnecessaryContinue
						continue;
					} else {
						isDescentOnly = false;
						break;
					}
				}
				tripStop.setDescentOnly(isDescentOnly);
			}
			return; // SKIP (descent only set on the stop time schedule level
		}
		if (mTripStopsList.size() == 0) {
			return;
		}
		int i = mTripStopsList.size() - 1; // starting with last
		MTripStop currentTripStop;
		long currentTripId = -1;
		do {
			currentTripStop = mTripStopsList.get(i);
			if (currentTripStop.getTripId() != currentTripId) {
				currentTripStop.setDescentOnly(true);
			} // ELSE false == default
			currentTripId = currentTripStop.getTripId();
			i--; // previous
		} while (i >= 0);
	}

	private static boolean equalsMyTripStopLists(ArrayList<MTripStop> l1, ArrayList<MTripStop> l2) {
		if (l1 == null && l2 == null) {
			return true;
		}
		int s1 = l1 == null ? 0 : l1.size();
		int s2 = l2 == null ? 0 : l2.size();
		if (s1 != s2) {
			return false;
		}
		for (int i = 0; i < s1; i++) {
			if (!l1.get(i).equals(l2.get(i))) {
				return false;
			}
		}
		return true;
	}

	private ArrayList<MTripStop> mergeMyTripStopLists(ArrayList<MTripStop> list1, ArrayList<MTripStop> list2) {
		ArrayList<MTripStop> newList = new ArrayList<>();
		HashSet<Integer> newListStopIds = new HashSet<>();
		HashSet<Integer> list1StopIds = new HashSet<>();
		for (MTripStop ts1 : list1) {
			list1StopIds.add(ts1.getStopId());
		}
		HashSet<Integer> list2StopIds = new HashSet<>();
		for (MTripStop ts2 : list2) {
			list2StopIds.add(ts2.getStopId());
		}
		MTripStop ts1, ts2;
		boolean inL1, inL2;
		boolean lastInL1, lastInL2;
		GStop lastGStop, ts1GStop, ts2GStop;
		GStop commonGStop, previousTs1GStop, previousTs2GStop;
		double ts1Distance, ts2Distance;
		double previousTs1Distance, previousTs2Distance;
		MTripStop[] commonStopAndPrevious;
		int i1 = 0, i2 = 0;
		MTripStop last = null;
		//noinspection ForLoopReplaceableByWhile
		for (; i1 < list1.size() && i2 < list2.size(); ) {
			ts1 = list1.get(i1);
			ts2 = list2.get(i2);
			if (newListStopIds.contains(ts1.getStopId())) {
				MTLog.logDebug("%s: Skipped %s because already in the merged list (1).", this.routeId, ts1.toString());
				i1++; // skip this stop because already in the merged list
				continue;
			}
			if (newListStopIds.contains(ts2.getStopId())) {
				MTLog.logDebug("%s: Skipped %s because already in the merged list (2).", this.routeId, ts2.toString());
				i2++; // skip this stop because already in the merged list
				continue;
			}
			if (ts1.getStopId() == ts2.getStopId()) {
				// TODO merge other parameters such as drop off / pick up ...
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				i2++;
				continue;
			}
			// find next match
			// look for stop in other list
			inL1 = list1StopIds.contains(ts2.getStopId());
			inL2 = list2StopIds.contains(ts1.getStopId());
			if (inL1 && !inL2) {
				newList.add(ts1);
				newListStopIds.add(ts1.getStopId());
				last = ts1;
				i1++;
				continue;
			}
			if (!inL1 && inL2) {
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
					MTLog.log("" + this.routeId
							+ ": pick t:" + ts1.getTripId() + ">s:" + ts1.getStopId()
							+ " in same list w/ s:" + last.getStopId()
							+ " instead of t:" + ts2.getTripId() + ">s:" + ts2.getStopId());
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
					continue;
				}
				if (!lastInL1 && lastInL2) {
					MTLog.log("" + this.routeId
							+ ": pick t:" + ts2.getTripId() + ">s:" + ts2.getStopId()
							+ " in same list w/ s:" + last.getStopId()
							+ " instead of t:" + ts1.getTripId() + ">s:" + ts1.getStopId());
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
					MTLog.log("" + this.routeId
							+ ": pick t:" + ts1.getTripId() + ">" + ts1GStop.toStringPlus()
							+ " closest (" + (ts2Distance - ts1Distance) + ") to " + lastGStop.toStringPlus()
							+ " instead of t:" + ts2.getTripId() + ">" + ts2GStop.toStringPlus());
					newList.add(ts1);
					newListStopIds.add(ts1.getStopId());
					last = ts1;
					i1++;
				} else {
					MTLog.log("" + this.routeId
							+ ": pick t:" + ts2.getTripId() + ">" + ts2GStop.toStringPlus()
							+ " closest (" + (ts1Distance - ts2Distance) + ") to " + lastGStop.toStringPlus()
							+ " instead of t:" + ts1.getTripId() + ">" + ts1GStop.toStringPlus());
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
				MTLog.log("" + this.routeId + ": Resolved using 1st common stop trip ID:" + ts1.getTripId() + ", stop IDs:"
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
			//noinspection UnnecessaryContinue
			continue;
		}
		// add remaining stops
		//noinspection ForLoopReplaceableByWhile
		for (; i1 < list1.size(); ) {
			newList.add(list1.get(i1++));
		}
		//noinspection ForLoopReplaceableByWhile
		for (; i2 < list2.size(); ) {
			newList.add(list2.get(i2++));
		}
		return newList;
	}

	@Nullable
	private ArrayList<MTripStop> setMTripStopSequence(@Nullable ArrayList<MTripStop> mTripStops) {
		if (mTripStops != null) {
			for (int i = 0; i < mTripStops.size(); i++) {
				mTripStops.get(i).setStopSequence(i + 1);
			}
		}
		return mTripStops;
	}

	private MTripStop[] findFirstCommonStop(ArrayList<MTripStop> l1, ArrayList<MTripStop> l2) {
		MTripStop previousTs1 = null;
		MTripStop previousTs2;
		MTripStop[] commonStopAndPrevious;
		for (MTripStop tts1 : l1) {
			previousTs2 = null;
			for (MTripStop tts2 : l2) {
				if (tts1.getStopId() == tts2.getStopId()) {
					if (previousTs1 == null || previousTs2 == null) {
						MTLog.log("" + this.routeId + ": findFirstCommonStop() > Common stop found '" + tts1.getStopId()
								+ "' but no previous stop! Looking for next common stop...");
					} else {
						commonStopAndPrevious = new MTripStop[3];
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
		return new MTripStop[0];
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
			initialBearing *= 180.0 / Math.PI;
			results[1] = initialBearing;
			if (results.length > 2) {
				float finalBearing = (float) Math.atan2(cosU1 * sinLambda, -sinU1 * cosU2 + cosU1 * sinU2 * cosLambda);
				finalBearing *= 180.0 / Math.PI;
				results[2] = finalBearing;
			}
		}
	}
}
