package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 技能能力阶级变化定义。
 *
 * 现代战斗中，很多技能会提升或降低攻击、防御、速度、命中、闪避等能力阶级。本表以一条记录表达一次阶级变化：
 * 哪个技能规则、影响哪个能力项、作用在谁身上、什么时候结算、变化几级、概率是多少。
 *
 * 能力项引用 `game_stat`，这样后续如果基础资料维护了新的能力项名称或排序，战斗效果表不需要复制文本。
 * 引擎会把 `stageDelta` 限制在现代规则允许的能力阶级边界内结算，本表只保存单次效果的配置。
 */
@Entity
@Table(name = "battle_skill_stat_stage_effect")
interface BattleSkillStatStageEffect {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val skillRuleId: Long

	@Key
	val statId: Long

	@Key
	val targetScope: String

	@Key
	val effectTiming: String

	@Key
	val stageDelta: Int

	val chancePercent: Int
	val enabled: Boolean
	val sortOrder: Int
}
