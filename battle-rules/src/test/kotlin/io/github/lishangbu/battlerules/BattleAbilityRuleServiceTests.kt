package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleAbilityRuleRequest
import io.github.lishangbu.battlerules.service.BattleAbilityRuleService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@BattleRulesIntegrationTest
/**
 * 验证战斗特性规则的独立 CRUD、基础特性引用校验和触发策略唯一性。
 */
class BattleAbilityRuleServiceTests(
	@Autowired private val service: BattleAbilityRuleService,
) {
	@Test
	fun `create update read list and delete ability rule`() {
		val created = service.create(
			BattleAbilityRuleRequest(
				abilityId = 10,
				triggerTiming = "switch_in",
				effectPolicy = "test-ability-switch-in",
				triggerOrder = 30,
				description = "测试特性规则。",
				enabled = true,
				sortOrder = 931,
			),
		)

		assertThat(created.triggerTiming).isEqualTo("SWITCH_IN")
		assertThat(service.list(0, 20, abilityId = 10, triggerTiming = "switch_in", query = "ability").rows.map { it.id })
			.contains(created.id)

		val updated = service.update(
			created.id,
			BattleAbilityRuleRequest(
				abilityId = 10,
				triggerTiming = "before_damage",
				effectPolicy = "test-ability-switch-in",
				triggerOrder = 31,
				description = null,
				enabled = false,
				sortOrder = 932,
			),
		)
		assertThat(updated.triggerTiming).isEqualTo("BEFORE_DAMAGE")
		assertThat(updated.enabled).isFalse()

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects missing referenced ability`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleAbilityRuleRequest(
					abilityId = 999999,
					triggerTiming = "SWITCH_IN",
					effectPolicy = "test-missing-ability",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("abilityId")
	}

	@Test
	fun `rejects duplicated ability rule`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleAbilityRuleRequest(
					abilityId = 65,
					triggerTiming = "BEFORE_DAMAGE",
					effectPolicy = "low-hp-grass-boost",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("effectPolicy")
	}
}
