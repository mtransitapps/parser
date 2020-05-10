@file:Suppress("unused")

package org.mtransit.parser

object StringUtils {

    @JvmStatic
    fun isEmpty(string: String?) = string.isNullOrEmpty()

    @JvmStatic
    fun isBlank(string: String?) = string.isNullOrBlank()

    @JvmStatic
    fun equals(string1: String?, string2: String?) = string1.equals(string2)
}