package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface Pokemon {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 身高 */
    val height: Int?

    /** 体重 */
    val weight: Int?

    /** 基础经验 */
    val baseExperience: Int?

    /** 排序顺序 */
    val sortingOrder: Int?

    /** 宝可梦种族 */
    @ManyToOne
    @JoinColumn(name = "pokemon_species_id")
    val pokemonSpecies: PokemonSpecies?
}
