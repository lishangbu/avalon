package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 种类详情持久化实体。
 *
 * 对应 `game_species_detail` 表，仅承载一条种类详情记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_species_detail")
interface GameSpeciesDetails {
	/**
	 * 种类详情记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 种类详情引用的种类 ID。
	 *
	 * 对应数据库 `species_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val speciesId: Long

	/**
	 * 种类详情引用的成长速率 ID。
	 *
	 * 对应数据库 `growth_rate_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val growthRateId: Long?

	/**
	 * 种类详情引用的进化来源种类 ID。
	 *
	 * 对应数据库 `evolves_from_species_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val evolvesFromSpeciesId: Long?

	/**
	 * 种类详情引用的进化链 ID。
	 *
	 * 对应数据库 `evolution_chain_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val evolutionChainId: Long?

	/**
	 * 种类详情的排序。
	 *
	 * 对应数据库 `sort_order` 列，允许为空。
	 */
	val sortOrder: Int?

	/**
	 * 用于标识种类详情的“性别差异”状态。
	 *
	 * 对应数据库 `gender_differences` 列，写入时必须提供。
	 */
	val genderDifferences: Boolean

	/**
	 * 用于标识种类详情的“形态可切换”状态。
	 *
	 * 对应数据库 `forms_switchable` 列，写入时必须提供。
	 */
	val formsSwitchable: Boolean

	/**
	 * 种类详情的分类。
	 *
	 * 对应数据库 `genus` 列，允许为空，最大长度为 200 个字符。
	 */
	val genus: String?

	/**
	 * 种类详情的风味说明。
	 *
	 * 对应数据库 `flavor_text` 列，允许为空。
	 */
	val flavorText: String?
}
