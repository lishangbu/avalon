package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pokemon_type")
interface PokemonType {
    @Id
    @PropOverride(prop = "pokemonId", columnName = "pokemon_id")
    @PropOverride(prop = "typeId", columnName = "type_id")
    val id: PokemonTypeId

    val slot: Int?
}
