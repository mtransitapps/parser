package org.mtransit.commons

@Suppress("unused", "MemberVisibilityCanBePrivate")
object TorontoTTCCommons {

    // http://www.ttc.ca/PDF/Standards/TTC%20Brand%20Standards.pdf
    const val TTC_RED = "DA291C" // Pantone 485 C - C:0 M:100 Y:100 K:0 - R:218 G:37 B:29
    const val TTC_GREY = "4B4F54" // Pantone 7540 C - C:0 M:0 Y:0 K:70 - R:109 G:110 B:113
    const val TTC_LINE_1_YELLOW = "FFC72C" // Pantone 123 C - C:0 M:20 Y:100 K:0 - R:248 G:195 B:0
    const val TTC_LINE_2_GREEN = "009A44" // Pantone 347 C - C:100 M:0 Y:100 K:0 - R:0 G:146 B:63
    const val TTC_LINE_3_BLUE = "0086D6" // Pantone 2194 C - C:100 M:15 Y:0 K:0 - R:0 G:130 B:201
    const val TTC_LINE_4_PURPLE = "A20067" // Pantone 234 C - C:10 M:100 Y:0 K:20 - R:162 G:26 B:104
    const val TTC_LINE_5_ORANGE = "FE5000" // ? // Pantone Orange 021 - C:0 M:60 Y:100 K:0 -R:235 G:135 B:56

    // http://www.ttc.ca/Routes/Buses.jsp
    const val TTC_BUS_EXPRESS_GREEN = "00A651"
    const val TTC_BUS_NIGHT_BLUE = "0756A5"

    @JvmStatic
    fun fixColor(color: String?) : String? {
        return when (color) {
            "FF0000" -> TTC_RED
            "008000" -> TTC_BUS_EXPRESS_GREEN
            "0000FF" -> TTC_BUS_NIGHT_BLUE
            else -> null // not fixed
        }
    }
}