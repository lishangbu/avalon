package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗特性规则定义。
 *
 * 特性资料本身来自 `game_ability`，本表只维护进入战斗引擎后的触发时机、效果策略和同一时机内的结算顺序。
 * 这样可以区分“资料库里存在某个特性”和“现代战斗规则如何执行这个特性”，也便于后续逐步补全复杂特性。
 *
 * 同一特性可以在不同触发时机拥有多条规则，例如入场触发、伤害前修正、受到接触攻击后反制等。唯一键覆盖
 * `abilityId + triggerTiming + effectPolicy`，允许一个特性在同一时机下拥有多个不同策略，但禁止重复配置同一策略。
 */
@Entity
@Table(name = "battle_ability_rule")
interface BattleAbilityRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val abilityId: Long

	@Key
	val triggerTiming: String

	@Key
	val effectPolicy: String

	val triggerOrder: Int
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
