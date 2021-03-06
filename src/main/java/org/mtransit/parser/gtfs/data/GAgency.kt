package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyIdInt: Int,
    val agencyTimezone: String
) {

    constructor(
        agencyId: String?,
        agencyTimezone: String
    ) : this(
        GIDs.getInt(agencyId ?: Constants.EMPTY),
        agencyTimezone
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val agencyId = _agencyId

    private val _agencyId: String
        get() {
            return GIDs.getString(agencyIdInt)
        }

    companion object {
        const val FILENAME = "agency.txt"

        const val AGENCY_ID = "agency_id"
        const val AGENCY_TIMEZONE = "agency_timezone"
    }
}