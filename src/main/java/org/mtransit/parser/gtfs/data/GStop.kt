package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.StringUtils.EMPTY
import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog

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
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLong: Double,
        stopCode: String,
        locationType: Int?,
        parentStationId: String?,
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

    private val _stopId: String
        get() {
            return GIDs.getString(stopIdInt)
        }

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    val parentStationId = _parentStationId

    private val _parentStationId: String?
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

    companion object {
        const val FILENAME = "stops.txt"

        private const val STOP_ID = "stop_id"
        private const val STOP_NAME = "stop_name"
        private const val STOP_LAT = "stop_lat"
        private const val STOP_LON = "stop_lon"
        private const val STOP_CODE = "stop_code"
        private const val LOCATION_TYPE = "location_type"
        private const val PARENT_STATION = "parent_station"
        private const val WHEELCHAIR_BOARDING = "wheelchair_boarding"

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GStop(
            line[STOP_ID] ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            line[STOP_NAME] ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            line[STOP_LAT]?.toDouble() ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            line[STOP_LON]?.toDouble() ?: throw MTLog.Fatal("Invalid GStop from $line!"),
            line[STOP_CODE]?.trim() ?: EMPTY,
            line[LOCATION_TYPE]?.takeIf { it.isNotBlank() }?.toInt(),
            line[PARENT_STATION],
            line[WHEELCHAIR_BOARDING]?.takeIf { it.isNotBlank() }?.toInt(),
        )
    }
}