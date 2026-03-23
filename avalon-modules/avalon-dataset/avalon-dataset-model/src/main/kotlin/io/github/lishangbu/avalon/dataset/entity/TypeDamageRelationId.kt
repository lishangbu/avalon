package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface TypeDamageRelationId {
    /** 攻击方属性 ID */
    val attackingTypeId: Long

    /** 防御方属性 ID */
    val defendingTypeId: Long
}
