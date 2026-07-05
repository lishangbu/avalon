package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillTerrainElementOverrideRequest
import io.github.lishangbu.battlerules.dto.BattleSkillTerrainPowerModifierRequest
import io.github.lishangbu.battlerules.service.BattleSkillTerrainElementOverrideService
import io.github.lishangbu.battlerules.service.BattleSkillTerrainPowerModifierService
import io.github.lishangbu.common.web.ApiErrorCode
import io.github.lishangbu.common.web.ApiException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.babyfish.jimmer.sql.kt.KSqlClient

@BattleRulesIntegrationTest
/**
 * 验证技能场地威力倍率和场地属性覆盖子资源的独立维护能力。
 *
 * 场地脉冲这类技能同时需要“场地改变属性”和“使用者接地时威力翻倍”两种事实。测试刻意分别调用两套服务，
 * 确认维护端可以像普通表格一样逐项增删改查，而不是依赖一个不可拆分的复合策略字符串。
 */
class BattleSkillTerrainModifierServiceTests(
	@Autowired private val powerService: BattleSkillTerrainPowerModifierService,
	@Autowired private val elementService: BattleSkillTerrainElementOverrideService,
	@Autowired private val sqlClient: KSqlClient,
) {
	@Test
	fun `create update read list and delete terrain power modifier`() {
		val created = powerService.create(
			BattleSkillTerrainPowerModifierRequest(
				skillRuleId = 2,
				terrainRuleId = 1,
				powerMultiplier = 2.0,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.terrainRuleId).isEqualTo(1)
		assertThat(created.powerMultiplier).isEqualTo(2.0)
		assertThat(powerService.get(created.id).powerMultiplier).isEqualTo(2.0)
		assertThat(powerService.list(0, 20, skillRuleId = 2, terrainRuleId = null).rows.map { it.id })
			.contains(created.id)

		val updated = powerService.update(
			created.id,
			BattleSkillTerrainPowerModifierRequest(
				skillRuleId = 2,
				terrainRuleId = 2,
				powerMultiplier = 1.5,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.terrainRuleId).isEqualTo(2)
		assertThat(updated.powerMultiplier).isEqualTo(1.5)
		assertThat(updated.enabled).isFalse()
		assertThat(updated.sortOrder).isEqualTo(902)

		powerService.delete(created.id)
		val missing = assertThrows<ApiException> {
			powerService.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid terrain power modifier`() {
		val duplicate = assertThrows<ApiException> {
			powerService.create(
				BattleSkillTerrainPowerModifierRequest(
					skillRuleId = terrainPulseRuleId(),
					terrainRuleId = 1,
					powerMultiplier = 2.0,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("terrainRuleId")

		val invalidMultiplier = assertThrows<ApiException> {
			powerService.create(
				BattleSkillTerrainPowerModifierRequest(
					skillRuleId = 2,
					terrainRuleId = 1,
					powerMultiplier = 0.0,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidMultiplier.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidMultiplier.field).isEqualTo("powerMultiplier")

		withUnsupportedTerrainRule { terrainRuleId ->
			val unsupportedTerrain = assertThrows<ApiException> {
				powerService.create(
					BattleSkillTerrainPowerModifierRequest(
						skillRuleId = 2,
						terrainRuleId = terrainRuleId,
						powerMultiplier = 2.0,
						sortOrder = 10,
					),
				)
			}
			assertThat(unsupportedTerrain.status).isEqualTo(HttpStatus.BAD_REQUEST)
			assertThat(unsupportedTerrain.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
			assertThat(unsupportedTerrain.field).isEqualTo("terrainRuleId")
		}
	}

	@Test
	fun `create update read list and delete terrain element override`() {
		val created = elementService.create(
			BattleSkillTerrainElementOverrideRequest(
				skillRuleId = 2,
				terrainRuleId = 1,
				targetElementId = 13,
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillRuleId).isEqualTo(2)
		assertThat(created.terrainRuleId).isEqualTo(1)
		assertThat(created.targetElementId).isEqualTo(13)
		assertThat(elementService.get(created.id).targetElementId).isEqualTo(13)
		assertThat(elementService.list(0, 20, skillRuleId = 2, terrainRuleId = null, targetElementId = null).rows.map { it.id })
			.contains(created.id)

		val updated = elementService.update(
			created.id,
			BattleSkillTerrainElementOverrideRequest(
				skillRuleId = 2,
				terrainRuleId = 2,
				targetElementId = 12,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.terrainRuleId).isEqualTo(2)
		assertThat(updated.targetElementId).isEqualTo(12)
		assertThat(updated.enabled).isFalse()
		assertThat(updated.sortOrder).isEqualTo(902)

		elementService.delete(created.id)
		val missing = assertThrows<ApiException> {
			elementService.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects invalid terrain element override`() {
		val duplicate = assertThrows<ApiException> {
			elementService.create(
				BattleSkillTerrainElementOverrideRequest(
					skillRuleId = terrainPulseRuleId(),
					terrainRuleId = 1,
					targetElementId = 13,
					sortOrder = 10,
				),
			)
		}
		assertThat(duplicate.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(duplicate.field).isEqualTo("terrainRuleId")

		val invalidElement = assertThrows<ApiException> {
			elementService.create(
				BattleSkillTerrainElementOverrideRequest(
					skillRuleId = 2,
					terrainRuleId = 1,
					targetElementId = 999_999,
					sortOrder = 10,
				),
			)
		}
		assertThat(invalidElement.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(invalidElement.field).isEqualTo("targetElementId")

		withUnsupportedTerrainRule { terrainRuleId ->
			val unsupportedTerrain = assertThrows<ApiException> {
				elementService.create(
					BattleSkillTerrainElementOverrideRequest(
						skillRuleId = 2,
						terrainRuleId = terrainRuleId,
						targetElementId = 13,
						sortOrder = 10,
					),
				)
			}
			assertThat(unsupportedTerrain.status).isEqualTo(HttpStatus.BAD_REQUEST)
			assertThat(unsupportedTerrain.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
			assertThat(unsupportedTerrain.field).isEqualTo("terrainRuleId")
		}
	}

	private fun terrainPulseRuleId(): Long =
		sqlClient.querySingleTestSql(
			"""
			select r.id
			from battle_skill_rule r
			join game_skill s on s.id = r.skill_id
			where s.code = 'terrain-pulse'
				and r.enabled = true
			""".trimIndent(),
		) { resultSet -> resultSet.getLong("id") }

	private fun withUnsupportedTerrainRule(block: (Long) -> Unit) {
		deleteUnsupportedTerrainRule()
		sqlClient.executeTestSql(
			"""
			insert into battle_terrain_rule (
				id,
				code,
				name,
				effect_policy,
				default_duration_turns,
				description,
				enabled,
				sort_order
			) values (
				?,
				'unsupported-test-terrain',
				'测试场地',
				'terrain-unsupported-test',
				5,
				'仅用于验证服务层拒绝引擎尚不支持的场地 code。',
				true,
				9999
			)
			""".trimIndent(),
			UNSUPPORTED_TERRAIN_RULE_ID,
		)
		try {
			block(UNSUPPORTED_TERRAIN_RULE_ID)
		} finally {
			deleteUnsupportedTerrainRule()
		}
	}

	private fun deleteUnsupportedTerrainRule() {
		sqlClient.executeTestSql("delete from battle_terrain_rule where id = ?", UNSUPPORTED_TERRAIN_RULE_ID)
	}

	private companion object {
		private const val UNSUPPORTED_TERRAIN_RULE_ID = 9_920_002L
	}
}
