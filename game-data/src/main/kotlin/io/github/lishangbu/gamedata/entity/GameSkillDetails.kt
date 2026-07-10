package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 技能详情持久化实体。
 *
 * 对应 `game_skill_detail` 表，仅承载一条技能详情记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_skill_detail")
interface GameSkillDetails {
	/**
	 * 技能详情记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 技能详情引用的技能 ID。
	 *
	 * 对应数据库 `skill_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val skillId: Long

	/**
	 * 技能详情引用的异常 ID。
	 *
	 * 对应数据库 `ailment_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val ailmentId: Long?

	/**
	 * 技能详情引用的分类 ID。
	 *
	 * 对应数据库 `category_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val categoryId: Long?

	/**
	 * 技能详情引用的目标 ID。
	 *
	 * 对应数据库 `target_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val targetId: Long?

	/**
	 * 技能详情引用的评分类别 ID。
	 *
	 * 对应数据库 `contest_type_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val contestTypeId: Long?

	/**
	 * 技能详情引用的评价效果 ID。
	 *
	 * 对应数据库 `contest_effect_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val contestEffectId: Long?

	/**
	 * 技能详情引用的高级评价效果 ID。
	 *
	 * 对应数据库 `advanced_contest_effect_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val advancedContestEffectId: Long?

	/**
	 * 技能详情的最少命中。
	 *
	 * 对应数据库 `min_hits` 列，允许为空。
	 */
	val minHits: Int?

	/**
	 * 技能详情的最多命中。
	 *
	 * 对应数据库 `max_hits` 列，允许为空。
	 */
	val maxHits: Int?

	/**
	 * 技能详情的最少回合。
	 *
	 * 对应数据库 `min_turns` 列，允许为空。
	 */
	val minTurns: Int?

	/**
	 * 技能详情的最多回合。
	 *
	 * 对应数据库 `max_turns` 列，允许为空。
	 */
	val maxTurns: Int?

	/**
	 * 技能详情的吸取值。
	 *
	 * 对应数据库 `drain` 列，允许为空。
	 */
	val drain: Int?

	/**
	 * 技能详情的回复值。
	 *
	 * 对应数据库 `healing` 列，允许为空。
	 */
	val healing: Int?

	/**
	 * 技能详情的暴击修正。
	 *
	 * 对应数据库 `crit_rate` 列，允许为空。
	 */
	val critRate: Int?

	/**
	 * 技能详情的异常概率。
	 *
	 * 对应数据库 `ailment_chance` 列，允许为空。
	 */
	val ailmentChance: Int?

	/**
	 * 技能详情的畏缩概率。
	 *
	 * 对应数据库 `flinch_chance` 列，允许为空。
	 */
	val flinchChance: Int?

	/**
	 * 技能详情的数值变化概率。
	 *
	 * 对应数据库 `stat_chance` 列，允许为空。
	 */
	val statChance: Int?

	/**
	 * 技能详情的效果。
	 *
	 * 对应数据库 `effect` 列，允许为空。
	 */
	val effect: String?

	/**
	 * 技能详情的短效果。
	 *
	 * 对应数据库 `short_effect` 列，允许为空。
	 */
	val shortEffect: String?

	/**
	 * 技能详情的风味说明。
	 *
	 * 对应数据库 `flavor_text` 列，允许为空。
	 */
	val flavorText: String?
}
