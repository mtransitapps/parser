package org.mtransit.commons

import org.mtransit.commons.Constants.EMPTY
import org.mtransit.commons.Constants.SPACE_

@Suppress("unused")
object HtmlSymbols {

    private const val C_AIRPLANE = "&#9992;" // ✈ https://unicode-table.com/en/2708/
    private const val C_AIRPLANE_ARRIVING = "&#128748;" // 🛬 https://unicode-table.com/en/1F6EC/
    private const val C_AIRPLANE_DEPARTURE = "&#128747;" // 🛫 https://unicode-table.com/en/1F6EB/
    private const val C_AIRPLANE_SMALL = "&#128745;" // 🛩 https://unicode-table.com/en/1F6E9/
    private const val C_DOOR = "&#128682;" // 🚪 https://unicode-table.com/en/1F6AA/
    private const val C_DOWNWARDS_ARROW = "&#8595;" // ↓ https://unicode-table.com/en/2193/
    private const val C_HOSPITAL = "&#127973;" // 🏥 https://unicode-table.com/en/1F3E5/
    private const val C_METRO = "&#128647;" // 🚇 https://unicode-table.com/en/1F687/
    private const val C_NATIONAL_PARK = "&#127966;" // 🏞 https://unicode-table.com/en/1F3DE/
    private const val C_ROLLER_COASTER = "&#127906;" // 🎢 https://unicode-table.com/en/1F3A2/
    private const val C_SCHOOL = "&#127979;" // 🏫 https://unicode-table.com/en/1F3EB/
    private const val C_SLOT_MACHINE = "&#127920;" // 🎰 https://unicode-table.com/en/1F3B0/
    private const val C_STADIUM = "&#127967;" // 🏟 https://unicode-table.com/en/1F3DF/
    private const val C_TRAIN = "&#128646;" // 🚆 https://unicode-table.com/en/1F686/

    // @formatter:off

    @JvmField // TODO const
    val SUBWAY = if (FeatureFlags.F_HTML_POI_NAME) C_METRO else EMPTY
    @JvmField // TODO const
    val SUBWAY_ = if (FeatureFlags.F_HTML_POI_NAME) SUBWAY + SPACE_ else EMPTY

    @JvmField // TODO const
    val TRAIN = if (FeatureFlags.F_HTML_POI_NAME) C_TRAIN else EMPTY
    @JvmField // TODO const
    val TRAIN_ = if (FeatureFlags.F_HTML_POI_NAME) TRAIN + SPACE_ else EMPTY

    @JvmField // TODO const
    val HOSPITAL = if (FeatureFlags.F_HTML_POI_NAME) C_HOSPITAL else EMPTY
    @JvmField // TODO const
    val HOSPITAL_ = if (FeatureFlags.F_HTML_POI_NAME) HOSPITAL + SPACE_ else EMPTY

    @JvmField // TODO const
    val SCHOOL = if (FeatureFlags.F_HTML_POI_NAME) C_SCHOOL else EMPTY
    @JvmField // TODO const
    val SCHOOL_ = if (FeatureFlags.F_HTML_POI_NAME) SCHOOL + SPACE_ else EMPTY

    @JvmField // TODO const
    val STADIUM = if (FeatureFlags.F_HTML_POI_NAME) C_STADIUM else EMPTY
    @JvmField // TODO const
    val STADIUM_ = if (FeatureFlags.F_HTML_POI_NAME) STADIUM + SPACE_ else EMPTY

    @JvmField // TODO const
    val PARK = if (FeatureFlags.F_HTML_POI_NAME) C_NATIONAL_PARK else EMPTY
    @JvmField // TODO const
    val PARK_ = if (FeatureFlags.F_HTML_POI_NAME) PARK + SPACE_ else EMPTY

    @JvmField // TODO const
    val ROLLER_COASTER = if (FeatureFlags.F_HTML_POI_NAME) C_ROLLER_COASTER else EMPTY
    @JvmField // TODO const
    val ROLLER_COASTER_ = if (FeatureFlags.F_HTML_POI_NAME) ROLLER_COASTER + SPACE_ else EMPTY

    // @formatter:on
}