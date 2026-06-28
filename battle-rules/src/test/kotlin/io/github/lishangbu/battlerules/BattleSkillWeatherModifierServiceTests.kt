package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillChargeSkipWeatherRequest
import io.github.lishangbu.battlerules.dto.BattleSkillRuleRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierRequest
import io.github.lishangbu.battlerules.service.BattleSkillChargeSkipWeatherService
import io.github.lishangbu.battlerules.service.BattleSkillRuleService
import io.github.lishangbu.battlerules.service.BattleSkillWeatherAccuracyOverrideService
import io.github.lishangbu.battlerules.service.BattleSkillWeatherPowerModifierService
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
 * 验证技能天气命中覆盖和威力倍率子资源的独立维护能力。
 */
class BattleSkillWeatherModifierServiceTests(
	@Autowired private val accuracyService: BattleSkillWeatherAccuracyOverrideService,
	@Autowired private val powerService: BattleSkillWeatherPowerModifierService,
	@Autowired private val chargeSkipService: BattleSkillChargeSkipWeatherService,
	@Autowired private val skillRuleService: BattleSkillRuleService,
) {
	@Test
	fun `create update read list and delete weather accuracy override`() {
		val created = accuracyService.create(
			BattleSkillWeatherAccuracyOverrideRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				accuracyPercent = 50,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.weatherRuleId).isEqualTo(2)
		assertThat(created.accuracyPercent).isEqualTo(50)
		assertThat(accuracyService.get(created.id).accuracyPercent).isEqualTo(50)
		assertThat(accuracyService.list(0, 20, skillRuleId = 2, weatherRuleId = null).rows.map { it.id })
			.contains(created.id)

		val updated = accuracyService.update(
			created.id,
			BattleSkillWeatherAccuracyOverrideRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				accuracyPercent = null,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.accuracyPercent).isNull()
		assertThat(updated.enabled).isFalse()
		assertThat(updated.sortOrder).isEqualTo(902)

		accuracyService.delete(created.id)
		val missing = assertThrows<ApiException> {
			accuracyService.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid weather accuracy override`() {
		val duplicate = assertThrows<ApiException> {
			accuracyService.create(
				BattleSkillWeatherAccuracyOverrideRequest(
					skillRuleId = 11,
					weatherRuleId = 3,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("weatherRuleId")

		val clearWeather = assertThrows<ApiException> {
			accuracyService.create(
				BattleSkillWeatherAccuracyOverrideRequest(
					skillRuleId = 2,
					weatherRuleId = 1,
					sortOrder = 10,
				),
			)
		}
		assertThat(clearWeather.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(clearWeather.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(clearWeather.field).isEqualTo("weatherRuleId")

		val invalidAccuracy = assertThrows<ApiException> {
			accuracyService.create(
				BattleSkillWeatherAccuracyOverrideRequest(
					skillRuleId = 2,
					weatherRuleId = 3,
					accuracyPercent = 0,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidAccuracy.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidAccuracy.field).isEqualTo("accuracyPercent")
	}

	@Test
	fun `create update read list and delete weather power modifier`() {
		val created = powerService.create(
			BattleSkillWeatherPowerModifierRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				powerMultiplier = 1.5,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.weatherRuleId).isEqualTo(2)
		assertThat(created.powerMultiplier).isEqualTo(1.5)
		assertThat(powerService.get(created.id).powerMultiplier).isEqualTo(1.5)
		assertThat(powerService.list(0, 20, skillRuleId = 2, weatherRuleId = null).rows.map { it.id })
			.contains(created.id)

		val updated = powerService.update(
			created.id,
			BattleSkillWeatherPowerModifierRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				powerMultiplier = 0.5,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.powerMultiplier).isEqualTo(0.5)
		assertThat(updated.enabled).isFalse()
		assertThat(updated.sortOrder).isEqualTo(902)

		powerService.delete(created.id)
		val missing = assertThrows<ApiException> {
			powerService.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid weather power modifier`() {
		val duplicate = assertThrows<ApiException> {
			powerService.create(
				BattleSkillWeatherPowerModifierRequest(
					skillRuleId = 10,
					weatherRuleId = 3,
					powerMultiplier = 0.5,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("weatherRuleId")

		val clearWeather = assertThrows<ApiException> {
			powerService.create(
				BattleSkillWeatherPowerModifierRequest(
					skillRuleId = 2,
					weatherRuleId = 1,
					powerMultiplier = 0.5,
					sortOrder = 10,
				),
			)
		}
		assertThat(clearWeather.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(clearWeather.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(clearWeather.field).isEqualTo("weatherRuleId")

		val invalidMultiplier = assertThrows<ApiException> {
			powerService.create(
				BattleSkillWeatherPowerModifierRequest(
					skillRuleId = 2,
					weatherRuleId = 3,
					powerMultiplier = 0.0,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidMultiplier.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidMultiplier.field).isEqualTo("powerMultiplier")
	}

	@Test
	fun `create update read list and delete charge skip weather`() {
		val skillRule = skillRuleService.create(chargeSkillRuleRequest())
		try {
			val created = chargeSkipService.create(
				BattleSkillChargeSkipWeatherRequest(
					skillRuleId = skillRule.id,
					weatherRuleId = 2,
					enabled = true,
					sortOrder = 901,
				),
			)

			assertThat(created.skillRuleId).isEqualTo(skillRule.id)
			assertThat(created.weatherRuleId).isEqualTo(2)
			assertThat(chargeSkipService.get(created.id).weatherRuleId).isEqualTo(2)
			assertThat(chargeSkipService.list(0, 20, skillRuleId = skillRule.id, weatherRuleId = null).rows.map { it.id })
				.contains(created.id)

			val updated = chargeSkipService.update(
				created.id,
				BattleSkillChargeSkipWeatherRequest(
					skillRuleId = skillRule.id,
					weatherRuleId = 3,
					enabled = false,
					sortOrder = 902,
				),
			)
			assertThat(updated.weatherRuleId).isEqualTo(3)
			assertThat(updated.enabled).isFalse()
			assertThat(updated.sortOrder).isEqualTo(902)

			chargeSkipService.delete(created.id)
			val missing = assertThrows<ApiException> {
				chargeSkipService.get(created.id)
			}
			assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
		} finally {
			skillRuleService.delete(skillRule.id)
		}
	}

	@Test
	fun `rejects invalid charge skip weather`() {
		val duplicate = assertThrows<ApiException> {
			chargeSkipService.create(
				BattleSkillChargeSkipWeatherRequest(
					skillRuleId = 10,
					weatherRuleId = 2,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("weatherRuleId")

		val clearWeather = assertThrows<ApiException> {
			chargeSkipService.create(
				BattleSkillChargeSkipWeatherRequest(
					skillRuleId = 10,
					weatherRuleId = 1,
					sortOrder = 10,
				),
			)
		}
		assertThat(clearWeather.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(clearWeather.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(clearWeather.field).isEqualTo("weatherRuleId")

		val nonChargeSkill = assertThrows<ApiException> {
			chargeSkipService.create(
				BattleSkillChargeSkipWeatherRequest(
					skillRuleId = 1,
					weatherRuleId = 2,
					sortOrder = 10,
				),
			)
		}
		assertThat(nonChargeSkill.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(nonChargeSkill.field).isEqualTo("skillRuleId")
	}

	private fun chargeSkillRuleRequest(): BattleSkillRuleRequest =
		BattleSkillRuleRequest(
			skillId = 10,
			effectPolicy = "test-charge-skip",
			targetPolicy = "selected-target",
			hitPolicy = "standard-hit",
			damagePolicy = "standard-damage",
			minHits = 1,
			maxHits = 1,
			criticalHitStage = 0,
			affectedByProtect = true,
			chargesBeforeUse = true,
			lockMoveTurnsMin = 1,
			lockMoveTurnsMax = 1,
			enabled = true,
			sortOrder = 903,
		)
}
