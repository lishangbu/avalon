package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 进化链节点持久化实体。
 *
 * 对应 `game_evolution_node` 表，仅承载一条进化链节点记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_evolution_node")
interface GameEvolutionNodes {
	/**
	 * 进化链节点记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 进化链节点引用的进化链 ID。
	 *
	 * 对应数据库 `chain_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val chainId: Long

	/**
	 * 进化链节点引用的种类 ID。
	 *
	 * 对应数据库 `species_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val speciesId: Long

	/**
	 * 进化链节点引用的父级种类 ID。
	 *
	 * 对应数据库 `parent_species_id` 列，允许为空；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val parentSpeciesId: Long?

	/**
	 * 用于标识进化链节点的“幼体”状态。
	 *
	 * 对应数据库 `baby` 列，写入时必须提供。
	 */
	val baby: Boolean

	/**
	 * 进化链节点的节点顺序。
	 *
	 * 对应数据库 `node_order` 列，写入时必须提供。
	 */
	val nodeOrder: Int
}
