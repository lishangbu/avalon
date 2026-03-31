package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.Embeddable

@Embeddable
interface PokemonFormTypeId {
    /** 宝可梦形态 ID */
    val pokemonFormId: Long

    /** 属性 ID */
    val typeId: Long
}
