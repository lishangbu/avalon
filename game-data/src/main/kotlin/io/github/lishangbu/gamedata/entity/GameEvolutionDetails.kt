package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 进化条件持久化实体。
 *
 * 对应 `game_evolution_detail` 表，仅承载一条进化条件记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_evolution_detail")
interface GameEvolutionDetails {
	/**
	 * 进化条件记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 进化条件引用的进化链 ID。
	 *
	 * 对应数据库 `chain_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val chainId: Long

	/**
	 * 进化条件引用的起始种类 ID。
	 *
	 * 对应数据库 `from_species_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val fromSpeciesId: Long

	/**
	 * 进化条件引用的目标种类 ID。
	 *
	 * 对应数据库 `to_species_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val toSpeciesId: Long

	/**
	 * 进化条件引用的触发器 ID。
	 *
	 * 对应数据库 `trigger_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val triggerId: Long?

	/**
	 * 进化条件引用的道具 ID。
	 *
	 * 对应数据库 `item_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val itemId: Long?

	/**
	 * 进化条件引用的持有道具 ID。
	 *
	 * 对应数据库 `held_item_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val heldItemId: Long?

	/**
	 * 进化条件引用的已掌握技能 ID。
	 *
	 * 对应数据库 `known_skill_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val knownSkillId: Long?

	/**
	 * 进化条件引用的已掌握属性 ID。
	 *
	 * 对应数据库 `known_element_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val knownElementId: Long?

	/**
	 * 进化条件引用的地点 ID。
	 *
	 * 对应数据库 `location_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val locationId: Long?

	/**
	 * 进化条件引用的队伍种类 ID。
	 *
	 * 对应数据库 `party_species_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val partySpeciesId: Long?

	/**
	 * 进化条件引用的队伍属性 ID。
	 *
	 * 对应数据库 `party_element_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val partyElementId: Long?

	/**
	 * 进化条件引用的交换种类 ID。
	 *
	 * 对应数据库 `trade_species_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val tradeSpeciesId: Long?

	/**
	 * 进化条件引用的性别 ID。
	 *
	 * 对应数据库 `gender_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val genderId: Long?

	/**
	 * 进化条件引用的地区 ID。
	 *
	 * 对应数据库 `region_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val regionId: Long?

	/**
	 * 进化条件的最低等级。
	 *
	 * 对应数据库 `min_level` 列，允许为空。
	 */
	val minLevel: Int?

	/**
	 * 进化条件的最低亲和度。
	 *
	 * 对应数据库 `min_happiness` 列，允许为空。
	 */
	val minHappiness: Int?

	/**
	 * 进化条件的最低美丽度。
	 *
	 * 对应数据库 `min_beauty` 列，允许为空。
	 */
	val minBeauty: Int?

	/**
	 * 进化条件的最低友好度。
	 *
	 * 对应数据库 `min_affection` 列，允许为空。
	 */
	val minAffection: Int?

	/**
	 * 进化条件的物攻物防关系。
	 *
	 * 对应数据库 `relative_physical_stats` 列，允许为空。
	 */
	val relativePhysicalStats: Int?

	/**
	 * 进化条件的最低承伤。
	 *
	 * 对应数据库 `min_damage_taken` 列，允许为空。
	 */
	val minDamageTaken: Int?

	/**
	 * 进化条件的最低技能数。
	 *
	 * 对应数据库 `min_move_count` 列，允许为空。
	 */
	val minMoveCount: Int?

	/**
	 * 进化条件的最低步数。
	 *
	 * 对应数据库 `min_steps` 列，允许为空。
	 */
	val minSteps: Int?

	/**
	 * 进化条件的时间段。
	 *
	 * 对应数据库 `time_of_day` 列，允许为空，最大长度为 40 个字符。
	 */
	val timeOfDay: String?

	/**
	 * 用于标识进化条件的“需要下雨”状态。
	 *
	 * 对应数据库 `needs_overworld_rain` 列，允许为空。
	 */
	val needsOverworldRain: Boolean?

	/**
	 * 用于标识进化条件的“需要倒置”状态。
	 *
	 * 对应数据库 `turn_upside_down` 列，允许为空。
	 */
	val turnUpsideDown: Boolean?

	/**
	 * 用于标识进化条件的“靠近特殊岩石”状态。
	 *
	 * 对应数据库 `near_special_rock` 列，允许为空。
	 */
	val nearSpecialRock: Boolean?

	/**
	 * 用于标识进化条件的“需要多人”状态。
	 *
	 * 对应数据库 `needs_multiplayer` 列，允许为空。
	 */
	val needsMultiplayer: Boolean?

	/**
	 * 用于标识进化条件的“默认条件”状态。
	 *
	 * 对应数据库 `is_default` 列，允许为空。
	 */
	val isDefault: Boolean?
}
