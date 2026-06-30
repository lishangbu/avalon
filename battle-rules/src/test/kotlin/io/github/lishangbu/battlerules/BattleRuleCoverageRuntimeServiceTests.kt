package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.service.BattleRuleCoverageService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
	classes = [BattleRulesTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=9",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
/**
 * 验证覆盖报告可以关联数据库中的公开对照 fixture 和最近一次运行结果。
 */
class BattleRuleCoverageRuntimeServiceTests(
	@Autowired private val service: BattleRuleCoverageService,
) {
	@Test
	fun `coverage report includes persisted fixture runtime status`() {
		val coverage = service.getCoverage()
		val goldenItem = coverage.items.single { it.code == "golden-replay-pins-random-trace-event-fragment-and-final-hp" }
		val goldenFixture = goldenItem.fixtures.single()

		assertThat(coverage.fixtureSummary.runtimeAvailable).isTrue()
		assertThat(coverage.summary.totalCount).isEqualTo(412)
		assertThat(coverage.summary.fixtureCount).isEqualTo(412)
		assertThat(coverage.targetSummary.coverageItemCount).isEqualTo(412)
		assertThat(coverage.fixtureSummary.missingFixtureCount).isZero()
		assertThat(coverage.fixtureSummary.withoutRunCount).isZero()
		assertThat(goldenItem.name).isEqualTo("黄金回放固定随机事件和最终体力")
		assertThat(goldenItem.category).isEqualTo("随机/回放")
		assertThat(goldenFixture.fixtureId).isEqualTo(412)
		assertThat(goldenFixture.name).isEqualTo("黄金回放固定随机事件和最终体力")
		assertThat(goldenFixture.latestRunCode).isEqualTo("golden-replay-coverage-20260630-1")
		assertThat(goldenFixture.latestRunStatus).isEqualTo("PASSED")
		assertThat(coverage.checks.filter { it.code in setOf("golden-replay", "fixture-data", "fixture-latest-run") })
			.allSatisfy { assertThat(it.status).isEqualTo("PASSED") }
	}
}
