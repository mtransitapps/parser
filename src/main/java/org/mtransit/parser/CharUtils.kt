@file:Suppress("unused")

package org.mtransit.parser

object CharUtils {

    const val EMPTY = Constants.EMPTY

    @JvmStatic
    fun countUpperCase(string: String?) = countUpperCase(string?.toCharArray())

    @JvmStatic
    fun countUpperCase(charArray: CharArray?) = charArray?.count { it.isUpperCase() } ?: 0
}