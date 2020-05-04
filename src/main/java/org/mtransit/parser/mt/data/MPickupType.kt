package org.mtransit.parser.mt.data

import org.mtransit.parser.MTLog

enum class MPickupType(val id: Int) {

    REGULAR(0),
    NO_PICKUP(1),
    MUST_PHONE_AGENCY(2),
    MUST_COORDINATE_WITH_DRIVER(3);

    @Suppress("unused")
    fun toFile(): String {
        return id.toString()
    }

    companion object {

        fun parse(id: Int): MPickupType {
            return when {
                REGULAR.id == id -> REGULAR
                NO_PICKUP.id == id -> NO_PICKUP
                MUST_PHONE_AGENCY.id == id -> MUST_PHONE_AGENCY
                MUST_COORDINATE_WITH_DRIVER.id == id -> MUST_COORDINATE_WITH_DRIVER
                else -> REGULAR  // default
            }
        }

        fun parse(id: String?): MPickupType {
            return if (id == null) { // no pickup info, that's OK
                REGULAR // default
            } else try {
                parse(id.toInt())
            } catch (nfe: NumberFormatException) {
                throw MTLog.Fatal(nfe, "Error while parsing '$id' as pickup type!")
            }
        }
    }
}