package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 评价效果持久化实体。
 *
 * 对应 `game_contest_effect` 表，仅承载一条评价效果记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_contest_effect")
interface GameContestEffects {
	/**
	 * 评价效果记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 评价效果的吸引力。
	 *
	 * 对应数据库 `appeal` 列，写入时必须提供。
	 */
	val appeal: Int

	/**
	 * 评价效果的干扰值。
	 *
	 * 对应数据库 `jam` 列，写入时必须提供。
	 */
	val jam: Int

	/**
	 * 评价效果的效果。
	 *
	 * 对应数据库 `effect` 列，允许为空。
	 */
	val effect: String?

	/**
	 * 评价效果的风味说明。
	 *
	 * 对应数据库 `flavor_text` 列，允许为空。
	 */
	val flavorText: String?
}
