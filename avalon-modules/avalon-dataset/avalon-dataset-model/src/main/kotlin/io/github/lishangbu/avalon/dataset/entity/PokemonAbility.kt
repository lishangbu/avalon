package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pokemon_ability")
interface PokemonAbility {
    @Id
    @PropOverride(prop = "pokemonId", columnName = "pokemon_id")
    @PropOverride(prop = "abilityId", columnName = "ability_id")
    val id: PokemonAbilityId

    val isHidden: Boolean?

    val slot: Int?
}
