@file:Suppress("unused")

package org.mtransit.parser

import org.mtransit.commons.CharUtils

@Deprecated(message = "Use commons-java instead!")
object CharUtils {

    const val EMPTY = CharUtils.EMPTY

    @JvmStatic
    fun countUpperCase(string: String?) = CharUtils.countUpperCase(string)

    @JvmStatic
    fun countUpperCase(charArray: CharArray?) = CharUtils.countUpperCase(charArray)
}