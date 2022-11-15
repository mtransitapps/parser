package org.mtransit.commons

import org.mtransit.commons.Constants.EMPTY
import org.mtransit.commons.Constants.SPACE_

@Suppress("unused")
object HtmlSymbols {

    private const val C_DOOR = "&#128682;" // https://unicode-table.com/en/1F6AA/
    private const val C_DOWNWARDS_ARROW = "&#8595;" // https://unicode-table.com/en/2193/
    private const val C_HOSPITAL = "&#127973;" // https://unicode-table.com/en/1F3E5/
    private const val C_METRO = "&#128647;" // https://unicode-table.com/en/1F687/

    @JvmField // TODO const
    val SUBWAY = if (FeatureFlags.F_HTML_POI_NAME) C_METRO else EMPTY

    @JvmField // TODO const
    val SUBWAY_ = if (FeatureFlags.F_HTML_POI_NAME) SUBWAY + SPACE_ else EMPTY

    @JvmField // TODO const
    val HOSPITAL = if (FeatureFlags.F_HTML_POI_NAME) C_HOSPITAL else EMPTY

    @JvmField // TODO const
    val HOSPITAL_ = if (FeatureFlags.F_HTML_POI_NAME) HOSPITAL + SPACE_ else EMPTY
}