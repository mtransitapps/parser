package org.mtransit.parser.mt

import org.mtransit.parser.MTLog
import org.mtransit.parser.MTLog.Fatal
import org.mtransit.parser.gtfs.data.GFieldTypes.toDate
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

object MStoreListingGenerator {

    private const val PLAY = "play"
    private const val LISTINGS = "listings"
    private const val RELEASE_NOTES = "release-notes"
    private const val DEFAULT_TXT = "default.txt"

    // region en-US (default)

    private const val EN_US = "en-US"

    private val DATE_FORMAT_EN: DateFormat get() = SimpleDateFormat("MMMMM d, yyyy", Locale.ENGLISH) // NOT thread-safe

    private val SCHEDULE_EN = Pattern.compile(
        "(Schedule from ([A-Za-z]+ [0-9]{1,2}, [0-9]{4}) to ([A-Za-z]+ [0-9]{1,2}, [0-9]{4})(\\.)?)",
        Pattern.CASE_INSENSITIVE
    ).toRegex()

    private const val SCHEDULE_EN_FROM_TO = $$"Schedule from %1$s to %2$s."

    private const val SCHEDULE_EN_KEEP_FROM_TO = $$"Schedule from $2 to %2$s."

    // endregion

    // region fr-FR

    private const val FR_FR = "fr-FR"

    private val DATE_FORMAT_FR: DateFormat get() = SimpleDateFormat("d MMMMM yyyy", Locale.FRENCH) // NOT thread-safe

    private val SCHEDULE_FR = Pattern.compile(
        "(Horaires du ([0-9]{1,2} \\w+ [0-9]{4}) au ([0-9]{1,2} \\w+ [0-9]{4})(\\.)?)",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CHARACTER_CLASS
    ).toRegex()

    @Suppress("SpellCheckingInspection")
    private const val SCHEDULE_FROM_TO_FR = $$"Horaires du %1$s au %2$s."

    @Suppress("SpellCheckingInspection")
    private const val SCHEDULE_KEEP_FROM_TO_FR = $$"Horaires du $2 au %2$s."

    // endregion

    @JvmStatic
    fun dumpStoreReleaseNotes(
        dumpDirF: File,
        fileBase: String,
        minDate: Int,
        maxDate: Int,
    ) {
        val dumpDirRootF = dumpDirF.parentFile.parentFile
        val dumpDirPlayF = File(dumpDirRootF, PLAY)
        val dirListingF = File(dumpDirPlayF, LISTINGS)
        val dumpDirReleaseNotesF = File(dumpDirPlayF, RELEASE_NOTES)
        val isNext = "next_".equals(fileBase, ignoreCase = true)
        dumpStoreReleaseNote(
            dirListingF, dumpDirReleaseNotesF, isNext, minDate, maxDate,
            EN_US, DATE_FORMAT_EN, SCHEDULE_EN, SCHEDULE_EN_FROM_TO, SCHEDULE_EN_KEEP_FROM_TO
        )
        dumpStoreReleaseNote(
            dirListingF, dumpDirReleaseNotesF, isNext, minDate, maxDate,
            FR_FR, DATE_FORMAT_FR, SCHEDULE_FR, SCHEDULE_FROM_TO_FR, SCHEDULE_KEEP_FROM_TO_FR
        )
    }

    private fun dumpStoreReleaseNote(
        dirListingF: File,
        dumpDirReleaseNotesF: File,
        isNext: Boolean,
        minDate: Int,
        maxDate: Int,
        lang: String,
        dateFormat: DateFormat,
        regex: Regex,
        formatFromTo: String,
        formatKeepFromTo: String,
    ) {
        val dirListingLangF = File(dirListingF, lang)
        val dumpDirReleaseNotesLangF = File(dumpDirReleaseNotesF, lang)
        if (dirListingLangF.exists()) {
            if (dumpDirReleaseNotesLangF.mkdirs()) {
                MTLog.log("Created directory: '%s'.", dumpDirReleaseNotesLangF)
            }
            val dumpFileReleaseNotesLang = File(dumpDirReleaseNotesLangF, DEFAULT_TXT)
            if (dumpFileReleaseNotesLang.exists()) {
                MTLog.log("Update store release notes file: %s.", dumpFileReleaseNotesLang)
                try {
                    val content = dumpFileReleaseNotesLang.readText().replace(
                        regex = regex,
                        replacement = String.format(
                            if (isNext) formatKeepFromTo else formatFromTo,
                            dateFormat.format(toDate(MGenerator.DATE_FORMAT, minDate)),
                            dateFormat.format(toDate(MGenerator.DATE_FORMAT, maxDate))
                        )
                    )
                    dumpFileReleaseNotesLang.writeText(content)
                } catch (ioe: Exception) {
                    throw Fatal(ioe, "Error while writing store release notes files!")
                }
            } else {
                MTLog.log("Generate new store release notes file: %s.", dumpFileReleaseNotesLang)
                try {
                    val content = String.format(
                        formatFromTo,
                        dateFormat.format(toDate(MGenerator.DATE_FORMAT, minDate)),
                        dateFormat.format(toDate(MGenerator.DATE_FORMAT, maxDate))
                    )
                    dumpFileReleaseNotesLang.writeText(content)
                } catch (ioe: Exception) {
                    throw Fatal(ioe, "Error while writing new store release notes files!")
                }
            }
        } else {
            MTLog.log("Do not generate store release notes file in '%s' (listing '%s' does not exist).", dumpDirReleaseNotesLangF, dirListingLangF)
        }
    }
}
