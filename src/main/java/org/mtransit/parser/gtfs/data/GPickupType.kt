package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

// https://developers.google.com/transit/gtfs/reference#stop_times_pickup_type_field
enum class GPickupType(val id: Int) {

    REGULAR(0),
    NO_PICKUP(1),
    MUST_PHONE_AGENCY(2),
    MUST_COORDINATE_WITH_DRIVER(3);

    companion object {
        fun parse(id: Int): GPickupType {
            if (REGULAR.id == id) {
                return REGULAR
            }
            if (NO_PICKUP.id == id) {
                return NO_PICKUP
            }
            if (MUST_PHONE_AGENCY.id == id) {
                return MUST_PHONE_AGENCY
            }
            return if (MUST_COORDINATE_WITH_DRIVER.id == id) {
                MUST_COORDINATE_WITH_DRIVER
            } else REGULAR // default
        }

        @JvmStatic
        fun parse(id: String?): GPickupType {
            return if (id.isNullOrEmpty()) { // no pickup info, that's OK
                REGULAR // default
            } else try {
                parse(id.toInt())
            } catch (nfe: NumberFormatException) {
                throw MTLog.Fatal(
                    nfe,
                    "Error while parsing '\$id' as pickup type!"
                )
            }
        }
    }
}