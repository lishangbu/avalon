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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

	@Test
	fun `player can create list archive and restore own trainer`() {
		insertUser("trainer-player")
		val token = issuePublicToken("trainer-player")

		val created = mockMvc.perform(
			post("/api/player/trainers")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"00000000-0000-4000-8000-000000000001","displayName":"Avalon一号"}"""),
		).andExpect(status().isCreated)
			.andExpect(jsonPath("$.displayName").value("Avalon一号"))
			.andReturn().response.contentAsString
		val trainerId: String = JsonPath.read(created, "$.id")

		mockMvc.perform(get("/api/player/trainers").header("Authorization", "Bearer $token"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$[0].id").value(trainerId))

		mockMvc.perform(
			post("/api/player/trainers/$trainerId/archive")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"expectedRevision":0}"""),
		).andExpect(status().isOk).andExpect(jsonPath("$.revision").value(1))

		mockMvc.perform(
			post("/api/player/trainers/$trainerId/restore")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"expectedRevision":1}"""),
		).andExpect(status().isOk).andExpect(jsonPath("$.revision").value(2))
	}

	@Test
	fun `player trainer session replaces previous credential and supports current and leave`() {
		insertUser("trainer-session-player")
		val token = issuePublicToken("trainer-session-player")
		val trainerIds = listOf("One", "Two").mapIndexed { index, name ->
			val response = mockMvc.perform(
				post("/api/player/trainers")
					.header("Authorization", "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"commandId":"00000000-0000-4000-8000-00000000001${index + 1}","displayName":"Session$name"}"""),
			).andExpect(status().isCreated).andReturn().response.contentAsString
			JsonPath.read<String>(response, "$.id")
		}

		fun enter(trainerId: String): String {
			val response = mockMvc.perform(
				post("/api/player/trainer-session")
					.header("Authorization", "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"trainerId":"$trainerId"}"""),
			).andExpect(status().isCreated)
				.andExpect(jsonPath("$.trainer.id").value(trainerId))
				.andReturn().response.contentAsString
			return JsonPath.read(response, "$.credential")
		}

		val firstCredential = enter(trainerIds[0])
		val secondCredential = enter(trainerIds[1])
		mockMvc.perform(
			get("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", firstCredential),
		).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("trainer-session.invalid"))
		insertUser("other-trainer-session-player")
		val otherToken = issuePublicToken("other-trainer-session-player")
		mockMvc.perform(
			get("/api/player/trainer-session")
				.header("Authorization", "Bearer $otherToken")
				.header("X-Trainer-Session", secondCredential),
		).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("trainer-session.invalid"))
		mockMvc.perform(
			get("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", secondCredential),
		).andExpect(status().isOk).andExpect(jsonPath("$.trainer.id").value(trainerIds[1]))
		mockMvc.perform(
			post("/api/player/trainer-session/heartbeat")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", secondCredential),
		).andExpect(status().isNoContent)
		mockMvc.perform(
			delete("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", secondCredential),
		).andExpect(status().isNoContent)
		mockMvc.perform(
			get("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", secondCredential),
		).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("trainer-session.invalid"))
	}

	@Test
	fun `player can find only the minimal exact public trainer profile`() {
		insertUser("public-trainer-searcher")
		insertUser("public-trainer-target")
		val searcherToken = issuePublicToken("public-trainer-searcher")
		val targetToken = issuePublicToken("public-trainer-target")

		fun createAndEnter(token: String, commandId: String, displayName: String): Pair<String, String> {
			val trainer = mockMvc.perform(
				post("/api/player/trainers")
					.header("Authorization", "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"commandId":"$commandId","displayName":"$displayName"}"""),
			).andExpect(status().isCreated).andReturn().response.contentAsString
			val trainerId = JsonPath.read<String>(trainer, "$.id")
			val session = mockMvc.perform(
				post("/api/player/trainer-session")
					.header("Authorization", "Bearer $token")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""{"trainerId":"$trainerId"}"""),
			).andExpect(status().isCreated).andReturn().response.contentAsString
			return trainerId to JsonPath.read(session, "$.credential")
		}

		val (_, searcherCredential) = createAndEnter(
			searcherToken,
			"00000000-0000-4000-8000-000000000031",
			"PublicSearcher",
		)
		val (targetTrainerId, targetCredential) = createAndEnter(
			targetToken,
			"00000000-0000-4000-8000-000000000032",
			"PublicTarget",
		)

		mockMvc.perform(
			get("/api/player/public-trainers")
				.header("Authorization", "Bearer $searcherToken")
				.header("X-Trainer-Session", searcherCredential)
				.param("displayName", " publictarget "),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.displayName").value("PublicTarget"))
			.andExpect(jsonPath("$.online").value(true))
			.andExpect(jsonPath("$.challengeable").value(true))
			.andExpect(jsonPath("$.id").doesNotExist())
			.andExpect(jsonPath("$.accountId").doesNotExist())

		mockMvc.perform(
			get("/api/player/public-trainers")
				.header("Authorization", "Bearer $searcherToken")
				.header("X-Trainer-Session", searcherCredential)
				.param("displayName", "Public"),
		).andExpect(status().isNotFound)
		mockMvc.perform(
			get("/api/player/public-trainers")
				.header("Authorization", "Bearer $searcherToken")
				.header("X-Trainer-Session", searcherCredential)
				.param("displayName", "PublicSearcher"),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.online").value(true))
			.andExpect(jsonPath("$.challengeable").value(false))

		mockMvc.perform(
			delete("/api/player/trainer-session")
				.header("Authorization", "Bearer $targetToken")
				.header("X-Trainer-Session", targetCredential),
		).andExpect(status().isNoContent)
		mockMvc.perform(
			get("/api/player/public-trainers")
				.header("Authorization", "Bearer $searcherToken")
				.header("X-Trainer-Session", searcherCredential)
				.param("displayName", "PublicTarget"),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.online").value(false))
			.andExpect(jsonPath("$.challengeable").value(false))

		mockMvc.perform(
			post("/api/player/trainers/$targetTrainerId/archive")
				.header("Authorization", "Bearer $targetToken")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"expectedRevision":0}"""),
		).andExpect(status().isOk)
		mockMvc.perform(
			get("/api/player/public-trainers")
				.header("Authorization", "Bearer $searcherToken")
				.header("X-Trainer-Session", searcherCredential)
				.param("displayName", "PublicTarget"),
		).andExpect(status().isNotFound)
	}

	@Test
	fun `player can replace and read the complete current trainer team`() {
		insertUser("trainer-team-player")
		val token = issuePublicToken("trainer-team-player")
		val trainerResponse = mockMvc.perform(
			post("/api/player/trainers")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"00000000-0000-4000-8000-000000000021","displayName":"TeamPlayer"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerId = JsonPath.read<String>(trainerResponse, "$.id")
		val sessionResponse = mockMvc.perform(
			post("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"trainerId":"$trainerId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val credential = JsonPath.read<String>(sessionResponse, "$.credential")
		val team = """
			{
			  "expectedRevision": null,
			  "members": [{
			    "creatureId": "1",
			    "skillIds": ["14"],
			    "abilityId": "65",
			    "itemId": "1",
			    "natureId": "1",
			    "individualValues": {},
			    "effortValues": {}
			  }]
			}
		""".trimIndent()

		mockMvc.perform(
			put("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(team),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.revision").value(0))
			.andExpect(jsonPath("$.members[0].creatureId").value("1"))
			.andExpect(jsonPath("$.members[0].individualValues.hp").value(31))
			.andExpect(jsonPath("$.members[0].effortValues.hp").value(0))

		mockMvc.perform(
			get("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.trainerId").value(trainerId))
			.andExpect(jsonPath("$.members[0].skillIds[0]").value("14"))

		val replacement = team.replace("\"expectedRevision\": null", "\"expectedRevision\": 0")
			.replace("\"itemId\": \"1\"", "\"itemId\": \"2\"")
		mockMvc.perform(
			put("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(replacement),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.revision").value(1))
			.andExpect(jsonPath("$.members[0].itemId").value("2"))

		mockMvc.perform(
			put("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(replacement),
		).andExpect(status().isConflict)
		mockMvc.perform(
			get("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.revision").value(1))
			.andExpect(jsonPath("$.members[0].itemId").value("2"))
	}

	@Test
	fun `invalid trainer team reference is rejected without saving a draft`() {
		insertUser("invalid-trainer-team-player")
		val token = issuePublicToken("invalid-trainer-team-player")
		val trainerResponse = mockMvc.perform(
			post("/api/player/trainers")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"00000000-0000-4000-8000-000000000022","displayName":"InvalidTeam"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerId = JsonPath.read<String>(trainerResponse, "$.id")
		val sessionResponse = mockMvc.perform(
			post("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"trainerId":"$trainerId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val credential = JsonPath.read<String>(sessionResponse, "$.credential")

		mockMvc.perform(
			put("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "expectedRevision": null,
					  "members": [{
					    "creatureId": "999999999",
					    "skillIds": ["1"],
					    "abilityId": "65",
					    "itemId": "1",
					    "natureId": "1"
					  }]
					}
					""".trimIndent(),
				),
		).andExpect(status().isUnprocessableEntity)
		mockMvc.perform(
			put("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""{"expectedRevision":null,"members":[{"creatureId":"1","skillIds":["14","014"],"abilityId":"65","itemId":"1","natureId":"1"}]}""",
				),
		).andExpect(status().isUnprocessableEntity)

		mockMvc.perform(
			get("/api/player/trainer-team")
				.header("Authorization", "Bearer $token")
				.header("X-Trainer-Session", credential),
		).andExpect(status().isNotFound)
	}

	@Test
	fun `concurrent first trainer team saves resolve as one success and one revision conflict`() {
		insertUser("concurrent-trainer-team-player")
		val token = issuePublicToken("concurrent-trainer-team-player")
		val trainerResponse = mockMvc.perform(
			post("/api/player/trainers")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"00000000-0000-4000-8000-000000000023","displayName":"ConcurrentTeam"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerId = JsonPath.read<String>(trainerResponse, "$.id")
		val sessionResponse = mockMvc.perform(
			post("/api/player/trainer-session")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"trainerId":"$trainerId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val credential = JsonPath.read<String>(sessionResponse, "$.credential")
		val team = """
			{"expectedRevision":null,"members":[{"creatureId":"1","skillIds":["14"],"abilityId":"65","itemId":"1","natureId":"1"}]}
		""".trimIndent()
		val ready = CountDownLatch(2)
		val start = CountDownLatch(1)
		val executor = Executors.newFixedThreadPool(2)
		try {
			val futures = List(2) {
				executor.submit<Int> {
					ready.countDown()
					check(start.await(10, TimeUnit.SECONDS))
					mockMvc.perform(
						put("/api/player/trainer-team")
							.header("Authorization", "Bearer $token")
							.header("X-Trainer-Session", credential)
							.contentType(MediaType.APPLICATION_JSON)
							.content(team),
					).andReturn().response.status
				}
			}
			check(ready.await(10, TimeUnit.SECONDS))
			start.countDown()
			assertThat(futures.map { it.get(30, TimeUnit.SECONDS) }.sorted()).containsExactly(200, 409)
		} finally {
			executor.shutdownNow()
		}
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

	private fun issuePublicToken(username: String): String {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("client_id", "avalon-web")
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", "secret")
				.param("scope", "player"),
		).andExpect(status().isOk).andReturn().response.contentAsString
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
