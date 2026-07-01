package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillStatusEffectRequest
import io.github.lishangbu.battlerules.service.BattleSkillStatusEffectService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@BattleRulesIntegrationTest
/**
 * 验证技能状态附加效果的独立维护、大小写归一化和重复效果保护。
 */
class BattleSkillStatusEffectServiceTests(
	@Autowired private val service: BattleSkillStatusEffectService,
) {
	@Test
	fun `create update read list and delete skill status effect`() {
		val created = service.create(
			BattleSkillStatusEffectRequest(
				skillRuleId = 1,
				statusRuleId = 2,
				targetScope = "target",
				effectTiming = "after_hit",
				chancePercent = 30,
				enabled = true,
				sortOrder = 911,
			),
		)

		assertThat(created.targetScope).isEqualTo("TARGET")
		assertThat(created.effectTiming).isEqualTo("AFTER_HIT")
		assertThat(service.list(0, 20, skillRuleId = 1, statusRuleId = null).rows.map { it.id }).contains(created.id)

		val updated = service.update(
			created.id,
			BattleSkillStatusEffectRequest(
				skillRuleId = 1,
				statusRuleId = 2,
				targetScope = "target",
				effectTiming = "after_hit",
				chancePercent = 40,
				enabled = false,
				sortOrder = 912,
			),
		)
		assertThat(updated.chancePercent).isEqualTo(40)
		assertThat(updated.enabled).isFalse()

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects missing referenced skill rule`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillStatusEffectRequest(
					skillRuleId = 999999,
					statusRuleId = 1,
					targetScope = "TARGET",
					effectTiming = "AFTER_HIT",
					chancePercent = 10,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("skillRuleId")
	}

	@Test
	fun `rejects duplicated skill status effect`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillStatusEffectRequest(
					skillRuleId = 4,
					statusRuleId = 1,
					targetScope = "TARGET",
					effectTiming = "AFTER_HIT",
					chancePercent = 20,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("statusRuleId")
	}
}
