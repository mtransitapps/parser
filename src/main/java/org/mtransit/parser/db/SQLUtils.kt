@file:Suppress("unused")

package org.mtransit.parser.db

import org.apache.commons.text.translate.CharSequenceTranslator
import org.apache.commons.text.translate.LookupTranslator
import org.mtransit.commons.Constants

object SQLUtils {

    private val ESCAPE: CharSequenceTranslator by lazy {
        LookupTranslator(
            mapOf(
                "'" to "''",
                "_" to Constants.EMPTY
            )
        )
    }

    @JvmStatic
    fun escape(string: String): String {
        return ESCAPE.translate(string)
    }

    @JvmStatic
    fun quotes(string: String): String {
        return "'$string'"
    }
}