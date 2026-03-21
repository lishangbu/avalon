package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonTypeId {
    val pokemonId: Long

    val typeId: Long
}
