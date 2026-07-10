package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 树果资料持久化实体。
 *
 * 对应 `game_berry` 表，仅承载一条树果资料记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_berry")
interface GameBerries {
	/**
	 * 树果资料记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 树果资料的编码。
	 *
	 * 对应数据库 `code` 列，写入时必须提供，最大长度为 80 个字符。
	 */
	val code: String

	/**
	 * 树果资料的名称。
	 *
	 * 对应数据库 `name` 列，写入时必须提供，最大长度为 120 个字符。
	 */
	val name: String

	/**
	 * 树果资料引用的道具 ID。
	 *
	 * 对应数据库 `item_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val itemId: Long?

	/**
	 * 树果资料引用的硬度 ID。
	 *
	 * 对应数据库 `firmness_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val firmnessId: Long?

	/**
	 * 树果资料引用的自然效果属性 ID。
	 *
	 * 对应数据库 `natural_gift_element_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val naturalGiftElementId: Long?

	/**
	 * 树果资料的成长时间。
	 *
	 * 对应数据库 `growth_time` 列，允许为空。
	 */
	val growthTime: Int?

	/**
	 * 树果资料的最大收获。
	 *
	 * 对应数据库 `max_harvest` 列，允许为空。
	 */
	val maxHarvest: Int?

	/**
	 * 树果资料的自然效果威力。
	 *
	 * 对应数据库 `natural_gift_power` 列，允许为空。
	 */
	val naturalGiftPower: Int?

	/**
	 * 树果资料的尺寸。
	 *
	 * 对应数据库 `size` 列，允许为空。
	 */
	val size: Int?

	/**
	 * 树果资料的顺滑度。
	 *
	 * 对应数据库 `smoothness` 列，允许为空。
	 */
	val smoothness: Int?

	/**
	 * 树果资料的土壤干燥度。
	 *
	 * 对应数据库 `soil_dryness` 列，允许为空。
	 */
	val soilDryness: Int?

	/**
	 * 用于标识树果资料的“启用”状态。
	 *
	 * 对应数据库 `enabled` 列，写入时必须提供。
	 */
	val enabled: Boolean
}
