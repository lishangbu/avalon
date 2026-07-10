package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 区域精灵遭遇持久化实体。
 *
 * 对应 `game_location_area_encounter` 表，仅承载一条区域精灵遭遇记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_location_area_encounter")
interface GameLocationAreaEncounters {
	/**
	 * 区域精灵遭遇记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 区域精灵遭遇引用的区域 ID。
	 *
	 * 对应数据库 `area_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val areaId: Long

	/**
	 * 区域精灵遭遇引用的精灵 ID。
	 *
	 * 对应数据库 `creature_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val creatureId: Long

	/**
	 * 区域精灵遭遇引用的遭遇方式 ID。
	 *
	 * 对应数据库 `method_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val methodId: Long

	/**
	 * 区域精灵遭遇的最低等级。
	 *
	 * 对应数据库 `min_level` 列，写入时必须提供。
	 */
	val minLevel: Int

	/**
	 * 区域精灵遭遇的最高等级。
	 *
	 * 对应数据库 `max_level` 列，写入时必须提供。
	 */
	val maxLevel: Int

	/**
	 * 区域精灵遭遇的概率。
	 *
	 * 对应数据库 `chance` 列，写入时必须提供。
	 */
	val chance: Int

	/**
	 * 区域精灵遭遇的最大概率。
	 *
	 * 对应数据库 `max_chance` 列，写入时必须提供。
	 */
	val maxChance: Int
}
