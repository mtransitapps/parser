package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

// https://developers.google.com/transit/gtfs/reference#stop_times_drop_off_type_field
enum class GDropOffType(val id: Int) {

    REGULAR(0),
    NO_DROP_OFF(1),
    MUST_PHONE_AGENCY(2),
    MUST_COORDINATE_WITH_DRIVER(3);

    companion object {

        fun parse(id: Int?): GDropOffType {
            if (REGULAR.id == id) {
                return REGULAR
            }
            if (NO_DROP_OFF.id == id) {
                return NO_DROP_OFF
            }
            if (MUST_PHONE_AGENCY.id == id) {
                return MUST_PHONE_AGENCY
            }
            return if (MUST_COORDINATE_WITH_DRIVER.id == id) {
                MUST_COORDINATE_WITH_DRIVER
            } else REGULAR  // default
        }

        @JvmStatic
        fun parse(id: String?): GDropOffType {
            return if (id.isNullOrEmpty()) { // that's OK
                REGULAR // default
            } else try {
                parse(id.toInt())
            } catch (nfe: NumberFormatException) {
                throw MTLog.Fatal(nfe, "Error while parsing '$id' as drop off type!")
            }
        }
    }
}