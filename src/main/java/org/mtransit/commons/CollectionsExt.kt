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