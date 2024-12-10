package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

// https://gtfs.org/schedule/reference/#stop_timestxt
// https://developers.google.com/transit/gtfs/reference#stop_timestxt
enum class GTimePoint(val id: Int) {

    APPROXIMATE(0),
    EXACT(1);

    companion object {

        fun parse(id: Int?): GTimePoint {
            return when {
                APPROXIMATE.id == id -> APPROXIMATE
                EXACT.id == id -> EXACT
                else -> EXACT // default
            }
        }

        @JvmStatic
        fun parse(id: String?): GTimePoint {
            return if (id == null || id.isEmpty()) { // that's OK
                EXACT // default
            } else try {
                parse(id.toInt())
            } catch (nfe: NumberFormatException) {
                throw MTLog.Fatal(nfe, "Error while parsing '$id' as time point!")
            }
        }
    }
}