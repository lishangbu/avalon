package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 种类资料持久化实体。
 *
 * 对应 `game_species` 表，仅承载一条种类资料记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_species")
interface GameSpecies {
	/**
	 * 种类资料记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 种类资料的编码。
	 *
	 * 对应数据库 `code` 列，写入时必须提供，最大长度为 80 个字符。
	 */
	val code: String

	/**
	 * 种类资料的名称。
	 *
	 * 对应数据库 `name` 列，写入时必须提供，最大长度为 120 个字符。
	 */
	val name: String

	/**
	 * 种类资料的当前全国编号。
	 *
	 * 对应数据库 `national_number` 列，写入时必须提供。
	 */
	val nationalNumber: Int

	/**
	 * 种类资料引用的颜色 ID。
	 *
	 * 对应数据库 `color_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val colorId: Long?

	/**
	 * 种类资料引用的形态 ID。
	 *
	 * 对应数据库 `shape_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val shapeId: Long?

	/**
	 * 种类资料引用的栖息地 ID。
	 *
	 * 对应数据库 `habitat_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val habitatId: Long?

	/**
	 * 种类资料的性别比例。
	 *
	 * 对应数据库 `gender_rate` 列，允许为空。
	 */
	val genderRate: Int?

	/**
	 * 种类资料的捕获率。
	 *
	 * 对应数据库 `capture_rate` 列，允许为空。
	 */
	val captureRate: Int?

	/**
	 * 种类资料的初始亲和度。
	 *
	 * 对应数据库 `base_happiness` 列，允许为空。
	 */
	val baseHappiness: Int?

	/**
	 * 种类资料的孵化计数。
	 *
	 * 对应数据库 `hatch_counter` 列，允许为空。
	 */
	val hatchCounter: Int?

	/**
	 * 用于标识种类资料的“幼体”状态。
	 *
	 * 对应数据库 `baby` 列，允许为空。
	 */
	val baby: Boolean?

	/**
	 * 用于标识种类资料的“传说级”状态。
	 *
	 * 对应数据库 `legendary` 列，允许为空。
	 */
	val legendary: Boolean?

	/**
	 * 用于标识种类资料的“幻级”状态。
	 *
	 * 对应数据库 `mythical` 列，允许为空。
	 */
	val mythical: Boolean?

	/**
	 * 用于标识种类资料的“启用”状态。
	 *
	 * 对应数据库 `enabled` 列，允许为空。
	 */
	val enabled: Boolean?
}
