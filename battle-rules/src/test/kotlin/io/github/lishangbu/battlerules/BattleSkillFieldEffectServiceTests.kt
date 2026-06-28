package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillFieldEffectRequest
import io.github.lishangbu.battlerules.service.BattleSkillFieldEffectService
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
		"cosid.machine.distributor.manual.machine-id=6",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
/**
 * 验证技能场上效果子资源的独立维护能力。
 */
class BattleSkillFieldEffectServiceTests(
	@Autowired private val service: BattleSkillFieldEffectService,
) {
	@Test
	fun `create update read list and delete skill field effect`() {
		val created = service.create(
			BattleSkillFieldEffectRequest(
				skillRuleId = 2,
				fieldRuleId = 4,
				targetSide = "USER_SIDE",
				effectTiming = "AFTER_HIT",
				requiredWeatherRuleId = null,
				chancePercent = 100,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.fieldRuleId).isEqualTo(4)
		assertThat(created.targetSide).isEqualTo("USER_SIDE")
		assertThat(created.requiredWeatherRuleId).isNull()
		assertThat(service.get(created.id).chancePercent).isEqualTo(100)
		assertThat(service.list(0, 20, skillRuleId = 2, fieldRuleId = null).rows.map { it.id })
			.contains(created.id)

		val updated = service.update(
			created.id,
			BattleSkillFieldEffectRequest(
				skillRuleId = 2,
				fieldRuleId = 4,
				targetSide = "TARGET_SIDE",
				effectTiming = "AFTER_HIT",
				requiredWeatherRuleId = 5,
				chancePercent = 50,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.targetSide).isEqualTo("TARGET_SIDE")
		assertThat(updated.requiredWeatherRuleId).isEqualTo(5)
		assertThat(updated.chancePercent).isEqualTo(50)
		assertThat(updated.enabled).isFalse()

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid skill field effect`() {
		val duplicate = assertThrows<ApiException> {
			service.create(
				BattleSkillFieldEffectRequest(
					skillRuleId = 14,
					fieldRuleId = 1,
					targetSide = "USER_SIDE",
					effectTiming = "AFTER_HIT",
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("fieldRuleId")

		val fieldScope = assertThrows<ApiException> {
			service.create(
				BattleSkillFieldEffectRequest(
					skillRuleId = 2,
					fieldRuleId = 5,
					targetSide = "USER_SIDE",
					effectTiming = "AFTER_HIT",
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}
		assertThat(fieldScope.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(fieldScope.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(fieldScope.field).isEqualTo("fieldRuleId")

		val clearWeather = assertThrows<ApiException> {
			service.create(
				BattleSkillFieldEffectRequest(
					skillRuleId = 2,
					fieldRuleId = 4,
					targetSide = "USER_SIDE",
					effectTiming = "AFTER_HIT",
					requiredWeatherRuleId = 1,
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}
		assertThat(clearWeather.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(clearWeather.field).isEqualTo("requiredWeatherRuleId")

		val invalidTargetSide = assertThrows<ApiException> {
			service.create(
				BattleSkillFieldEffectRequest(
					skillRuleId = 2,
					fieldRuleId = 4,
					targetSide = "BAD_SIDE",
					effectTiming = "AFTER_HIT",
					chancePercent = 100,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidTargetSide.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidTargetSide.field).isEqualTo("targetSide")
	}
}
