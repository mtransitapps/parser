package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#frequencies_fields
data class GFrequency(
    val tripId: String,
    val startTime: String,
    val endTime: String,
    val headwaySecs: Int
) {

    companion object {
        const val FILENAME = "frequencies.txt"

        val DEFAULT_PICKUP_TYPE = GPickupType.REGULAR.intValue() // Regularly scheduled pickup
        val DEFAULT_DROP_OFF_TYPE = GDropOffType.REGULAR.intValue() // Regularly scheduled drop off
        const val TRIP_ID = "trip_id"
        const val START_TIME = "start_time"
        const val END_TIME = "end_time"
        const val HEADWAY_SECS = "headway_secs"
    }
}