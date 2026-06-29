package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.service.BattleRuleCoverageService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证战斗规则覆盖报告的汇总一致性。
 *
 * 覆盖报告是手工维护的代码清单，测试重点不是业务规则本身，而是防止新增条目时漏更新状态或汇总计算。
 */
class BattleRuleCoverageServiceTests {
	private val service = BattleRuleCoverageService()

	@Test
	fun `coverage summary matches item statuses`() {
		val coverage = service.getCoverage()
		val implementedCount = coverage.items.count { it.status == "IMPLEMENTED" }
		val partialCount = coverage.items.count { it.status == "PARTIAL" }
		val plannedCount = coverage.items.count { it.status == "PLANNED" }

		assertTrue(coverage.items.isNotEmpty())
		assertEquals(coverage.items.size, coverage.summary.totalCount)
		assertEquals(implementedCount, coverage.summary.implementedCount)
		assertEquals(partialCount, coverage.summary.partialCount)
		assertEquals(plannedCount, coverage.summary.plannedCount)
		assertEquals(coverage.items.sumOf { it.fixtureNames.size }, coverage.summary.fixtureCount)
		assertEquals(312, coverage.targetSummary.targetRuleCount)
		assertEquals(83, coverage.targetSummary.coveredRuleCount)
		assertEquals(229, coverage.targetSummary.remainingRuleCount)
		assertEquals(26, coverage.targetSummary.implementationPercent)
		assertEquals(coverage.items.size, coverage.targetSummary.coverageItemCount)
		assertTrue(coverage.targetSummary.basis.contains("可复用规则行为族"))
		assertTrue(coverage.items.any { it.code == "status.volatile-flinch-confusion" })
		assertTrue(coverage.items.any { it.code == "format.max-turn-limit" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.grassy-heal" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "status.paralysis-speed-action" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "weather.sun-rain-damage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "weather.healing-ability" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "weather.setting-skill" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.accuracy-evasion-stage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "status.freeze" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "status.burn-physical-damage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "damage.fixed-damage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "status.immunity-and-grounding" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-stat-stage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-weather" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-terrain" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.contact-status" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "damage.full-hp-survival" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.held-core-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "field.environment-duration" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "field.side-entry-hazard" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.multi-hit-and-locked-move" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.recharge-after-use" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.charge-before-use" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.substitute" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.major-status-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.stat-stage-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.hp-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.psychic-priority-block" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.status-priority-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.priority-move-block" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.element-absorb-heal" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.element-absorb-stat" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.element-damage-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.low-hp-element-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.weather-element-damage-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.super-effective-damage-reduction" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.full-hp-damage-reduction" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.damage-class-damage-reduction" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.defending-stat-multiplier" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.attacking-stat-multiplier" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.same-element-bonus-override" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.tagged-skill-damage-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.indirect-damage-immunity" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.skill-recoil-damage-immunity" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.critical-hit-immunity" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.ignore-opponent-damage-stat-stages" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.ignore-opponent-accuracy-stat-stages" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.ignore-target-ability-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.sound-based-skill-immunity" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.conditional-damage-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.element-damage-boost" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.element-damage-reduction" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.major-status-cure" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "item.volatile-status-cure" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.setting-skill" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.speed-ability" && it.status == "IMPLEMENTED" })
	}
}
