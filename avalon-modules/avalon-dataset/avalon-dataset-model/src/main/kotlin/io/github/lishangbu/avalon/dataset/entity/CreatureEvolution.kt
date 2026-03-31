package io.github.lishangbu.avalon.dataset.entity

import io.github.lishangbu.avalon.jimmer.id.SnowflakeIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.JoinColumn
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table

@Entity
@Table(name = "creature_evolution")
interface CreatureEvolution {
    /** ID */
    @Id
    @GeneratedValue(generatorType = SnowflakeIdGenerator::class)
    @JsonConverter(LongToStringConverter::class)
    val id: Long

    /**
     * 分支顺序。
     *
     * 对应 PokeAPI `evolves_to` 数组顺序，用来稳定恢复分支结构。
     */
    val branchSortOrder: Int?

    /**
     * 条件顺序。
     *
     * 同一 `from -> to` 下，多条记录表示多个 `evolution_details` 对象，
     * 也就是同一条边的多种候选进化方式，而不是自关联链表。
     */
    val detailSortOrder: Int?

    /** 是否需要多人联机 */
    val needsMultiplayer: Boolean?

    /** 是否需要大地图下雨 */
    val needsOverworldRain: Boolean?

    /** 是否需要设备倒置 */
    val turnUpsideDown: Boolean?

    /** 时间段 */
    val timeOfDay: String?

    /** 性别 */
    @ManyToOne
    val gender: Gender?

    /** 最低亲密互动值 */
    val minAffection: Int?

    /** 最低美丽值 */
    val minBeauty: Int?

    /** 最低承伤值 */
    val minDamageTaken: Int?

    /** 最低亲密度 */
    val minHappiness: Int?

    /** 最低等级 */
    val minLevel: Int?

    /** 最低招式使用次数 */
    val minMoveCount: Int?

    /** 最低步数 */
    val minSteps: Int?

    /** 相对物攻物防关系 */
    val relativePhysicalStats: Int?

    /** 基础形态 */
    @ManyToOne
    @JoinColumn(name = "base_variant_id")
    val baseVariant: CreatureVariant?

    /** 地区 */
    @ManyToOne
    val region: Region?

    /** 进化链 */
    @ManyToOne
    val evolutionChain: EvolutionChain?

    /**
     * 起始种族。
     *
     * `pokemon_evolution` 是边表，不做自关联；
     * 进化图通过 `fromCreatureSpecies -> toCreatureSpecies` 表达。
     */
    @ManyToOne
    @JoinColumn(name = "from_creature_species_id")
    val fromCreatureSpecies: CreatureSpecies?

    /** 目标种族 */
    @ManyToOne
    @JoinColumn(name = "to_creature_species_id")
    val toCreatureSpecies: CreatureSpecies?

    /** 携带道具 */
    @ManyToOne
    val heldItem: Item?

    /** 使用道具 */
    @ManyToOne
    val item: Item?

    /** 已学会招式 */
    @ManyToOne
    val knownMove: Move?

    /** 已学会招式属性 */
    @ManyToOne
    val knownMoveType: Type?

    /** 地点 */
    @ManyToOne
    val location: Location?

    /** 同队种族 */
    @ManyToOne
    @JoinColumn(name = "party_creature_species_id")
    val partyCreatureSpecies: CreatureSpecies?

    /** 同队属性 */
    @ManyToOne
    val partyType: Type?

    /** 交换种族 */
    @ManyToOne
    @JoinColumn(name = "trade_creature_species_id")
    val tradeCreatureSpecies: CreatureSpecies?

    /** 进化触发方式 */
    @ManyToOne
    val trigger: EvolutionTrigger?

    /** 使用招式 */
    @ManyToOne
    val usedMove: Move?
}
