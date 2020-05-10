package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#stops_fields
data class GStop(
    val stopIdInt: Int,
    val stopName: String,
    val stopLat: Double,
    val stopLong: Double,
    val stopCode: String
) {

    constructor(
        stopId: String,
        stopName: String,
        stopLat: Double,
        stopLong: Double,
        stopCode: String
    ) : this(
        GIDs.getInt(stopId),
        stopName,
        stopLat,
        stopLong,
        stopCode
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val stopId = _stopId

    private val _stopId: String
        get() {
            return GIDs.getString(stopIdInt)
        }

    companion object {
        const val FILENAME = "stops.txt"

        const val STOP_ID = "stop_id"
        const val STOP_NAME = "stop_name"
        const val STOP_LAT = "stop_lat"
        const val STOP_LON = "stop_lon"
        const val STOP_CODE = "stop_code"
        const val LOCATION_TYPE = "location_type"
    }
}