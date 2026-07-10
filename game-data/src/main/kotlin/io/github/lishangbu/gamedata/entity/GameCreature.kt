package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 精灵资料持久化实体。
 *
 * 对应 `game_creature` 表，仅承载一条精灵资料记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_creature")
interface GameCreature {
	/**
	 * 精灵资料记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 精灵资料的编码。
	 *
	 * 对应数据库 `code` 列，写入时必须提供，最大长度为 80 个字符。
	 */
	val code: String

	/**
	 * 精灵资料的名称。
	 *
	 * 对应数据库 `name` 列，写入时必须提供，最大长度为 120 个字符。
	 */
	val name: String

	/**
	 * 精灵资料引用的种类 ID。
	 *
	 * 对应数据库 `species_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val speciesId: Long

	/**
	 * 精灵资料继承缺失数据时引用的来源精灵 ID。
	 *
	 * 对应数据库 `inherits_from_creature_id` 列，允许为空；数据库约束要求来源精灵属于同一种类且不能自引用。
	 */
	val inheritsFromCreatureId: Long?

	/**
	 * 精灵资料的高度。
	 *
	 * 对应数据库 `height` 列，允许为空。
	 */
	val height: Int?

	/**
	 * 精灵资料的重量。
	 *
	 * 对应数据库 `weight` 列，允许为空。
	 */
	val weight: Int?

	/**
	 * 精灵资料的基础经验。
	 *
	 * 对应数据库 `base_experience` 列，允许为空。
	 */
	val baseExperience: Int?

	/**
	 * 精灵资料的排序。
	 *
	 * 对应数据库 `sort_order` 列，允许为空。
	 */
	val sortOrder: Int?

	/**
	 * 用于标识精灵资料的“默认形态”状态。
	 *
	 * 对应数据库 `default_form` 列，允许为空。
	 */
	val defaultForm: Boolean?

	/**
	 * 用于标识精灵资料的“启用”状态。
	 *
	 * 对应数据库 `enabled` 列，允许为空。
	 */
	val enabled: Boolean?
}
