package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 活动能力性格影响持久化实体。
 *
 * 对应 `game_event_stat_nature_effect` 表，仅承载一条活动能力性格影响记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_event_stat_nature_effect")
interface GameEventStatNatureEffects {
	/**
	 * 活动能力性格影响记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 活动能力性格影响引用的活动能力项 ID。
	 *
	 * 对应数据库 `event_stat_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val eventStatId: Long

	/**
	 * 活动能力性格影响引用的性格 ID。
	 *
	 * 对应数据库 `nature_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val natureId: Long

	/**
	 * 活动能力性格影响的影响类型。
	 *
	 * 对应数据库 `effect_type` 列，写入时必须提供，最大长度为 20 个字符。
	 */
	val effectType: String
}
