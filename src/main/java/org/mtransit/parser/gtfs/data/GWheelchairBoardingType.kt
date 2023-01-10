package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#stopstxt
enum class GWheelchairBoardingType(val id: Int) {

    NO_INFO(0),
    POSSIBLE(1),
    NOT_POSSIBLE(2);

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