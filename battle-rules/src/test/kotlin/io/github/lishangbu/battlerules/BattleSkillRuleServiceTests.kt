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
				makesContact = true,
				affectedByProtect = true,
				description = "测试技能规则。",
				enabled = true,
				sortOrder = 901,
			),
		)

		assertThat(created.skillId).isEqualTo(10)
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
				makesContact = false,
				affectedByProtect = false,
				soundBased = true,
				description = null,
				enabled = false,
				sortOrder = 902,
			),
		)
		assertThat(updated.makesContact).isFalse()
		assertThat(updated.affectedByProtect).isFalse()
		assertThat(updated.soundBased).isTrue()
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
}
