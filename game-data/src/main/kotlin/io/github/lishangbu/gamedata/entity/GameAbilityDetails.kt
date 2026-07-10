package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 特性详情持久化实体。
 *
 * 对应 `game_ability_detail` 表，仅承载一条特性详情记录及其标量属性。
 * 关联项保留为独立 ID，避免把其他功能的实体聚合进当前资料对象；新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_ability_detail")
interface GameAbilityDetails {
	/**
	 * 特性详情记录的唯一标识。
	 *
	 * 新增时由 [CosIdLongUserIdGenerator] 生成；对外 JSON 通过 [LongToStringConverter] 输出为字符串，
	 * 避免 JavaScript 数值精度丢失，同时在 Kotlin 和数据库中保持 Long 类型。
	 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	/**
	 * 特性详情引用的特性 ID。
	 *
	 * 对应数据库 `ability_id` 列，写入时必须提供；关联标识以 Long 标量保存，不在该实体中聚合关联对象。
	 */
	val abilityId: Long

	/**
	 * 特性详情的效果。
	 *
	 * 对应数据库 `effect` 列，允许为空。
	 */
	val effect: String?

	/**
	 * 特性详情的短效果。
	 *
	 * 对应数据库 `short_effect` 列，允许为空。
	 */
	val shortEffect: String?

	/**
	 * 特性详情的风味说明。
	 *
	 * 对应数据库 `flavor_text` 列，允许为空。
	 */
	val flavorText: String?
}
