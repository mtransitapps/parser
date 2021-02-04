@file:Suppress("unused")

package org.mtransit.parser

import org.mtransit.commons.StringUtils

@Deprecated(message = "Use commons-java")
object StringUtils {

    const val EMPTY = StringUtils.EMPTY

    @JvmStatic
    fun isEmpty(string: String?) = StringUtils.isEmpty(string)

    @JvmStatic
    fun isBlank(string: String?) = StringUtils.isBlank(string)

    @JvmStatic
    fun equals(string1: String?, string2: String?) = StringUtils.equals(string1, string2)

    @JvmStatic
    fun isNumeric(string: String) = StringUtils.isNumeric(string)
}