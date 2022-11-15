package org.mtransit.commons

import org.mtransit.commons.Constants.EMPTY
import org.mtransit.commons.Constants.SPACE_

@Suppress("unused")
object HtmlSymbols {

    private const val DOWNWARDS_ARROW = "&#8595;" // https://unicode-table.com/en/2193/
    private const val METRO = "&#128647;" // https://unicode-table.com/en/1F687/

    @JvmField // TODO const
    val SUBWAY = if (FeatureFlags.F_HTML_POI_NAME) METRO else EMPTY

    @JvmField // TODO const
    val SUBWAY_ = if (FeatureFlags.F_HTML_POI_NAME) SUBWAY + SPACE_ else EMPTY
}