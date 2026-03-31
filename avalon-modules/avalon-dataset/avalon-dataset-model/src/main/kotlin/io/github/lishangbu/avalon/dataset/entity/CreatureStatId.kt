package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface CreatureStatId {
    /** 生物 ID */
    val creatureId: Long

    /** 能力值 ID */
    val statId: Long
}
