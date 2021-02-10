package org.mtransit.commons

fun <T> Iterable<T>.indexOf(pairT: Pair<T?, T?>?): Int {
    var index: Int = pairT?.first?.let { first ->
        this.indexOf(first)
    } ?: -1
    if (index < 0) {
        index = pairT?.second?.let { second ->
            this.indexOf(second)
        } ?: -1
    }
    return index
}

fun <T> Iterable<T>.lastIndexOf(pairT: Pair<T?, T?>?): Int {
    var index: Int = pairT?.first?.let { first ->
        this.lastIndexOf(first)
    } ?: -1
    if (index < 0) {
        index = pairT?.second?.let { second ->
            this.lastIndexOf(second)
        } ?: -1
    }
    return index
}

fun <T> Iterable<T>.intersectWithOrder(otherIt: Iterable<T>): Set<T> {
    val intersect = this.intersect(otherIt)
    val firstCommonT = intersect.firstOrNull()
    val lastCommonT = intersect.lastOrNull()
    if (firstCommonT == null || lastCommonT == null) {
        return emptySet()
    }
    val firstCommonTIdx = otherIt.indexOf(firstCommonT)
    val lastCommonTIdx = otherIt.indexOf(lastCommonT)
    if (firstCommonTIdx > lastCommonTIdx) {
        return emptySet()
    }
    return intersect
}

fun <T> Iterable<T>.containsExactList(otherIt: Iterable<T>): Boolean {
    if (this.count() < otherIt.count()) {
        return false // smaller list can NOT contain bigger list
    }
    return this.joinToString { it.toString() }
        .contains(
            otherIt.joinToString { it.toString() }
        )
}

fun <T> Iterable<T>.matchList(otherIt: Iterable<T>): Float {
    val stringLength: Int = this.union(otherIt).maxOf { it.toString().length }
    val intersect = this.intersect(otherIt)
    val firstCommonChar = intersect.firstOrNull()
    val lastCommonChar = intersect.lastOrNull()
    if (firstCommonChar == null || lastCommonChar == null) {
        return 0.0f
    }
    val thisCommon: Iterable<T> = this
        .drop(this.indexOf(firstCommonChar))
        .dropLast(this.count() - 1 - this.indexOf(lastCommonChar))
    val thisString = thisCommon.joinToString(separator = ",", postfix = ",") { it.toString().padStart(stringLength, '_') }
    val otherString = otherIt.joinToString(separator = ",", postfix = ",") { it.toString().padStart(stringLength, '_') }
    val prefix = thisString.commonPrefixWith(otherString)
    val prefixLength = prefix.length.toFloat()
    val suffix = thisString.commonSuffixWith(otherString)
    val suffixLength = suffix.length.toFloat()
    val otherLength = otherString.length.toFloat()
    return if (prefixLength > suffixLength) {
        prefixLength / otherLength
    } else {
        suffixLength / otherLength
    }
}