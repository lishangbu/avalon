package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleItemRuleRequest
import io.github.lishangbu.battlerules.service.BattleItemRuleService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@BattleRulesIntegrationTest
/**
 * 验证战斗道具规则的独立 CRUD、基础道具引用校验和触发策略唯一性。
 */
class BattleItemRuleServiceTests(
	@Autowired private val service: BattleItemRuleService,
) {
	@Test
	fun `create update read list and delete item rule`() {
		val created = service.create(
			BattleItemRuleRequest(
				itemId = 1,
				triggerTiming = "held_end_turn",
				effectPolicy = "test-item-end-turn",
				consumable = false,
				triggerOrder = 30,
				description = "测试道具规则。",
				enabled = true,
				sortOrder = 941,
			),
		)

		assertThat(created.triggerTiming).isEqualTo("HELD_END_TURN")
		assertThat(service.list(0, 20, itemId = 1, triggerTiming = "held_end_turn", query = "item").rows.map { it.id })
			.contains(created.id)

		val updated = service.update(
			created.id,
			BattleItemRuleRequest(
				itemId = 1,
				triggerTiming = "before_damage",
				effectPolicy = "test-item-end-turn",
				consumable = true,
				triggerOrder = 31,
				description = null,
				enabled = false,
				sortOrder = 942,
			),
		)
		assertThat(updated.triggerTiming).isEqualTo("BEFORE_DAMAGE")
		assertThat(updated.consumable).isTrue()
		assertThat(updated.enabled).isFalse()

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects missing referenced item`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleItemRuleRequest(
					itemId = 999999,
					triggerTiming = "HELD_END_TURN",
					effectPolicy = "test-missing-item",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("itemId")
	}

	@Test
	fun `rejects duplicated item rule`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleItemRuleRequest(
					itemId = 211,
					triggerTiming = "HELD_END_TURN",
					effectPolicy = "leftovers-heal",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("effectPolicy")
	}
}
