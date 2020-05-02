package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyIdInt: Int,
    val agencyTimezone: String
) {

    constructor(
        agencyId: String,
        agencyTimezone: String
    ) : this(
        GIDs.getInt(agencyId),
        agencyTimezone
    )

    val agencyId: String
        get() {
            return GIDs.getString(agencyIdInt)
        }

    companion object {
        const val FILENAME = "agency.txt"

        const val AGENCY_ID = "agency_id"
        const val AGENCY_TIMEZONE = "agency_timezone"
    }
}