package org.mtransit.scratch

import java.util.regex.Pattern

@Suppress("JoinDeclarationAndAssignment", "CanBeVal")
internal object RegexScratch {
    @JvmStatic
    fun main(args: Array<String>) {
        var regex: String
        regex = ""
        println("regex: '$regex'.")
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)

        var string: String
        string = ""
        println("string: '$string'.")
        println("-------")

        val matcher = pattern.matcher(string)
        while (matcher.find()) {
            println("-------")
            println("group: '" + matcher.group() + "'.")
            println("groupCount: '" + matcher.groupCount() + "'.")
            for (g in 0..matcher.groupCount()) {
                println("group[" + g + "]: '" + matcher.group(g) + "'.")
            }
            println("-------")
        }

        println("-------")
        var replaceAll: String
        replaceAll = ""
        println("replaceAll: '$replaceAll'.")
        println("-> '" + pattern.matcher(string).replaceAll(replaceAll) + "'.")
    }
}