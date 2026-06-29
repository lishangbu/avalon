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
		val replayItem = coverage.items.single { it.code == "replay.deterministic-random-trace" }
		val goldenFixture = replayItem.fixtures.single {
			it.code == "golden-replay-pins-random-trace-event-fragment-and-final-hp"
		}

		assertThat(coverage.fixtureSummary.runtimeAvailable).isTrue()
		assertThat(coverage.fixtureSummary.missingFixtureCount).isZero()
		assertThat(coverage.fixtureSummary.withoutRunCount).isZero()
		assertThat(goldenFixture.fixtureId).isEqualTo(412)
		assertThat(goldenFixture.name).isEqualTo("黄金回放固定随机事件和最终体力")
		assertThat(goldenFixture.latestRunCode).isEqualTo("golden-replay-coverage-20260630-1")
		assertThat(goldenFixture.latestRunStatus).isEqualTo("PASSED")
		assertThat(coverage.checks).anySatisfy {
			assertThat(it.code).isEqualTo("fixture-data")
			assertThat(it.status).isEqualTo("PASSED")
		}
	}
}
