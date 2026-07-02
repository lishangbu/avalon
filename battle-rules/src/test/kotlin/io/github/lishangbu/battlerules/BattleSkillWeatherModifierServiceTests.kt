package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillChargeSkipWeatherRequest
import io.github.lishangbu.battlerules.dto.BattleSkillRuleRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherAccuracyOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillWeatherPowerModifierRequest
import io.github.lishangbu.battlerules.service.BattleSkillChargeSkipWeatherService
import io.github.lishangbu.battlerules.service.BattleSkillRuleService
import io.github.lishangbu.battlerules.service.BattleSkillWeatherAccuracyOverrideService
import io.github.lishangbu.battlerules.service.BattleSkillWeatherElementOverrideService
import io.github.lishangbu.battlerules.service.BattleSkillWeatherPowerModifierService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.jdbc.core.JdbcTemplate

@BattleRulesIntegrationTest
/**
 * 验证技能天气命中覆盖和威力倍率子资源的独立维护能力。
 */
class BattleSkillWeatherModifierServiceTests(
	@Autowired private val accuracyService: BattleSkillWeatherAccuracyOverrideService,
	@Autowired private val powerService: BattleSkillWeatherPowerModifierService,
	@Autowired private val elementService: BattleSkillWeatherElementOverrideService,
	@Autowired private val chargeSkipService: BattleSkillChargeSkipWeatherService,
	@Autowired private val skillRuleService: BattleSkillRuleService,
	@Autowired private val jdbcTemplate: JdbcTemplate,
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
		withTemporarySkill { skillId ->
			val skillRule = skillRuleService.create(chargeSkillRuleRequest(skillId))
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

			skillRuleService.delete(skillRule.id)
		}
	}

	@Test
	fun `create update read list and delete weather element override`() {
		val created = elementService.create(
			BattleSkillWeatherElementOverrideRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				targetElementId = 10,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.weatherRuleId).isEqualTo(2)
		assertThat(created.targetElementId).isEqualTo(10)
		assertThat(elementService.get(created.id).targetElementId).isEqualTo(10)
		assertThat(elementService.list(0, 20, skillRuleId = 2, weatherRuleId = null, targetElementId = null).rows.map { it.id })
			.contains(created.id)

		val updated = elementService.update(
			created.id,
			BattleSkillWeatherElementOverrideRequest(
				skillRuleId = 2,
				weatherRuleId = 2,
				targetElementId = 11,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.targetElementId).isEqualTo(11)
		assertThat(updated.enabled).isFalse()
		assertThat(updated.sortOrder).isEqualTo(902)

		elementService.delete(created.id)
		val missing = assertThrows<ApiException> {
			elementService.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid weather element override`() {
		val duplicate = assertThrows<ApiException> {
			elementService.create(
				BattleSkillWeatherElementOverrideRequest(
					skillRuleId = 13,
					weatherRuleId = 3,
					targetElementId = 11,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("weatherRuleId")

		val clearWeather = assertThrows<ApiException> {
			elementService.create(
				BattleSkillWeatherElementOverrideRequest(
					skillRuleId = 2,
					weatherRuleId = 1,
					targetElementId = 10,
					sortOrder = 10,
				),
			)
		}
		assertThat(clearWeather.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(clearWeather.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(clearWeather.field).isEqualTo("weatherRuleId")

		val invalidElement = assertThrows<ApiException> {
			elementService.create(
				BattleSkillWeatherElementOverrideRequest(
					skillRuleId = 2,
					weatherRuleId = 3,
					targetElementId = 999_999,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidElement.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidElement.field).isEqualTo("targetElementId")
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

	private fun chargeSkillRuleRequest(skillId: Long): BattleSkillRuleRequest =
		BattleSkillRuleRequest(
			skillId = skillId,
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

	private fun withTemporarySkill(block: (Long) -> Unit) {
		deleteTemporarySkill()
		jdbcTemplate.update(
			"""
			insert into game_skill (
				id,
				code,
				name,
				element_id,
				damage_class_id,
				accuracy,
				power,
				pp,
				priority,
				effect_chance,
				enabled
			) values (
				?,
				'skill-weather-modifier-test',
				'技能天气修正测试',
				1,
				2,
				100,
				40,
				5,
				0,
				null,
				true
			)
			""".trimIndent(),
			TEMP_SKILL_ID,
		)
		try {
			block(TEMP_SKILL_ID)
		} finally {
			deleteTemporarySkill()
		}
	}

	private fun deleteTemporarySkill() {
		jdbcTemplate.update("delete from battle_skill_rule where skill_id = ?", TEMP_SKILL_ID)
		jdbcTemplate.update("delete from game_skill where id = ?", TEMP_SKILL_ID)
	}

	private companion object {
		private const val TEMP_SKILL_ID = 9_920_001L
	}
}
