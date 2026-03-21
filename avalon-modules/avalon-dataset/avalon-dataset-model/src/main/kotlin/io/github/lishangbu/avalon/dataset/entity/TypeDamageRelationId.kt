package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface TypeDamageRelationId {
    val attackingTypeId: Long

    val defendingTypeId: Long
}
