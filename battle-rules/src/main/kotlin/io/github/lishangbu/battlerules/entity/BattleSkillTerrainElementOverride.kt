package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能在指定场地下的属性覆盖规则。
 *
 * 该实体表达“技能规则 + 场地规则 => 目标属性”的结构化资料，用于场地脉冲这类会随当前场地改变本次结算属性的
 * 技能。属性覆盖会影响属性一致加成、属性克制、属性吸收、火属性解冻和属性相关道具，因此必须作为独立资料进入
 * battle-engine 的统一属性读取入口，而不能只在伤害公式里临时判断。
 *
 * 同一技能规则在同一场地下只能配置一条覆盖记录，运行时才能稳定装配为 `Map<BattleTerrain, Long>`，不需要在战斗
 * 过程中处理多条资料的优先级冲突。
 */
@Entity
@Table(name = "battle_skill_terrain_element_override")
interface BattleSkillTerrainElementOverride {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val terrainRuleId: Long

	val targetElementId: Long
	val enabled: Boolean
	val sortOrder: Int
}
