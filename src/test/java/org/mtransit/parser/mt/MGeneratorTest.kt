package org.mtransit.parser.mt

import org.junit.Assert.assertEquals
import org.junit.Test

class MGeneratorTest {

    // https://developer.android.com/guide/topics/resources/string-resource#FormattingAndStyling
    // https://developer.android.com/guide/topics/resources/string-resource#escaping_quotes
    @Test
    fun test_escapeResString() {
        val string = "\\-@-?-'-\"-%"

        val result = MGenerator.escapeResString(string)

        assertEquals("\\\\-\\@-\\?-\\'-\\\"-%%", result)
    }
}