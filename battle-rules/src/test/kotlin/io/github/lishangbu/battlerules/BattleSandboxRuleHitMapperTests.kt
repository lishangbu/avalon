package io.github.lishangbu.battlerules

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battlerules.service.BattleSandboxRuleHitMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 沙盒规则命中映射测试。
 *
 * 312 条规则覆盖账本最终落到 12 个规则族，沙盒页面只展示这些规则族的命中摘要。这里直接枚举
 * [BattleEvent] 的 sealed 子类型，保证新增事件时必须归入某个规则族；否则生产排障时会出现事件流有事实、
 * 规则命中表却没有对应规则入口的问题。
 */
class BattleSandboxRuleHitMapperTests {
	@Test
	fun `sandbox rule hit families cover every battle event`() {
		val mapper = BattleSandboxRuleHitMapper()
		val eventTypes = BattleEvent::class.sealedSubclasses.mapNotNull { it.simpleName }.sorted()
		val missingFamilies = eventTypes.filter { mapper.familyCodeForEventType(it).isNullOrBlank() }

		assertThat(missingFamilies)
			.withFailMessage("新增 BattleEvent 后必须登记到沙盒规则命中规则族，避免管理页和覆盖报告漏掉事件：$missingFamilies")
			.isEmpty()
		assertThat(mapper.ruleHitFamilyCodes()).containsExactly(
			"format-and-team-validation",
			"lifecycle-switch-faint-result",
			"turn-flow-action-ordering",
			"target-scope-redirection",
			"hit-protect-substitute-immunity-reflect",
			"damage-formula-stat-element-rounding",
			"major-volatile-persistent-status",
			"weather-terrain-field-side-condition",
			"skill-effect-family",
			"ability-effect-family",
			"item-effect-family",
			"random-replay-public-reference",
		)
	}
}
