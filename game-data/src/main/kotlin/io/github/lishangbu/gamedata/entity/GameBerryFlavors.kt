package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 树果口味持久化实体。
 *
 * 对应 `game_berry_flavor` 表，仅承载一条树果口味记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_berry_flavor")
interface GameBerryFlavors {
	/**
	 * 树果口味记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 树果口味的编码。
	 *
	 * 对应数据库 `code` 列，写入时必须提供，最大长度为 80 个字符。
	 */
	val code: String

	/**
	 * 树果口味的名称。
	 *
	 * 对应数据库 `name` 列，写入时必须提供，最大长度为 120 个字符。
	 */
	val name: String

	/**
	 * 树果口味引用的评分类别 ID。
	 *
	 * 对应数据库 `contest_type_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val contestTypeId: Long?

	/**
	 * 用于标识树果口味的“启用”状态。
	 *
	 * 对应数据库 `enabled` 列，写入时必须提供。
	 */
	val enabled: Boolean
}
