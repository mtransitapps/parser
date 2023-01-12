package org.mtransit.parser.gtfs.data

// https://gtfs.org/schedule/reference/#stopstxt
// https://gtfs.org/schedule/reference/#tripstxt
enum class GWheelchairBoardingType(val id: Int) {

    NO_INFO(0), // No accessibility information for the trip.
    POSSIBLE(1), // Vehicle being used on this particular trip can accommodate at least one rider in a wheelchair.
    NOT_POSSIBLE(2); // No riders in wheelchairs can be accommodated on this trip.

    companion object {

        fun parse(id: Int?): GWheelchairBoardingType {
            return when (id) {
                NO_INFO.id -> NO_INFO
                POSSIBLE.id -> POSSIBLE
                NOT_POSSIBLE.id -> NOT_POSSIBLE
                else -> NO_INFO // default
            }
        }

        @JvmStatic
        fun parse(id: String?): GWheelchairBoardingType {
            return parse(id?.toInt())
        }
    }
}