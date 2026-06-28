package io.github.lishangbu.battlerules

import io.github.lishangbu.battlerules.dto.BattleSkillRuleRequest
import io.github.lishangbu.battlerules.service.BattleSkillRuleService
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
 * 验证技能主规则的独立 CRUD、基础技能引用校验和唯一性保护。
 */
class BattleSkillRuleServiceTests(
	@Autowired private val service: BattleSkillRuleService,
) {
	@Test
	fun `create update read list and delete skill rule`() {
		val created = service.create(
			BattleSkillRuleRequest(
				skillId = 10,
				effectPolicy = "test-standard-damage",
				targetPolicy = "selected-target",
				hitPolicy = "standard-hit",
				damagePolicy = "standard-damage",
				minHits = 2,
				maxHits = 5,
				criticalHitStage = 1,
				makesContact = true,
				affectedByProtect = true,
				weakenedByGrassyTerrain = true,
				rechargesAfterUse = true,
				lockMoveTurnsMin = 2,
				lockMoveTurnsMax = 3,
				confusesUserAfterLock = true,
				description = "测试技能规则。",
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillId).isEqualTo(10)
		assertThat(created.minHits).isEqualTo(2)
		assertThat(created.maxHits).isEqualTo(5)
		assertThat(created.criticalHitStage).isEqualTo(1)
		assertThat(created.weakenedByGrassyTerrain).isTrue()
		assertThat(created.rechargesAfterUse).isTrue()
		assertThat(created.lockMoveTurnsMin).isEqualTo(2)
		assertThat(created.lockMoveTurnsMax).isEqualTo(3)
		assertThat(created.confusesUserAfterLock).isTrue()
		assertThat(service.get(created.id).effectPolicy).isEqualTo("test-standard-damage")
		assertThat(service.list(0, 20, skillId = 10, query = "standard").rows.map { it.id }).contains(created.id)

		val updated = service.update(
			created.id,
			BattleSkillRuleRequest(
				skillId = 10,
				effectPolicy = "test-standard-damage",
				targetPolicy = "selected-target",
				hitPolicy = "standard-hit",
				damagePolicy = "standard-damage",
				minHits = 1,
				maxHits = 1,
				criticalHitStage = 2,
				makesContact = false,
				affectedByProtect = false,
				thawsUserBeforeMove = true,
				chargesBeforeUse = true,
				rechargesAfterUse = false,
				soundBased = true,
				lockMoveTurnsMin = 1,
				lockMoveTurnsMax = 1,
				confusesUserAfterLock = false,
				description = null,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.makesContact).isFalse()
		assertThat(updated.affectedByProtect).isFalse()
		assertThat(updated.soundBased).isTrue()
		assertThat(updated.criticalHitStage).isEqualTo(2)
		assertThat(updated.thawsUserBeforeMove).isTrue()
		assertThat(updated.chargesBeforeUse).isTrue()
		assertThat(updated.rechargesAfterUse).isFalse()
		assertThat(updated.confusesUserAfterLock).isFalse()
		assertThat(updated.description).isNull()

		service.delete(created.id)
		val missing = assertThrows<ApiException> {
			service.get(created.id)
		}
		assertThat(missing.status).isEqualTo(HttpStatus.NOT_FOUND)
	}

	@Test
	fun `rejects missing referenced skill`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 999999,
					effectPolicy = "test-missing-skill",
					targetPolicy = "selected-target",
					hitPolicy = "standard-hit",
					damagePolicy = "standard-damage",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(exception.code).isEqualTo(ApiErrorCode.VALIDATION_INVALID)
		assertThat(exception.field).isEqualTo("skillId")
	}

	@Test
	fun `rejects duplicated skill rule`() {
		val exception = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 1,
					effectPolicy = "test-duplicate-skill",
					targetPolicy = "selected-target",
					hitPolicy = "standard-hit",
					damagePolicy = "standard-damage",
					sortOrder = 10,
				),
			)
		}

		assertThat(exception.status).isEqualTo(HttpStatus.CONFLICT)
		assertThat(exception.code).isEqualTo(ApiErrorCode.RESOURCE_CONFLICT)
		assertThat(exception.field).isEqualTo("skillId")
	}

	@Test
	fun `rejects runtime fields that break engine invariants`() {
		val reversedHits = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 11,
					effectPolicy = "test-reversed-hit",
					targetPolicy = "selected-target",
					hitPolicy = "multi-hit",
					damagePolicy = "standard-damage",
					minHits = 5,
					maxHits = 2,
					sortOrder = 10,
				),
			)
		}
		assertThat(reversedHits.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(reversedHits.field).isEqualTo("maxHits")

		val statusMultiHit = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 14,
					effectPolicy = "test-status-multi-hit",
					targetPolicy = "self",
					hitPolicy = "multi-hit",
					damagePolicy = "no-damage",
					minHits = 2,
					maxHits = 2,
					sortOrder = 10,
				),
			)
		}
		assertThat(statusMultiHit.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(statusMultiHit.field).isEqualTo("maxHits")

		val nonStatusProtect = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 11,
					effectPolicy = "test-invalid-protect",
					targetPolicy = "selected-target",
					hitPolicy = "standard-hit",
					damagePolicy = "standard-damage",
					protectsUser = true,
					sortOrder = 10,
				),
			)
		}
		assertThat(nonStatusProtect.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(nonStatusProtect.field).isEqualTo("protectsUser")

		val lockConfusion = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 11,
					effectPolicy = "test-invalid-lock-confusion",
					targetPolicy = "selected-target",
					hitPolicy = "standard-hit",
					damagePolicy = "standard-damage",
					confusesUserAfterLock = true,
					sortOrder = 10,
				),
			)
		}
		assertThat(lockConfusion.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(lockConfusion.field).isEqualTo("confusesUserAfterLock")

		val statusRecharge = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 14,
					effectPolicy = "test-invalid-status-recharge",
					targetPolicy = "self",
					hitPolicy = "standard-hit",
					damagePolicy = "no-damage",
					rechargesAfterUse = true,
					sortOrder = 10,
				),
			)
		}
		assertThat(statusRecharge.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(statusRecharge.field).isEqualTo("rechargesAfterUse")

		val statusCharge = assertThrows<ApiException> {
			service.create(
				BattleSkillRuleRequest(
					skillId = 14,
					effectPolicy = "test-invalid-status-charge",
					targetPolicy = "self",
					hitPolicy = "standard-hit",
					damagePolicy = "no-damage",
					chargesBeforeUse = true,
					sortOrder = 10,
				),
			)
		}
		assertThat(statusCharge.status).isEqualTo(HttpStatus.BAD_REQUEST)
		assertThat(statusCharge.field).isEqualTo("chargesBeforeUse")
	}
}
