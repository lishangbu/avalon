package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface CreatureAbilityId {
    /** 生物 ID */
    val creatureId: Long

    /** 特性 ID */
    val abilityId: Long
}
