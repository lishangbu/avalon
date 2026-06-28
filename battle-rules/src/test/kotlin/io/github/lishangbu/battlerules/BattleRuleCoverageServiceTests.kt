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
		assertTrue(coverage.items.any { it.code == "status.immunity-and-grounding" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-stat-stage" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-weather" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "ability.switch-in-terrain" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "field.environment-duration" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "field.side-entry-hazard" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "turn.multi-hit-and-locked-move" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.major-status-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.stat-stage-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "skill.hp-effects" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.psychic-priority-block" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.setting-skill" && it.status == "IMPLEMENTED" })
		assertTrue(coverage.items.any { it.code == "terrain.speed-ability" && it.status == "IMPLEMENTED" })
	}
}
