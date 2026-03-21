package io.github.lishangbu.avalon.dataset.entity

import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pokemon_stat")
interface PokemonStat {
    @Id
    @PropOverride(prop = "pokemonId", columnName = "pokemon_id")
    @PropOverride(prop = "statId", columnName = "stat_id")
    val id: PokemonStatId

    val baseStat: Int?

    val effort: Int?
}
