package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
@Table(name = "pokemon_species")
interface PokemonSpecies {
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    val internalName: String?

    val name: String?

    val sortingOrder: Int?

    val genderRate: Int?

    val captureRate: Int?

    val baseHappiness: Int?

    val isBaby: Boolean?

    val isLegendary: Boolean?

    val isMythical: Boolean?

    val hatchCounter: Int?

    val hasGenderDifferences: Boolean?

    val formsSwitchable: Boolean?

    @ManyToOne
    @JoinColumn(name = "growth_rate_id")
    val growthRate: GrowthRate?

    @ManyToOne
    @JoinColumn(name = "pokemon_color_id")
    val pokemonColor: PokemonColor?

    @ManyToOne
    @JoinColumn(name = "pokemon_shape_id")
    val pokemonShape: PokemonShape?

    @Column(name = "evolves_from_species_id")
    val evolvesFromSpeciesId: Long?

    @Column(name = "evolution_chain_id")
    val evolutionChainId: Long?

    @ManyToOne
    @JoinColumn(name = "pokemon_habitat_id")
    val pokemonHabitat: PokemonHabitat?
}
