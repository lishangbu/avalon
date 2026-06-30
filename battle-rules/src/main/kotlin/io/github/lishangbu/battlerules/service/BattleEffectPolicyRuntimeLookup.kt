package io.github.lishangbu.battlerules.service

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

/**
 * 特性与道具效果策略读取器。
 *
 * 特性、道具的运行时解释分为两步：本类只按资料表顺序读取启用的 `effect_policy` 文本；
 * [BattleRuntimePolicyMapper] 里的扩展函数再把这些稳定 code 翻译成 battle-engine 的强类型效果。
 * 这样 SQL 读取和策略解释不会互相夹杂：新增资料列时改读取器，新增引擎效果时改 mapper。
 *
 * 当前没有在这里做存在性校验。原因是特性/道具可能暂时没有接入引擎的显式规则，空列表表示“没有运行时 hook”，
 * 不等价于基础资料不存在；基础资料的存在性应由队伍维护或资料 CRUD 自身保证。
 */
@Component
class BattleEffectPolicyRuntimeLookup(
	private val jdbcTemplate: JdbcTemplate,
) {
	fun enabledAbilityPolicies(abilityId: Long): List<String> =
		jdbcTemplate.query(
			"""
			select effect_policy
			from battle_ability_rule
			where ability_id = ? and enabled = true
			order by trigger_order, sort_order, id
			""".trimIndent(),
			{ rs, _ -> rs.getString("effect_policy") },
			abilityId,
		)

	fun enabledItemPolicies(itemId: Long): List<String> =
		jdbcTemplate.query(
			"""
			select effect_policy
			from battle_item_rule
			where item_id = ? and enabled = true
			order by trigger_order, sort_order, id
			""".trimIndent(),
			{ rs, _ -> rs.getString("effect_policy") },
			itemId,
		)
}
