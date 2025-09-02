package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.StringUtils.EMPTY
import org.mtransit.commons.gtfs.data.Stop
import org.mtransit.commons.gtfs.data.StopId
import org.mtransit.parser.Constants
import org.mtransit.parser.LocationUtils
import org.mtransit.parser.MTLog
import org.mtransit.parser.gtfs.GAgencyTools

// https://developers.google.com/transit/gtfs/reference#stops_fields
// https://gtfs.org/schedule/reference/#stopstxt
data class GStop(
    val stopIdInt: Int,
    val stopName: String,
    val stopLat: Double,
    val stopLong: Double,
    val stopCode: String,
    val locationType: GLocationType,
    val parentStationIdInt: Int?,
    var wheelchairBoarding: GWheelchairBoardingType,
) {

    constructor(
        stopId: StopId,
        stopName: String,
        stopLat: Double,
        stopLong: Double,
        stopCode: String,
        locationType: Int?,
        parentStationId: StopId?,
        wheelchairBoarding: Int?,
    ) : this(
        GIDs.getInt(stopId),
        stopName,
        stopLat,
        stopLong,
        stopCode,
        GLocationType.parse(locationType),
        parentStationId?.let { GIDs.getInt(it) },
        GWheelchairBoardingType.parse(wheelchairBoarding),
    )

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val stopId = _stopId

    private val _stopId: StopId
        get() {
            return GIDs.getString(stopIdInt)
        }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val parentStationId = _parentStationId

    private val _parentStationId: StopId?
        get() {
            return parentStationIdInt?.let { GIDs.getString(it) }
        }

    @JvmOverloads
    @Suppress("unused")
    fun toStringPlus(debug: Boolean = Constants.DEBUG): String {
        return if (debug) { // longer
            return toString() +
                    "+(stopId:$_stopId)" +
                    "+(parent:$_parentStationId)"
        } else { // shorter #CI
            "{s:$_stopId${
                if (stopCode.isNotBlank() && stopCode != _stopId) {
                    ",c:$stopCode"
                } else {
                    ""
                }
            }}"
        }
    }

    fun considerEqual(other: GStop) : Boolean {
        if (stopIdInt != other.stopIdInt) {
            return false
        }
        if (stopCode != other.stopCode) {
            return false
        }
        if (stopName != other.stopName) {
            return false
        }
        if (locationType != other.locationType) {
            return false
        }
        if (parentStationIdInt != other.parentStationIdInt) {
            return false
        }
        if (wheelchairBoarding != other.wheelchairBoarding) {
            return false
        }
        if (stopLat == other.stopLat && stopLong == other.stopLong) {
            return true
        }
        val distanceInMeters = LocationUtils.findDistance(
            stopLat, stopLong,
            other.stopLat, other.stopLong
        )
        return distanceInMeters < 100.0f
    }

    fun to() = Stop(
        stopId = _stopId,
        stopCode = stopCode,
        stopName = stopName,
        stopLat = stopLat,
        stopLon = stopLong,
        stopUrl = null, // TODO
        locationType = locationType.id,
        parentStationId = _parentStationId,
        wheelchairBoarding = wheelchairBoarding.id,
    )

    companion object {
        const val FILENAME = "stops.txt"

        internal const val STOP_ID = "stop_id"
        private const val STOP_NAME = "stop_name"
        private const val STOP_LAT = "stop_lat"
        private const val STOP_LON = "stop_lon"
        private const val STOP_CODE = "stop_code"
        private const val LOCATION_TYPE = "location_type"
        private const val PARENT_STATION = "parent_station"
        private const val WHEELCHAIR_BOARDING = "wheelchair_boarding"

        @JvmStatic
        fun fromLine(line: Map<String, String>, agencyTools: GAgencyTools) = GStop(
            stopId = line[STOP_ID]?.trim()
                ?.let { agencyTools.cleanStopOriginalId(it) }
                ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            stopName = line[STOP_NAME] ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            stopLat = line[STOP_LAT]?.toDouble() ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            stopLong = line[STOP_LON]?.toDouble() ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            stopCode = line[STOP_CODE]?.trim() ?: EMPTY,
            locationType = line[LOCATION_TYPE]?.takeIf { it.isNotBlank() }?.toInt(),
            parentStationId = line[PARENT_STATION],
            wheelchairBoarding = line[WHEELCHAIR_BOARDING]?.takeIf { it.isNotBlank() }?.toInt(),
        )

        @JvmStatic
        fun from(stops: Collection<Stop>) = stops.mapNotNull { from(it) }

        @JvmStatic
        fun from(stop: Stop?) = stop?.let {
            GStop(
                stopId = it.stopId,
                stopName = it.stopName,
                stopLat = it.stopLat,
                stopLong = it.stopLon,
                stopCode = it.stopCode ?: EMPTY,
                locationType = it.locationType,
                parentStationId = it.parentStationId,
                wheelchairBoarding = it.wheelchairBoarding,
            )
        }
    }
}