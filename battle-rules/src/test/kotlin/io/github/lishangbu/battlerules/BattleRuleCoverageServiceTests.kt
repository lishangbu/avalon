package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.service.BattleRuleCoverageService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证战斗规则覆盖报告的汇总一致性。
 *
 * 没有数据库运行时时，覆盖报告只暴露最终目标账本，避免重新维护一份静态 fixture 目录。
 */
class BattleRuleCoverageServiceTests {
	private val service = BattleRuleCoverageService()

	@Test
	fun `coverage summary matches item statuses`() {
		val coverage = service.getCoverage()
		val implementedCount = coverage.items.count { it.status == "IMPLEMENTED" }
		val partialCount = coverage.items.count { it.status == "PARTIAL" }
		val plannedCount = coverage.items.count { it.status == "PLANNED" }

		assertTrue(coverage.items.isEmpty())
		assertEquals(coverage.items.size, coverage.summary.totalCount)
		assertEquals(implementedCount, coverage.summary.implementedCount)
		assertEquals(partialCount, coverage.summary.partialCount)
		assertEquals(plannedCount, coverage.summary.plannedCount)
		assertEquals(coverage.items.sumOf { it.fixtureNames.size }, coverage.summary.fixtureCount)
		assertEquals(coverage.items.groupBy { it.category }.size, coverage.matrix.size)
		assertEquals(coverage.summary.totalCount, coverage.matrix.sumOf { it.totalCount })
		assertEquals(coverage.summary.implementedCount, coverage.matrix.sumOf { it.implementedCount })
		assertEquals(coverage.summary.partialCount, coverage.matrix.sumOf { it.partialCount })
		assertEquals(coverage.summary.plannedCount, coverage.matrix.sumOf { it.plannedCount })
		assertEquals(coverage.summary.fixtureCount, coverage.matrix.sumOf { it.fixtureCount })
		assertEquals(coverage.summary.fixtureCount, coverage.fixtureSummary.fixtureReferenceCount)
		assertEquals(false, coverage.fixtureSummary.runtimeAvailable)
		assertEquals(312, coverage.targetSummary.targetRuleCount)
		assertEquals(312, coverage.targetSummary.coveredRuleCount)
		assertEquals(0, coverage.targetSummary.remainingRuleCount)
		assertEquals(100, coverage.targetSummary.implementationPercent)
		assertEquals(coverage.items.size, coverage.targetSummary.coverageItemCount)
		assertTrue(coverage.targetSummary.basis.contains("可复用规则行为族"))
	}

	@Test
	fun `coverage completeness checks stay green`() {
		val coverage = service.getCoverage()

		assertTrue(coverage.checks.isNotEmpty())
		assertTrue(coverage.checks.any { it.code == "target-count" && it.message.contains("312") })
		assertTrue(coverage.checks.all { it.status == "PASSED" })
	}
}
