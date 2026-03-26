package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.*

@Entity
interface PokemonSpecies {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /** 内部名称 */
    val internalName: String?

    /** 名称 */
    val name: String?

    /** 排序顺序 */
    val sortingOrder: Int?

    /** 性别比率 */
    val genderRate: Int?

    /** 捕获速率 */
    val captureRate: Int?

    /** 基础亲密度 */
    val baseHappiness: Int?

    /** 是否幼年 */
    val baby: Boolean?

    /** 是否传说 */
    val legendary: Boolean?

    /** 是否幻之 */
    val mythical: Boolean?

    /** 孵化计数器 */
    val hatchCounter: Int?

    /** 是否性别差异 */
    val hasGenderDifferences: Boolean?

    /** 形态可切换 */
    val formsSwitchable: Boolean?

    /** 成长速率 */
    @ManyToOne
    @JoinColumn(name = "growth_rate_id")
    val growthRate: GrowthRate?

    /** 宝可梦颜色 */
    @ManyToOne
    @JoinColumn(name = "pokemon_color_id")
    val pokemonColor: PokemonColor?

    /** 宝可梦形态 */
    @ManyToOne
    @JoinColumn(name = "pokemon_shape_id")
    val pokemonShape: PokemonShape?

    /** 进化从种族 ID */
    val evolvesFromSpeciesId: Long?

    /** 进化链 ID */
    val evolutionChainId: Long?

    /** 宝可梦栖息地 */
    @ManyToOne
    @JoinColumn(name = "pokemon_habitat_id")
    val pokemonHabitat: PokemonHabitat?
}
