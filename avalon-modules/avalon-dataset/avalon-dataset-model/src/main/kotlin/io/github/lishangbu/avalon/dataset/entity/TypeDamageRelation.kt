package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "type_damage_relation")
interface TypeDamageRelation {
    @Id
    @PropOverride(prop = "attackingTypeId", columnName = "attacking_type_id")
    @PropOverride(prop = "defendingTypeId", columnName = "defending_type_id")
    val id: TypeDamageRelationId

    @Column(name = "multiplier")
    val multiplier: Float?
}
