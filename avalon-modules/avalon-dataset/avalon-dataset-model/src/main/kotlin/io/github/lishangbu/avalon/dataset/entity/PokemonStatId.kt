package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonStatId {
    val pokemonId: Long

    val statId: Long
}
