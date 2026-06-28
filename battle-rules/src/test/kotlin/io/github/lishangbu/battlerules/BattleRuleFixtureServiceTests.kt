package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleRuleFixtureRequest
import io.github.lishangbu.battlerules.dto.BattleRuleFixtureSourceRequest
import io.github.lishangbu.battlerules.dto.BattleRuleTestRunRequest
import io.github.lishangbu.battlerules.service.BattleRuleFixtureService
import io.github.lishangbu.battlerules.service.BattleRuleFixtureSourceService
import io.github.lishangbu.battlerules.service.BattleRuleTestRunService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import java.time.OffsetDateTime

@SpringBootTest(
	classes = [BattleRulesTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=8",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
/**
 * 验证公开对照 Fixture、公开来源和测试运行结果三张表各自独立维护。
 */
class BattleRuleFixtureServiceTests(
	@Autowired private val fixtureService: BattleRuleFixtureService,
	@Autowired private val sourceService: BattleRuleFixtureSourceService,
	@Autowired private val testRunService: BattleRuleTestRunService,
) {
	@Test
	fun `create update read list and delete battle rule fixture records`() {
		val fixture = fixtureService.create(
			BattleRuleFixtureRequest(
				code = "test-fixture-service",
				name = "Fixture 服务测试",
				category = "status",
				fixtureType = "formula",
				description = "验证 Fixture 主表 CRUD。",
				inputSummary = "固定输入摘要",
				expectedSummary = "固定期望摘要",
				enabled = true,
				sortOrder = 901,
			),
		)
		assertThat(fixture.category).isEqualTo("STATUS")
		assertThat(fixture.fixtureType).isEqualTo("FORMULA")
		assertThat(fixtureService.list(0, 20, query = "service", category = "status", enabled = true).rows.map { it.id })
			.contains(fixture.id)

		val source = sourceService.create(
			BattleRuleFixtureSourceRequest(
				fixtureId = fixture.id,
				sourceUrl = "https://example.com/battle-rule-fixture",
				sourceLabel = "公开来源",
				sourceNote = "验证来源维护。",
				sortOrder = 902,
			),
		)
		assertThat(sourceService.list(0, 20, fixtureId = fixture.id).rows.map { it.id }).contains(source.id)

		val run = testRunService.create(
			BattleRuleTestRunRequest(
				runCode = "test-fixture-service-run",
				fixtureId = fixture.id,
				runStatus = "passed",
				executor = "gradle",
				command = "./gradlew :battle-engine:test",
				engineCommit = "local-test",
				startedAt = OffsetDateTime.parse("2026-06-28T21:30:00+08:00"),
				finishedAt = OffsetDateTime.parse("2026-06-28T21:30:10+08:00"),
				durationMs = 10_000,
				assertionCount = 2,
				sortOrder = 903,
			),
		)
		assertThat(run.runStatus).isEqualTo("PASSED")
		assertThat(testRunService.list(0, 20, fixtureId = fixture.id, runStatus = "passed").rows.map { it.id })
			.contains(run.id)

		val updated = fixtureService.update(
			fixture.id,
			BattleRuleFixtureRequest(
				code = "test-fixture-service",
				name = "Fixture 服务测试已更新",
				category = "turn",
				fixtureType = "state_machine",
				inputSummary = "更新输入摘要",
				expectedSummary = "更新期望摘要",
				enabled = false,
				sortOrder = 904,
			),
		)
		assertThat(updated.category).isEqualTo("TURN")
		assertThat(updated.enabled).isFalse()

		testRunService.delete(run.id)
		sourceService.delete(source.id)
		fixtureService.delete(fixture.id)
		val missing = assertThrows<ApiException> {
			fixtureService.get(fixture.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `fixture source requires https url`() {
		val exception = assertThrows<ApiException> {
			sourceService.create(
				BattleRuleFixtureSourceRequest(
					fixtureId = 1,
					sourceUrl = "http://example.com/not-secure",
					sortOrder = 1,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("sourceUrl")
	}
}
