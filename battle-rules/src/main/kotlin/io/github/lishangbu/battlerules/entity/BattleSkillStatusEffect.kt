package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能附加状态效果定义。
 *
 * 一条技能规则可以拥有零到多个状态附加效果，例如造成伤害后以一定概率让目标灼伤或麻痹。
 * 状态本身仍由 `battle_status_rule` 定义，本表只描述技能和状态之间的触发目标、触发时机和概率。
 *
 * `targetScope` 和 `effectTiming` 使用稳定枚举文本，先保留为数据库可维护的字符串。战斗引擎消费时会把它们转换成
 * 强类型枚举，转换失败会暴露为规则配置错误，避免静默跳过关键战斗效果。
 */
@Entity
@Table(name = "battle_skill_status_effect")
interface BattleSkillStatusEffect {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val statusRuleId: Long

	@Key
	val targetScope: String

	@Key
	val effectTiming: String

	val chancePercent: Int
	val enabled: Boolean
	val sortOrder: Int
}
