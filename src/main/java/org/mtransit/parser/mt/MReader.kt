package org.mtransit.parser.mt

import org.mtransit.commons.RegexUtils.ALPHA_NUM_CAR
import org.mtransit.commons.RegexUtils.ANY
import org.mtransit.commons.RegexUtils.DIGIT_CAR
import org.mtransit.commons.RegexUtils.WHITESPACE_CAR
import org.mtransit.commons.RegexUtils.any
import org.mtransit.commons.RegexUtils.group
import org.mtransit.commons.RegexUtils.oneOrMore
import org.mtransit.commons.secToMs
import org.mtransit.commons.toDate
import org.mtransit.parser.MTLog
import org.mtransit.parser.Pair
import org.mtransit.parser.gtfs.data.GFieldTypes
import org.mtransit.parser.mt.data.MServiceDate
import java.io.File
import java.util.TimeZone

object MReader {

    // region common

    private const val PROJECT_DIR = ".."
    private const val APP_ANDROID_DIR = "$PROJECT_DIR/app-android"
    private const val MAIN_SRC_DIR = "$APP_ANDROID_DIR/src/main"

    private const val RES = "res"
    private const val RAW = "raw"
    private const val VALUES = "values"

    private fun getResDirName(fileBase: String? = null): String {
        return "$MAIN_SRC_DIR/" + if ("current_".equals(fileBase, ignoreCase = true)) {
            "$RES-current"
        } else if ("next_".equals(fileBase, ignoreCase = true)) {
            "$RES-next"
        } else {
            RES
        }
    }

    // endregion

    // region first/last departures

    private const val GTFS_RTS_VALUES_GEN_XML = "gtfs_rts_values_gen.xml"

    private const val GTFS_RTS_FIRST_DEPARTURE_IN_SEC = "gtfs_rts_first_departure_in_sec"
    private const val GTFS_RTS_LAST_DEPARTURE_IN_SEC = "gtfs_rts_last_departure_in_sec"

    private fun makeFirstDepartureRegex(fileBase: String) =
        Regex(any(WHITESPACE_CAR) + "<integer name=\"$fileBase$GTFS_RTS_FIRST_DEPARTURE_IN_SEC\">${group(oneOrMore(DIGIT_CAR))}</integer>" + any(ANY))

    private fun makeLastDepartureRegex(fileBase: String) =
        Regex(any(WHITESPACE_CAR) + "<integer name=\"$fileBase$GTFS_RTS_LAST_DEPARTURE_IN_SEC\">${group(oneOrMore(DIGIT_CAR))}</integer>" + any(ANY))

    private const val GTFS_RTS_TIMEZONE = "gtfs_rts_timezone"

    private fun makeTimeZoneRegex() =
        Regex(any(WHITESPACE_CAR) + "<string name=\"$GTFS_RTS_TIMEZONE\">${group(oneOrMore(ALPHA_NUM_CAR) + "/" + oneOrMore(ALPHA_NUM_CAR))}</string>")

    @Suppress("unused") // TODO removed
    @JvmStatic
    fun readFirstLastDepartures(fileBase: String): Pair<Int?, Int?>? {
        try {
            val gtfsRtsValuesGenXml = getResDirName(fileBase) + "/$VALUES/${fileBase}$GTFS_RTS_VALUES_GEN_XML"
            val gtfsRtsValuesGenXmlFile = File(gtfsRtsValuesGenXml)
            if (!gtfsRtsValuesGenXmlFile.exists()) {
                MTLog.log("File not found '$gtfsRtsValuesGenXml'!")
                return null
            }
            val commonRtsValuesGenXml = getResDirName() + "/$VALUES/$GTFS_RTS_VALUES_GEN_XML"
            val commonRtsValuesGenXmlFile = File(commonRtsValuesGenXml)
            if (!commonRtsValuesGenXmlFile.exists()) {
                MTLog.log("File not found '$commonRtsValuesGenXml'!")
                return null
            }
            val firstDepartureRegex = makeFirstDepartureRegex(fileBase)
            val lastDepartureRegex = makeLastDepartureRegex(fileBase)
            val gtfsRtsValuesGenXmlFileLines = gtfsRtsValuesGenXmlFile.readLines()
            val firstDepartureInSec = gtfsRtsValuesGenXmlFileLines
                .firstOrNull { firstDepartureRegex.matches(it) }
                ?.let { firstDepartureRegex.find(it) }
                ?.let { it.groupValues[1] }
                ?.toIntOrNull()
            val lastDepartureInSec = gtfsRtsValuesGenXmlFileLines
                .firstOrNull { lastDepartureRegex.matches(it) }
                ?.let { lastDepartureRegex.find(it) }
                ?.let { it.groupValues[1] }
                ?.toIntOrNull()
            if (firstDepartureInSec == null || lastDepartureInSec == null) {
                MTLog.log("First ($firstDepartureInSec) or last ($lastDepartureInSec) departure not found in '$gtfsRtsValuesGenXml'!")
                return null
            }
            val commonTimeZoneRegex = makeTimeZoneRegex()
            val commonRtsValuesGenXmlFileLines = commonRtsValuesGenXmlFile.readLines()
            val timeZone = commonRtsValuesGenXmlFileLines
                .firstOrNull { commonTimeZoneRegex.matches(it) }
                ?.let { commonTimeZoneRegex.find(it) }
                ?.let { it.groupValues[1] }
            if (timeZone.isNullOrBlank()) {
                MTLog.log("Time zone not found in '$commonRtsValuesGenXml'!")
                return null
            }
            val dateFormat = GFieldTypes.makeDateFormat().apply {
                this.timeZone = TimeZone.getTimeZone(timeZone)
            }
            val startDate = dateFormat.format(firstDepartureInSec.secToMs().toDate()).toInt()
            val endData = dateFormat.format(lastDepartureInSec.secToMs().toDate()).toInt() - 1 // service ends next day
            return Pair(startDate, endData)
        } catch (e: Exception) {
            MTLog.logNonFatal(e, "Error while reading '$fileBase' departures!")
            return null
        }
    }

    // endregion

    // region service dates

    private const val GTFS_SCHEDULE_SERVICE_DATES = "gtfs_schedule_service_dates"

    @JvmStatic
    fun loadServiceDates(fileBase: String): List<MServiceDate>? {
        try {
            val gtfsScheduleServiceDates = getResDirName(fileBase) + "/$RAW/${fileBase}$GTFS_SCHEDULE_SERVICE_DATES"
            val gtfsScheduleServiceDatesFile = File(gtfsScheduleServiceDates)
            if (!gtfsScheduleServiceDatesFile.exists()) {
                MTLog.log("File not found '$gtfsScheduleServiceDates'!")
                return null
            }
            val gtfsScheduleServiceDatesFileLines = gtfsScheduleServiceDatesFile.readLines()
            val serviceDates = gtfsScheduleServiceDatesFileLines.mapNotNull { line ->
                MServiceDate.fromFileLine(line)
            }
            return serviceDates
        } catch (e: Exception) {
            MTLog.logNonFatal(e, "Error while reading '$fileBase' service dates!")
            return null
        }
    }

    // endregion
}