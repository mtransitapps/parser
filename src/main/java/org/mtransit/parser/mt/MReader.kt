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
import org.mtransit.parser.mt.data.MServiceId
import org.mtransit.parser.mt.data.MString
import org.mtransit.parser.mt.data.MTripId
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

    private const val CURRENT_ = "current_"
    private const val NEXT_ = "next_"

    private fun getResDirName(fileBase: String? = null): String {
        return "$MAIN_SRC_DIR/" +
                when {
                    CURRENT_.equals(fileBase, ignoreCase = true) -> "$RES-current"
                    NEXT_.equals(fileBase, ignoreCase = true) -> "$RES-next"
                    else -> RES
                }
    }

    // endregion

    // region first/last departures

    private fun makeFirstDepartureRegex(fileBase: String) =
        Regex(any(WHITESPACE_CAR) + "<integer name=\"$fileBase${MGenerator.GTFS_RDS_FIRST_DEPARTURE_IN_SEC}\">${group(oneOrMore(DIGIT_CAR))}</integer>" + any(ANY))

    private fun makeLastDepartureRegex(fileBase: String) =
        Regex(any(WHITESPACE_CAR) + "<integer name=\"$fileBase${MGenerator.GTFS_RDS_LAST_DEPARTURE_IN_SEC}\">${group(oneOrMore(DIGIT_CAR))}</integer>" + any(ANY))

    private fun makeTimeZoneRegex() =
        Regex(any(WHITESPACE_CAR) + "<string name=\"${MGenerator.GTFS_RDS_TIMEZONE}\">${group(oneOrMore(ALPHA_NUM_CAR) + "/" + oneOrMore(ALPHA_NUM_CAR))}</string>")

    @Suppress("unused") // TODO removed
    @JvmStatic
    fun readFirstLastDepartures(fileBase: String): Pair<Int?, Int?>? {
        try {
            val gtfsRdsValuesGenXml = getResDirName(fileBase) + "/$VALUES/${fileBase}${MGenerator.GTFS_RDS_VALUES_GEN_XML}"
            val gtfsRdsValuesGenXmlFile = File(gtfsRdsValuesGenXml)
            if (!gtfsRdsValuesGenXmlFile.exists()) {
                MTLog.log("File not found '$gtfsRdsValuesGenXml'!")
                return null
            }
            val commonRdsValuesGenXml = getResDirName() + "/$VALUES/${MGenerator.GTFS_RDS_VALUES_GEN_XML}"
            val commonRdsValuesGenXmlFile = File(commonRdsValuesGenXml)
            if (!commonRdsValuesGenXmlFile.exists()) {
                MTLog.log("File not found '$commonRdsValuesGenXml'!")
                return null
            }
            val firstDepartureRegex = makeFirstDepartureRegex(fileBase)
            val lastDepartureRegex = makeLastDepartureRegex(fileBase)
            val gtfsRdsValuesGenXmlFileLines = gtfsRdsValuesGenXmlFile.readLines()
            val firstDepartureInSec = gtfsRdsValuesGenXmlFileLines
                .firstOrNull { firstDepartureRegex.matches(it) }
                ?.let { firstDepartureRegex.find(it) }
                ?.let { it.groupValues[1] }
                ?.toIntOrNull()
            val lastDepartureInSec = gtfsRdsValuesGenXmlFileLines
                .firstOrNull { lastDepartureRegex.matches(it) }
                ?.let { lastDepartureRegex.find(it) }
                ?.let { it.groupValues[1] }
                ?.toIntOrNull()
            if (firstDepartureInSec == null || lastDepartureInSec == null) {
                MTLog.log("First ($firstDepartureInSec) or last ($lastDepartureInSec) departure not found in '$gtfsRdsValuesGenXml'!")
                return null
            }
            val commonTimeZoneRegex = makeTimeZoneRegex()
            val commonRdsValuesGenXmlFileLines = commonRdsValuesGenXmlFile.readLines()
            val timeZone = commonRdsValuesGenXmlFileLines
                .firstOrNull { commonTimeZoneRegex.matches(it) }
                ?.let { commonTimeZoneRegex.find(it) }
                ?.let { it.groupValues[1] }
            if (timeZone.isNullOrBlank()) {
                MTLog.log("Time zone not found in '$commonRdsValuesGenXml'!")
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

    @JvmStatic
    fun loadServiceDates(fileBase: String) =
        readFileMerge("service dates", fileBase, MGenerator.GTFS_SCHEDULE_SERVICE_DATES) { MServiceDate.fromFileLine(it) }

    private fun <T> readFile(type: String, fileBase: String, fileName: String, transform: (String) -> T?): List<T>? = try {
        (File("${getResDirName(fileBase)}/$RAW/${fileBase}$fileName").takeIf { it.exists() }
            ?: CURRENT_.takeIf { NEXT_ == fileBase }?.let { File("${getResDirName(it)}/$RAW/${it}$fileName") }?.takeIf { it.exists() }
            ?: "".takeIf { CURRENT_ == fileBase || NEXT_ == fileBase }?.let { File("${getResDirName(it)}/$RAW/${it}$fileName") }?.takeIf { it.exists() })
            ?.readLines()
            ?.mapNotNull { transform(it) }
            ?: run {
                MTLog.log("File not found for '$type' with fileBase '$fileBase' and fileName '$fileName'!")
                null
            }
    } catch (e: Exception) {
        MTLog.logNonFatal(e, "Error while reading '$fileBase' $type!")
        null
    }

    @Suppress("SameParameterValue")
    private fun <T> readFileMerge(type: String, fileBase: String, fileName: String, transform: (String) -> List<T>?): List<T>? = try {
        (File("${getResDirName(fileBase)}/$RAW/${fileBase}$fileName").takeIf { it.exists() }
            ?: CURRENT_.takeIf { NEXT_ == fileBase }?.let { File("${getResDirName(it)}/$RAW/${it}$fileName") }?.takeIf { it.exists() }
            ?: "".takeIf { CURRENT_ == fileBase || NEXT_ == fileBase }?.let { File("${getResDirName(it)}/$RAW/${it}$fileName") }?.takeIf { it.exists() })
            ?.readLines()
            ?.mapNotNull { transform(it) }
            ?.flatten()
            ?: run {
                MTLog.log("File not found for merge '$type' with fileBase '$fileBase' and fileName '$fileName'!")
                null
            }
    } catch (e: Exception) {
        MTLog.logNonFatal(e, "Error while reading '$fileBase' $type (merge)!")
        null
    }

    // endregion

    // region trip IDs

    @JvmStatic
    fun loadTripIds(fileBase: String) =
        readFile("trip ids", fileBase, MGenerator.GTFS_SCHEDULE_TRIP_IDS) { MTripId.fromFileLine(it) }

    // endregion

    // region service IDs

    @JvmStatic
    fun loadServiceIds(fileBase: String) =
        readFile("service ids", fileBase, MGenerator.GTFS_SCHEDULE_SERVICE_IDS) { MServiceId.fromFileLine(it) }

    // endregion

    // region strings

    @JvmStatic
    fun loadStrings(fileBase: String) =
        readFile("strings", fileBase, MGenerator.GTFS_STRINGS) { MString.fromFileLine(it) }

    // endregion
}