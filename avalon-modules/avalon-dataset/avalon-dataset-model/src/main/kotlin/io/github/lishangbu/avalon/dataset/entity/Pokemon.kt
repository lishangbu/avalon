package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pokemon")
interface Pokemon {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    val height: Int?

    val weight: Int?

    val baseExperience: Int?

    val sortingOrder: Int?

    @ManyToOne
    @JoinColumn(name = "pokemon_species_id")
    val pokemonSpecies: PokemonSpecies?
}
