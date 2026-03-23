package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonTypeId {
    /** 宝可梦 ID */
    val pokemonId: Long

    /** 属性 ID */
    val typeId: Long
}
