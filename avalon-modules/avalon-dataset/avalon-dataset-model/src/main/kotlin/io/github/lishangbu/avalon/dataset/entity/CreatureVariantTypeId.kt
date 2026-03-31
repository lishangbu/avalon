package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface CreatureVariantTypeId {
    /** 生物变体 ID */
    val creatureVariantId: Long

    /** 属性 ID */
    val typeId: Long
}
