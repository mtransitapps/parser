package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#stop_timestxt
// http://gtfs.org/reference/static#stop_timestxt
// -_trip_id field
// - stop_id field
data class GTripStop(
    val uID: String,
    val tripId: String,
    val stopId: String,
    val stopSequence: Int
) {

    companion object {
        const val UID = "uid"
        const val TRIP_ID = "trip_id"
        const val STOP_ID = "stop_id"
        const val STOP_SEQUENCE = "stop_sequence"

        @JvmStatic
        fun getNewUID(
            trip_uid: String,
            stop_id: String,
            stop_sequence: Int
        ): String {
            return stop_id + Constants.UUID_SEPARATOR + stop_sequence + Constants.UUID_SEPARATOR + trip_uid
        }
    }
}