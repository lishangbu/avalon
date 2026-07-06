package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.entity.BattleAbilityRule
import io.github.lishangbu.battlerules.entity.BattleItemRule
import io.github.lishangbu.battlerules.entity.abilityId
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.itemId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.triggerOrder
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Component

/**
 * 特性与道具效果策略读取器。
 *
 * 特性、道具的运行时解释分为两步：本类只按资料表顺序读取启用的 `effect_policy` 文本；
 * [BattleRuntimePolicyMapper] 里的扩展函数再把这些稳定 code 翻译成 battle-engine 的强类型效果。
 * 这样 SQL 读取和策略解释不会互相夹杂：新增资料列时改读取器，新增引擎效果时改 mapper。
 *
 * 本类不校验基础特性/道具 ID 是否存在：正数 ID 查不到启用规则时返回空列表，表示当前没有结构化运行时效果。
 * 但只要查到了启用 policy，后续 assembler 会逐条校验是否被引擎支持，未知 policy 不会被静默吞掉。
 */
@Component
class BattleEffectPolicyRuntimeLookup(
	private val sqlClient: KSqlClient,
) {
	fun enabledAbilityPolicies(abilityId: Long): List<String> =
		sqlClient.createQuery(BattleAbilityRule::class) {
			where(table.abilityId eq abilityId)
			where(table.enabled eq true)
			orderBy(table.triggerOrder, table.sortOrder, table.id)
			select(table.effectPolicy)
		}.execute()

	fun enabledItemPolicies(itemId: Long): List<String> =
		sqlClient.createQuery(BattleItemRule::class) {
			where(table.itemId eq itemId)
			where(table.enabled eq true)
			orderBy(table.triggerOrder, table.sortOrder, table.id)
			select(table.effectPolicy)
		}.execute()
}
