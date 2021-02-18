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

fun <T> Iterable<T>.filter(removeRepeat: Boolean = false, removeFirstAndLast: Boolean = false): Iterable<T> {
    return if (removeFirstAndLast) {
        if (removeRepeat) {
            val first = this.first()
            val last = this.last()
            this.groupBy { it }.filterNot { it.value.size >= 2 }.flatMap { it.value }.dropWhile { it == first }.dropLastWhile { it == last }
        } else {
            this.drop(1).dropLast(1)
        }
    } else {
        if (removeRepeat) {
            this.groupBy { it }.filterNot { it.value.size >= 2 }.flatMap { it.value }
        } else {
            this
        }
    }
}

fun <T> Iterable<T>.intersectWithOrder(otherIt: Iterable<T>, ignoreRepeat: Boolean = false, ignoreFirstAndLast: Boolean = false): Set<T> {
    val thisToCompare = this.filter(ignoreRepeat, ignoreFirstAndLast)
    val otherToCompare = otherIt.filter(ignoreRepeat, ignoreFirstAndLast)
    val intersect = thisToCompare.intersect(otherToCompare)
    val firstCommonT = intersect.firstOrNull()
    val lastCommonT = intersect.lastOrNull()
    if (firstCommonT == null || lastCommonT == null) {
        return emptySet()
    }
    val firstCommonTIdx = otherToCompare.indexOf(firstCommonT)
    val lastCommonTIdx = otherToCompare.indexOf(lastCommonT)
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

fun <T> Iterable<T>.overlap(otherIt: Iterable<T>): Boolean {
    if (this.count() == 0 || otherIt.count() == 0) {
        return false
    }
    val intersect = this.intersect(otherIt)
    if (intersect.isEmpty()) {
        return false
    }
    val startE = intersect.first()
    var thisIdx = this.indexOf(startE)
    if (thisIdx == -1) {
        return false // no overlap
    }
    val thisSize = this.count()
    var otherIdx = otherIt.indexOf(startE)
    if (otherIdx == -1) {
        return false // no overlap
    }
    val startOtherIdx = otherIdx
    val otherSize = otherIt.count()
    var thisCheckCount = 0
    var otherCheckCount = 0
    var thisLastItem: T? = null
    var otherLastItem: T? = null
    while (thisCheckCount < thisSize && otherCheckCount < otherSize) {
        val thisItem = this.elementAt(thisIdx)
        if (thisLastItem == thisItem) {
            thisCheckCount++
            thisIdx = thisIdx.inc().coerceAtMostTo(thisSize - 1, 0)
            continue // looking for different value
        }
        val otherItem = otherIt.elementAt(otherIdx)
        if (otherLastItem == otherItem) {
            otherCheckCount++
            otherIdx = otherIdx.inc().coerceAtMostTo(otherSize - 1, 0)
            continue // looking for different value
        }
        thisLastItem = thisItem // this item used
        if (otherIdx == 0
            && thisItem != otherItem
        ) {
            thisCheckCount++
            thisIdx = thisIdx.inc().coerceAtMostTo(thisSize - 1, 0)
            continue // looking for next match in this
        }
        otherLastItem = otherItem // other item used
        if (thisItem != otherItem) {
            return false
        }
        thisCheckCount++
        thisIdx = thisIdx.inc().coerceAtMostTo(thisSize - 1, 0)
        otherCheckCount++
        otherIdx = otherIdx.inc().coerceAtMostTo(otherSize - 1, 0)
        if (otherIdx == startOtherIdx) {
            break // finish traversing other, incomplete loop overlap
        }
    }
    if (otherSize != otherCheckCount) {
        return false
    }
    return true
}

fun <T> Iterable<T>.matchList(otherIt: Iterable<T>, ignoreRepeat: Boolean = false, ignoreFirstAndLast: Boolean = false): Float {
    val stringLength: Int = this.union(otherIt).maxOf { it.toString().length }
    val thisToCompare = this.filter(ignoreRepeat, ignoreFirstAndLast)
    val otherToCompare = otherIt.filter(ignoreRepeat, ignoreFirstAndLast)
    val intersect = thisToCompare.intersect(otherToCompare)
    val firstCommonItem = intersect.firstOrNull()
    val lastCommonItem = intersect.lastOrNull()
    if (firstCommonItem == null || lastCommonItem == null) {
        return 0.0f
    }
    val thisCommon: Iterable<T> = thisToCompare
        .drop(thisToCompare.indexOf(firstCommonItem))
        .dropLast(thisToCompare.count() - 1 - thisToCompare.indexOf(lastCommonItem))
    val thisString = thisCommon.joinToString(separator = ",", postfix = ",") { it.toString().padStart(stringLength, '_') }
    val otherString = otherToCompare.joinToString(separator = ",", postfix = ",") { it.toString().padStart(stringLength, '_') }
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