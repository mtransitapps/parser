package org.mtransit.commons

import org.mtransit.commons.Constants.SPACE_

@Suppress("unused", "MemberVisibilityCanBePrivate")
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

    const val SUBWAY = C_METRO
    const val SUBWAY_ = SUBWAY + SPACE_

    const val TRAIN = C_TRAIN
    const val TRAIN_ = TRAIN + SPACE_

    const val AIRPORT = C_AIRPLANE
    const val AIRPORT_ = AIRPORT + SPACE_

    const val HOSPITAL = C_HOSPITAL
    const val HOSPITAL_ = HOSPITAL + SPACE_

    const val SCHOOL = C_SCHOOL
    const val SCHOOL_ = SCHOOL + SPACE_

    const val STADIUM = C_STADIUM
    const val STADIUM_ = STADIUM + SPACE_

    const val PARK = C_NATIONAL_PARK
    const val PARK_ = PARK + SPACE_

    const val ROLLER_COASTER = C_ROLLER_COASTER
    const val ROLLER_COASTER_ = ROLLER_COASTER + SPACE_

    // @formatter:on
}