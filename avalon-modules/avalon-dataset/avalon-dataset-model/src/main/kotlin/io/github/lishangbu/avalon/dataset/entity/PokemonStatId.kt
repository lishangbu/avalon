package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonStatId {
    /** 宝可梦 ID */
    val pokemonId: Long

    /** 能力值 ID */
    val statId: Long
}
