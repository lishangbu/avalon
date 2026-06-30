package io.github.lishangbu.battlerules

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.SecurityManagementApiPostgresTestContainer
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.assertj.core.api.Assertions.assertThat
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [SecurityManagementApiPostgresTestContainer::class])
/**
 * 验证战斗规则覆盖率接口和服务端菜单返回真实运行态数据。
 */
class BattleRuleCoverageApiTests(
	@Autowired private val userRepository: SecurityUserRepository,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUpMockMvc() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(webApplicationContext)
			.apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
			.build()
	}

	@Test
	fun `battle rules admin can load coverage report and server menus`() {
		val token = issueBattleRulesToken("coverage-api-manager")

		val coverageResponse = mockMvc.perform(
			get("/api/battle-rules/coverage")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		assertThat(JsonPath.read<Int>(coverageResponse, "$.summary.totalCount")).isEqualTo(412)
		assertThat(JsonPath.read<Int>(coverageResponse, "$.targetSummary.coverageItemCount")).isEqualTo(412)
		assertThat(JsonPath.read<Boolean>(coverageResponse, "$.fixtureSummary.runtimeAvailable")).isTrue()
		assertThat(JsonPath.read<Int>(coverageResponse, "$.fixtureSummary.withoutRunCount")).isZero()
		assertThat(JsonPath.read<List<String>>(coverageResponse, "$.items[*].code"))
			.contains(GOLDEN_REPLAY_FIXTURE_CODE)
		assertThat(
			JsonPath.read<List<String>>(
				coverageResponse,
				"$.items[?(@.code == '$GOLDEN_REPLAY_FIXTURE_CODE')].fixtures[0].latestRunStatus",
			),
		).containsExactly("PASSED")

		val sessionResponse = mockMvc.perform(
			get("/api/session")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		assertThat(JsonPath.read<List<String>>(sessionResponse, "$.menus..code"))
			.contains("battle-rules.coverage", "battle-rules.fixtures", "battle-rules.test-runs")
			.doesNotContain("battle-rules.fixture-sources")
	}

	private fun issueBattleRulesToken(username: String): String {
		val userId = nextUserId.getAndIncrement()
		userRepository.save(
			SecurityUser {
				id = userId
				this.username = username
				passwordHash = "{noop}secret"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, BATTLE_RULES_ADMIN_ROLE_ID)

		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic("system-admin-jwt", "system-admin-jwt-secret"))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", "secret")
				.param("scope", "battle-rules:admin"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.access_token")
	}

	private companion object {
		private const val BATTLE_RULES_ADMIN_ROLE_ID = 203L
		private const val GOLDEN_REPLAY_FIXTURE_CODE = "golden-replay-pins-random-trace-event-fragment-and-final-hp"
		private val nextUserId = AtomicLong(52001)
	}
}
