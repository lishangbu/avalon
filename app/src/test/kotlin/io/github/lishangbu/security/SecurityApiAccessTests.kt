package io.github.lishangbu.security

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.scheduler.ScheduledTaskOperations
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
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
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
/**
 * 端到端验证 JWT 和 reference token 都能驱动 API 权限校验。
 */
class SecurityApiAccessTests(
	@Autowired private val userRepository: SecurityUserRepository,
	@Autowired private val scheduledTaskOperations: ScheduledTaskOperations,
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
	fun `scheduler operations bean is available in application context`() {
		assertThat(scheduledTaskOperations).isNotNull()
	}

	@Test
	fun `security api requires authentication`() {
		mockMvc.perform(get("/api/system/rbac/access-nodes"))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `battle session api requires its dedicated scope`() {
		val missingSessionPath = "/api/battle-sessions/00000000-0000-4000-8000-000000000000"
		mockMvc.perform(get(missingSessionPath))
			.andExpect(status().isUnauthorized)

		insertUser("battle-session-wrong-scope", roleId = 202L)
		val wrongScopeToken = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "battle-session-wrong-scope",
			scope = "game-data:admin",
		)
		mockMvc.perform(
			get(missingSessionPath)
				.header("Authorization", "Bearer $wrongScopeToken"),
		).andExpect(status().isForbidden)

		insertUser("battle-session-api-runner", roleId = 201L)
		val battleSessionToken = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "battle-session-api-runner",
			scope = "battle-sessions:run",
		)
		mockMvc.perform(
			get(missingSessionPath)
				.header("Authorization", "Bearer $battleSessionToken"),
		).andExpect(status().isNotFound)
	}

	@Test
	fun `jwt token with security admin can access security api`() {
		insertUser("jwt-api-admin")
		val token = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "jwt-api-admin",
		)

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isOk)
	}

	@Test
	fun `opaque token with security admin can access security api`() {
		insertUser("opaque-api-admin")
		val token = issueToken(
			clientId = "system-admin-opaque",
			clientSecret = "system-admin-opaque-secret",
			username = "opaque-api-admin",
		)

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isOk)
	}

	@Test
	fun `seeded admin can log in with documented development credentials`() {
		val token = issueToken(
			clientId = "system-admin-opaque",
			clientSecret = "system-admin-opaque-secret",
			username = "admin",
			password = "123456",
		)

		assertThat(token).isNotBlank()
	}

	@Test
	fun `jwt token with game data admin can access game data api only`() {
		insertUser("game-data-api-admin", roleId = 202L)
		val token = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "game-data-api-admin",
			scope = "game-data:admin",
		)

		mockMvc.perform(
			get("/api/game-data/creatures")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].code").value("bulbasaur"))
			.andExpect(jsonPath("$.rows[0].name").value("妙蛙种子"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isForbidden)

		mockMvc.perform(
			post("/api/battle-sandbox/turn")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"),
		).andExpect(status().isForbidden)
	}

	@Test
	fun `jwt token with battle rules admin can access battle rules api only`() {
		insertUser("battle-rules-api-admin", roleId = 203L)
		val token = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "battle-rules-api-admin",
			scope = "battle-rules:admin",
		)

		mockMvc.perform(
			get("/api/battle-rules/battle-formats")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].code").value("standard-single"))
			.andExpect(jsonPath("$.rows[0].name").value("标准单打"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isForbidden)
	}

	@Test
	fun `jwt token with battle sandbox runner can execute sandbox api only`() {
		insertUser("battle-sandbox-api-runner", roleId = 204L)
		val token = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "battle-sandbox-api-runner",
			scope = "battle-sandbox:run",
		)

		mockMvc.perform(
			post("/api/battle-sandbox/turn")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "formatCode": "standard-single",
					  "randomSeed": 0,
					  "sides": [
					    {
					      "sideId": "side-a",
					      "activeActorIds": ["a-1"],
					      "participants": [
					        {
					          "actorId": "a-1",
					          "creatureId": 1,
					          "level": 50,
					          "skillIds": [1]
					        }
					      ]
					    },
					    {
					      "sideId": "side-b",
					      "activeActorIds": ["b-1"],
					      "participants": [
					        {
					          "actorId": "b-1",
					          "creatureId": 4,
					          "level": 50,
					          "skillIds": [1]
					        }
					      ]
					    }
					  ],
					  "actions": [
					    {
					      "type": "USE_SKILL",
					      "actorId": "a-1",
					      "skillId": 1,
					      "targetActorId": "b-1"
					    },
					    {
					      "type": "USE_SKILL",
					      "actorId": "b-1",
					      "skillId": 1,
					      "targetActorId": "a-1"
					    }
					  ]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.resolved").value(true))
			.andExpect(jsonPath("$.turnNumber").value(1))
			.andExpect(jsonPath("$.sides[0].sideId").value("side-a"))
			.andExpect(jsonPath("$.sides[0].participants[0].actorId").value("a-1"))
			.andExpect(jsonPath("$.sides[1].sideId").value("side-b"))
			.andExpect(jsonPath("$.sides[1].participants[0].actorId").value("b-1"))
			.andExpect(jsonPath("$.events[?(@.type == 'DamageApplied')].message").isNotEmpty())
			.andExpect(jsonPath("$.randomTrace").isNotEmpty())
			.andExpect(jsonPath("$.violations").isEmpty())

		mockMvc.perform(
			get("/api/battle-rules/battle-formats")
				.header("Authorization", "Bearer $token"),
		).andExpect(status().isForbidden)
	}

	@Test
	fun `game data api supports exact field filters`() {
		insertUser("game-data-filter-admin", roleId = 202L)
		val token = issueToken(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "game-data-filter-admin",
			scope = "game-data:admin",
		)

		mockMvc.perform(
			get("/api/game-data/creatures")
				.header("Authorization", "Bearer $token")
				.param("species_id", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].code").value("bulbasaur"))
			.andExpect(jsonPath("$.rows[0].species_id").value(1))

		mockMvc.perform(
			get("/api/game-data/creatures")
				.header("Authorization", "Bearer $token")
				.param("unknown_id", "1"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("unknown_id"))
	}

	private fun issueToken(
		clientId: String,
		clientSecret: String,
		username: String,
		scope: String = "security:admin",
		password: String = "secret",
	): String {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic(clientId, clientSecret))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", password)
				.param("scope", scope),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.access_token")
	}

	private fun insertUser(username: String, roleId: Long = 201L) {
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
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, roleId)
	}

	private companion object {
		private val nextUserId = AtomicLong(10001)
	}
}
