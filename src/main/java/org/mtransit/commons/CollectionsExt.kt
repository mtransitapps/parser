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
            val count = this.count()
            val first = if (count <= 2) null else this.first()
            val last = if (count <= 2) null else this.last()
            this.groupBy { it }.filterNot { it.value.size >= 2 }.flatMap { it.value }
                .dropWhile { it == first }.dropLastWhile { it == last }
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

fun <T> Iterable<T>.toComparableString(stringLength: Int): String {
    return this.joinToString(separator = ",", postfix = ",") { it.toString().padStart(stringLength, '_') }
}

fun <T> Iterable<T>.matchList(
    otherIt: Iterable<T>,
    ignoreRepeat: Boolean = false,
    ignoreFirstAndLast: Boolean = false,
    combineMatch: Boolean = false,
): Float {
    val stringLength: Int = this.union(otherIt).maxOf { it.toString().length }
    val thisToCompare = this.filter(ignoreRepeat, ignoreFirstAndLast)
    val otherToCompare = otherIt.filter(ignoreRepeat, ignoreFirstAndLast)
    var ignoredMatch = 0
    if (ignoreRepeat) {
        val thisRepeat: Map<T, List<T>> = this.groupBy { it }.filter { it.value.size >= 2 }
        val otherRepeat: Map<T, List<T>> = otherIt.groupBy { it }.filter { it.value.size >= 2 }
        for (oRepeat in otherRepeat) {
            if (thisRepeat.containsKey(oRepeat.key)) {
                ignoredMatch += oRepeat.value.size
            }
        }
    }
    if (ignoreFirstAndLast) {
        val thisFirst = this.first()
        val thisLast = this.last()
        val otherFirst = otherIt.first()
        val otherLast = otherIt.last()
        if (thisFirst == otherFirst) {
            ignoredMatch++
        }
        if (thisLast == otherLast) {
            ignoredMatch++
        }
    }
    val ignoredMatchPt: Float = ignoredMatch.toFloat().div(otherIt.count())
    val intersect = thisToCompare.intersect(otherToCompare)
    val firstCommonItem = intersect.firstOrNull()
    val lastCommonItem = intersect.lastOrNull()
    if (firstCommonItem == null || lastCommonItem == null) {
        return 0.0f
    }
    val thisCommon: Iterable<T> = thisToCompare
        .drop(thisToCompare.indexOf(firstCommonItem))
        .dropLast(thisToCompare.count() - 1 - thisToCompare.indexOf(lastCommonItem))
    val thisString = thisCommon.toComparableString(stringLength)
    val otherString = otherToCompare.toComparableString(stringLength)
    val thisPrefix = thisString.commonPrefixWith(otherString)
    val thisPrefixLength = thisPrefix.length
    val thisSuffix = thisString.commonSuffixWith(otherString)
    val thisSuffixLength = thisSuffix.length
    val otherLength = otherIt.toComparableString(stringLength).length
    return (when {
        combineMatch -> thisSuffixLength + thisPrefixLength
        thisPrefixLength > thisSuffixLength -> thisPrefixLength
        else -> thisSuffixLength
    }).toFloat().div(otherLength) + ignoredMatchPt
}

fun <T> Iterable<T>.hasItemsGoingIntoSameOrder(otherIt: Iterable<T>): Boolean {
    return this.countItemsGoingIntoSameOrder(otherIt, firstItemsOnly = true) > 0
}

fun <T> Iterable<T>.countItemsGoingIntoSameOrder(otherIt: Iterable<T>, firstItemsOnly: Boolean = false): Int {
    val thisList = this.toMutableList()
    val otherList = otherIt.toMutableList()
    var count = 0
    val intersect = thisList.intersect(otherList).toMutableSet()
    var commonItem = intersect.firstOrNull()
        ?.apply { intersect -= this }
    var matched = false
    var nextItemInOrderThis: T?
    var nextItemInOrderIt: T?
    while(thisList.isNotEmpty() && thisList.firstOrNull() != commonItem) {
        thisList.removeFirst()
    }
    thisList.removeFirstOrNull()
    while(otherList.isNotEmpty() && otherList.firstOrNull() != commonItem) {
        otherList.removeFirst()
    }
    otherList.removeFirstOrNull()
    while (commonItem != null && intersect.isNotEmpty()) {
        nextItemInOrderThis = thisList.firstOrNull()
        nextItemInOrderIt = otherList.firstOrNull()
        while (nextItemInOrderIt != null && nextItemInOrderThis != null
            && nextItemInOrderIt == nextItemInOrderThis) {
            if (!matched) {
                count++
                matched = true
            }
            count++
            if (firstItemsOnly) return count
            thisList.removeFirst()
            otherList.removeFirst()
            intersect.remove(nextItemInOrderIt)
            nextItemInOrderThis = thisList.firstOrNull()
            nextItemInOrderIt = otherList.firstOrNull()
        }
        commonItem = intersect.firstOrNull() // next
            ?.apply { intersect -= this }
        matched = false
        while(thisList.isNotEmpty() && thisList.firstOrNull() != commonItem) {
            thisList.removeFirst()
        }
        thisList.removeFirstOrNull()
        while(otherList.isNotEmpty() && otherList.firstOrNull() != commonItem) {
            otherList.removeFirst()
        }
        otherList.removeFirstOrNull()
    }
    return count
}
