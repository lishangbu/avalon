package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface CreatureElementId {
    /** 生物 ID */
    val creatureId: Long

    /** 属性 ID */
    val typeId: Long
}
