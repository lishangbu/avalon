package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能能力阶级操作定义。
 *
 * 本表维护“清除、复制、交换、取反”这类不能用普通阶级加减表达的技能效果。每条记录只作用于一个能力项，
 * 因此力量互换、防守互换、自我暗示或黑雾这类多能力项技能，会由多条记录组合成完整规则。
 *
 * `sourceScope` 仅在复制和交换时使用；清除和取反只有目标范围。目标范围允许 `ALL_ACTIVE`，用于全场清除类效果。
 */
@Entity
@Table(name = "battle_skill_stat_stage_operation")
interface BattleSkillStatStageOperation {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val statId: Long

	@Key
	val operationKind: String

	@Key
	val targetScope: String

	@Key
	val sourceScope: String?

	@Key
	val effectTiming: String

	val chancePercent: Int
	val enabled: Boolean
	val sortOrder: Int
}

