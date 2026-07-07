package org.mtransit.parser

data class Period @JvmOverloads constructor(
    var todayStringInt: Int? = null,
    var startDate: Int? = null,
    var endDate: Int? = null,
) {
    companion object {
        @JvmStatic
        fun from(today: Int) = Period(
            todayStringInt = today,
            startDate = null,
            endDate = null
        )
    }
}
