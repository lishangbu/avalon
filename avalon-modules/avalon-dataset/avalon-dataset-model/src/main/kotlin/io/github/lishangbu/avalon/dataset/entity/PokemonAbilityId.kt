package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonAbilityId {
    /** 宝可梦 ID */
    val pokemonId: Long

    /** 特性 ID */
    val abilityId: Long
}
