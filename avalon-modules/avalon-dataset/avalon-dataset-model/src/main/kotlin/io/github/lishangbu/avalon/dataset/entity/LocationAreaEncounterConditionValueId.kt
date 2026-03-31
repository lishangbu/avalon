package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface LocationAreaEncounterConditionValueId {
    /** 地点区域遭遇 ID */
    val locationAreaEncounterId: Long

    /** 遭遇条件值 ID */
    val encounterConditionValueId: Long
}
