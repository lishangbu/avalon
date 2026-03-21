package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonAbilityId {
    val pokemonId: Long

    val abilityId: Long
}
