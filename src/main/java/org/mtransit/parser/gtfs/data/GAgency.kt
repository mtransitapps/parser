package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://gtfs.org/schedule/reference/#agencytxt
// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyIdInt: Int,
    val agencyName: String,
    val agencyUrl: String,
    val agencyTimezone: String,
    val agencyLang: String?,
    val agencyPhone: String?,
    val agencyFareUrl: String?,
    val agencyEmail: String?,
) {

    constructor(
        agencyId: String?,
        agencyName: String,
        agencyUrl: String,
        agencyTimezone: String,
        agencyLang: String?,
        agencyPhone: String?,
        agencyFareUrl: String?,
        agencyEmail: String?,
    ) : this(
        GIDs.getInt(agencyId ?: MISSING_AGENCY_ID),
        agencyName,
        agencyUrl,
        agencyTimezone,
        agencyLang,
        agencyPhone,
        agencyFareUrl,
        agencyEmail,
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val agencyId = _agencyId

    private val _agencyId: String
        get() {
            return GIDs.getString(agencyIdInt)
        }

    @Suppress("unused")
    fun isDifferentAgency(otherAgencyIdInt: Int): Boolean = agencyIdInt != otherAgencyIdInt

    @Deprecated(message = "Not memory efficient", replaceWith = ReplaceWith("isDifferentAgency(GIDs.getInt(otherAgencyId))"))
    @Suppress("unused")
    fun isDifferentAgency(otherAgencyId: String): Boolean = isDifferentAgency(GIDs.getInt(otherAgencyId))

    companion object {
        const val FILENAME = "agency.txt"

        const val AGENCY_ID = "agency_id" // Conditionally Required
        const val AGENCY_NAME = "agency_name" // Required
        const val AGENCY_URL = "agency_url" // Required
        const val AGENCY_TIMEZONE = "agency_timezone" // Required

        const val AGENCY_LANG = "agency_lang" // Optional
        const val AGENCY_PHONE = "agency_phone" // Optional
        const val AGENCY_FARE_URL = "agency_fare_url" // Optional
        const val AGENCY_EMAIL = "agency_email" // Optional

        const val MISSING_AGENCY_ID = Constants.EMPTY
    }
}