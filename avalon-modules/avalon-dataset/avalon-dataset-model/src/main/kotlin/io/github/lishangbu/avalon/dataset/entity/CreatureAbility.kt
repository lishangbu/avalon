package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride

@Entity
interface CreatureAbility {
    /** ID */
    @Id
    @PropOverride(prop = "creatureId", columnName = "pokemon_id")
    @PropOverride(prop = "abilityId", columnName = "ability_id")
    val id: CreatureAbilityId

    /** 是否隐藏 */
    val hidden: Boolean?

    /** 槽位 */
    val slot: Int?
}
