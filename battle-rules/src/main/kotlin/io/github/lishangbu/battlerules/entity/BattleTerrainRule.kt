package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗地形规则定义。
 *
 * 地形通常只影响接触地面的成员，后续引擎会在站位和成员状态计算后再应用 `effectPolicy`。
 * 数据层只维护可被管理端编辑的稳定资料。
 */
@Entity
@Table(name = "battle_terrain_rule")
interface BattleTerrainRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val effectPolicy: String
	val defaultDurationTurns: Int?
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
