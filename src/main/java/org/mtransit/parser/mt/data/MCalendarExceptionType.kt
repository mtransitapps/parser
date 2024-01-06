package org.mtransit.parser.mt.data

import org.mtransit.commons.GTFSCommons

enum class MCalendarExceptionType(val id: Int) {
    DEFAULT(GTFSCommons.EXCEPTION_TYPE_DEFAULT),
    ADDED(GTFSCommons.EXCEPTION_TYPE_ADDED),
    REMOVED(GTFSCommons.EXCEPTION_TYPE_REMOVED)
}
