package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "type_damage_relation")
interface TypeDamageRelation {
    /** ID */
    @Id
    @PropOverride(prop = "attackingTypeId", columnName = "attacking_type_id")
    @PropOverride(prop = "defendingTypeId", columnName = "defending_type_id")
    val id: TypeDamageRelationId

    /** 倍率 */
    @Column(name = "multiplier")
    val multiplier: Float?
}
