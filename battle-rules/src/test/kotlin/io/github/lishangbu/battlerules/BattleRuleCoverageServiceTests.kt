package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.service.BattleRuleCoverageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * 验证战斗规则覆盖报告只依赖代码内覆盖账本。
 *
 * 这组测试刻意不启动数据库：fixture 管理表已经移除，规则正确性由 `battle-engine`
 * 的行为单元测试负责；这里仅保证管理端看到的 312 条规则行为统计不会悄悄漂移。
 */
class BattleRuleCoverageServiceTests {
	private val service = BattleRuleCoverageService()

	@Test
	fun `coverage report summarizes all modern battle rule behavior groups`() {
		val coverage = service.getCoverage()

		assertThat(coverage.summary.totalCount).isEqualTo(312)
		assertThat(coverage.summary.implementedCount).isEqualTo(312)
		assertThat(coverage.summary.plannedCount).isZero()
		assertThat(coverage.targetSummary.targetRuleCount).isEqualTo(312)
		assertThat(coverage.targetSummary.coveredRuleCount).isEqualTo(312)
		assertThat(coverage.targetSummary.coverageItemCount).isEqualTo(12)
		assertThat(coverage.items).hasSize(12)
		assertThat(coverage.items).allSatisfy { item ->
			assertThat(item.ruleCount).isPositive()
			assertThat(item.fixtureNames).isNotEmpty()
			assertThat(item.fixtures).hasSameSizeAs(item.fixtureNames)
			assertThat(item.note).isNotBlank()
		}
		assertThat(coverage.items.map { it.code }).contains(
			"format-and-team-validation",
			"damage-formula-stat-element-rounding",
			"random-replay-public-reference",
		)
		assertThat(coverage.matrix.sumOf { it.totalCount }).isEqualTo(312)
		assertThat(coverage.checks).allSatisfy {
			assertThat(it.status).isEqualTo("PASSED")
		}
	}
}
