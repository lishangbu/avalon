package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleFormatRequest
import io.github.lishangbu.battlerules.service.BattleFormatService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

@BattleRulesIntegrationTest
/**
 * 验证战斗赛制服务的独立 CRUD 和唯一性校验。
 */
class BattleFormatServiceTests(
	@Autowired private val service: BattleFormatService,
) {
	@Test
	fun `create update read list and delete battle format`() {
		val code = "codex-format-${System.nanoTime()}".take(48)
		val created = service.create(
			BattleFormatRequest(
				code = code,
				name = "测试赛制",
				description = "用于验证战斗规则模块独立 CRUD。",
				battleMode = "single",
				playerCount = 2,
				teamSize = 6,
				activeParticipantCount = 1,
				defaultLevel = 50,
				allowCustomRules = true,
				enabled = true,
				sortOrder = 999,
			),
		)

		assertThat(created.code).isEqualTo(code)
		assertThat(created.battleMode).isEqualTo("SINGLE")
		assertThat(service.list(0, 20, "测试赛制").rows.map { it.id }).contains(created.id)

		val updated = service.update(
			created.id,
			BattleFormatRequest(
				code = code,
				name = "测试赛制改",
				description = null,
				battleMode = "double",
				playerCount = 2,
				teamSize = 4,
				activeParticipantCount = 2,
				defaultLevel = null,
				allowCustomRules = false,
				enabled = false,
				sortOrder = 998,
			),
		)
		assertThat(updated.name).isEqualTo("测试赛制改")
		assertThat(updated.battleMode).isEqualTo("DOUBLE")
		assertThat(updated.description).isNull()

		service.delete(created.id)

		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(missing.code).isEqualTo(ApiErrorCode.RESOURCE_NOT_FOUND)
	}

	@Test
	fun `rejects duplicated battle format code`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleFormatRequest(
					code = "standard-single",
					name = "重复赛制",
					battleMode = "SINGLE",
					playerCount = 2,
					teamSize = 6,
					activeParticipantCount = 1,
					allowCustomRules = true,
					enabled = true,
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("code")
	}
}
