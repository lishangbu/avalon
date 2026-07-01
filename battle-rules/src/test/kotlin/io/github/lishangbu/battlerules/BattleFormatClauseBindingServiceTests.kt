package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleFormatClauseBindingRequest
import io.github.lishangbu.battlerules.service.BattleFormatClauseBindingService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@BattleRulesIntegrationTest
/**
 * 验证战斗赛制条款绑定服务的外键校验和重复绑定保护。
 */
class BattleFormatClauseBindingServiceTests(
	@Autowired private val service: BattleFormatClauseBindingService,
) {
	@Test
	fun `create update list and delete clause binding`() {
		val created = service.create(
			BattleFormatClauseBindingRequest(
				formatId = 1,
				clauseId = 1,
				required = false,
				sortOrder = 100,
			),
		)

		assertThat(created.formatId).isEqualTo(1)
		assertThat(created.clauseId).isEqualTo(1)
		assertThat(service.list(0, 20, formatId = 1, clauseId = null).rows.map { it.id }).contains(created.id)

		val updated = service.update(
			created.id,
			BattleFormatClauseBindingRequest(
				formatId = 1,
				clauseId = 1,
				required = true,
				sortOrder = 101,
			),
		)
		assertThat(updated.required).isTrue()
		assertThat(updated.sortOrder).isEqualTo(101)

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects missing referenced format`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleFormatClauseBindingRequest(
					formatId = 999999,
					clauseId = 1,
					required = true,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("formatId")
	}

	@Test
	fun `rejects duplicated clause binding`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleFormatClauseBindingRequest(
					formatId = 3,
					clauseId = 1,
					required = true,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("clauseId")
	}
}
