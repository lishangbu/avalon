package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.PropOverride
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "pokemon_ability")
interface PokemonAbility {
    /** ID */
    @Id
    @PropOverride(prop = "pokemonId", columnName = "pokemon_id")
    @PropOverride(prop = "abilityId", columnName = "ability_id")
    val id: PokemonAbilityId

    /** 是否隐藏 */
    val hidden: Boolean?

    /** 槽位 */
    val slot: Int?
}
