package org.mtransit.parser.mt.data

enum class MDropOffType(val id: Int) {

    REGULAR(0),
    NO_DROP_OFF(1),
    MUST_PHONE_AGENCY(2),
    MUST_COORDINATE_WITH_DRIVER(3);

    fun toFile(): String {
        return id.toString()
    }

    companion object {

        @JvmStatic
        fun parse(id: Int): MDropOffType {
            return when {
                REGULAR.id == id -> REGULAR
                NO_DROP_OFF.id == id -> NO_DROP_OFF
                MUST_PHONE_AGENCY.id == id -> MUST_PHONE_AGENCY
                MUST_COORDINATE_WITH_DRIVER.id == id -> MUST_COORDINATE_WITH_DRIVER
                else -> REGULAR // default
            }
        }

        @JvmStatic
        fun parse(id: String?): MDropOffType {
            return when (id) {
                null -> REGULAR // default
                else -> parse(id.toInt())
            }
        }
    }
}