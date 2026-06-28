package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillStatStageEffectRequest
import io.github.lishangbu.battlerules.service.BattleSkillStatStageEffectService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(
	classes = [BattleRulesTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=4",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
/**
 * 验证技能能力阶级效果的独立维护、能力项引用校验和阶级变化值约束。
 */
class BattleSkillStatStageEffectServiceTests(
	@Autowired private val service: BattleSkillStatStageEffectService,
) {
	@Test
	fun `create update read list and delete skill stat stage effect`() {
		val created = service.create(
			BattleSkillStatStageEffectRequest(
				skillRuleId = 1,
				statId = 3,
				targetScope = "user",
				effectTiming = "after_hit",
				stageDelta = 1,
				chancePercent = 100,
				enabled = true,
				sortOrder = 921,
			),
		)

		assertThat(created.targetScope).isEqualTo("USER")
		assertThat(created.effectTiming).isEqualTo("AFTER_HIT")
		assertThat(service.list(0, 20, skillRuleId = 1, statId = 3).rows.map { it.id }).contains(created.id)

		val updated = service.update(
			created.id,
			BattleSkillStatStageEffectRequest(
				skillRuleId = 1,
				statId = 3,
				targetScope = "user",
				effectTiming = "after_hit",
				stageDelta = 2,
				chancePercent = 80,
				enabled = false,
				sortOrder = 922,
			),
		)
		assertThat(updated.stageDelta).isEqualTo(2)
		assertThat(updated.chancePercent).isEqualTo(80)

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects zero stage delta`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillStatStageEffectRequest(
					skillRuleId = 1,
					statId = 3,
					targetScope = "USER",
					effectTiming = "AFTER_HIT",
					stageDelta = 0,
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("stageDelta")
	}

	@Test
	fun `rejects missing referenced stat`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillStatStageEffectRequest(
					skillRuleId = 1,
					statId = 999999,
					targetScope = "USER",
					effectTiming = "AFTER_HIT",
					stageDelta = 1,
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("statId")
	}
}
