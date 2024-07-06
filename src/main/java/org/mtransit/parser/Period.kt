package org.mtransit.parser

data class Period(
    var todayStringInt: Int?,
    var startDate: Int?,
    var endDate: Int?,
) {
    constructor() : this(null, null, null)
}
