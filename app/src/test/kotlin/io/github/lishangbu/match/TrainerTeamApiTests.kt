package io.github.lishangbu.match

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.repository.SecurityUserRepository
import jakarta.servlet.Filter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

/** 从玩家 HTTP 边界验证多队伍、激活选择以及成员展示配置。 */
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(classes = [BackendApplication::class])
@ContextConfiguration(initializers = [MatchPostgresTestContainer::class])
class TrainerTeamApiTests(
	@Autowired private val context: WebApplicationContext,
	@Autowired private val users: SecurityUserRepository,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUp() {
		val filter = context.getBean("saTokenContextFilterForServlet", Filter::class.java)
		mockMvc = MockMvcBuilders.webAppContextSetup(context)
			.addFilters<DefaultMockMvcBuilder>(filter)
			.build()
	}

	@Test
	fun `trainer creates lists updates and activates named teams`() {
		val player = createPlayer(15001L, "multi-team-player", "Multi Team")
		val first = createTeam(player, "晴天队")
		val firstId = JsonPath.read<String>(first, "$.id")

		mockMvc.perform(
			get("/api/player/trainer-teams")
				.header("avalon-token", player.token)
				.header("X-Trainer-Session", player.credential),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$[0].name").value("晴天队"))
			.andExpect(jsonPath("$[0].active").value(true))
			.andExpect(jsonPath("$[0].members[0].gender").value("MALE"))
			.andExpect(jsonPath("$[0].members[0].skinId").value("200001"))
			.andExpect(jsonPath("$[0].members[0].teraElementId").value("12"))

		val second = createTeam(player, "雨天队")
		val secondId = JsonPath.read<String>(second, "$.id")
		val secondRevision = JsonPath.read<Int>(second, "$.revision")
		mockMvc.perform(
			put("/api/player/trainer-teams/$secondId")
				.header("avalon-token", player.token)
				.header("X-Trainer-Session", player.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(teamBody("雨天轮转", secondRevision.toLong())),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("雨天轮转"))
			.andExpect(jsonPath("$.revision").value(secondRevision + 1))

		mockMvc.perform(
			post("/api/player/trainer-teams/$secondId/activate")
				.header("avalon-token", player.token)
				.header("X-Trainer-Session", player.credential),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.active").value(true))

		mockMvc.perform(
			get("/api/player/trainer-teams/$firstId")
				.header("avalon-token", player.token)
				.header("X-Trainer-Session", player.credential),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.active").value(false))
	}

	@Test
	fun `trainer cannot create more than twenty teams`() {
		val player = createPlayer(15002L, "team-limit-player", "Limit Team")
		(1..20).forEach { createTeam(player, "队伍$it") }

		mockMvc.perform(
			post("/api/player/trainer-teams")
				.header("avalon-token", player.token)
				.header("X-Trainer-Session", player.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(teamBody("第二十一队")),
		)
			.andExpect(status().isUnprocessableEntity)
			.andExpect(jsonPath("$.code").value("trainer-team.limit-exceeded"))
	}

	@Test
	fun `shared team imports as an independently validated copy`() {
		val owner = createPlayer(15003L, "team-share-owner", "Share Owner")
		val source = createTeam(owner, "原始队伍")
		val sourceId = JsonPath.read<String>(source, "$.id")
		val sourceRevision = JsonPath.read<Int>(source, "$.revision")
		val share = mockMvc.perform(
			post("/api/player/trainer-teams/$sourceId/shares")
				.header("avalon-token", owner.token)
				.header("X-Trainer-Session", owner.credential),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val shareCode = JsonPath.read<String>(share, "$.code")

		mockMvc.perform(
			put("/api/player/trainer-teams/$sourceId")
				.header("avalon-token", owner.token)
				.header("X-Trainer-Session", owner.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(teamBody("已修改队伍", sourceRevision.toLong())),
		).andExpect(status().isOk)

		val importer = createPlayer(15004L, "team-share-importer", "Share Importer")
		mockMvc.perform(
			post("/api/player/trainer-teams/imports")
				.header("avalon-token", importer.token)
				.header("X-Trainer-Session", importer.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"shareCode":"$shareCode","name":"导入副本"}"""),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.name").value("导入副本"))
			.andExpect(jsonPath("$.active").value(true))
			.andExpect(jsonPath("$.members[0].creatureId").value("1"))
	}

	@Test
	fun `accepted challenge keeps both leads secret until team preview completes`() {
		val challenger = createPlayer(15005L, "preview-challenger", "Preview One")
		val challenged = createPlayer(15006L, "preview-challenged", "Preview Two")
		val challengerTeamId = JsonPath.read<String>(createTeam(challenger, "挑战队"), "$.id")
		val challengedTeamId = JsonPath.read<String>(createTeam(challenged, "应战队"), "$.id")
		val challenge = mockMvc.perform(
			post("/api/player/challenges")
				.header("avalon-token", challenger.token)
				.header("X-Trainer-Session", challenger.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"${UUID.randomUUID()}","challengedDisplayName":"Preview Two","teamId":"$challengerTeamId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val challengeId = JsonPath.read<String>(challenge, "$.id")
		val revision = JsonPath.read<Int>(challenge, "$.revision")
		val accepted = mockMvc.perform(
			post("/api/player/challenges/$challengeId/accept")
				.header("avalon-token", challenged.token)
				.header("X-Trainer-Session", challenged.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"expectedRevision":$revision,"teamId":"$challengedTeamId"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("PREVIEW"))
			.andExpect(jsonPath("$.previewDeadline").isNotEmpty)
			.andReturn().response.contentAsString
		val matchId = JsonPath.read<String>(accepted, "$.id")

		mockMvc.perform(
			post("/api/player/matches/$matchId/lead")
				.header("avalon-token", challenger.token)
				.header("X-Trainer-Session", challenger.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"leadPosition":1}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("PREVIEW"))
			.andExpect(jsonPath("$.leadPosition").value(1))
			.andExpect(jsonPath("$.sides[1].participants[0].skinId").value("200001"))

		mockMvc.perform(
			get("/api/player/matches/$matchId")
				.header("avalon-token", challenged.token)
				.header("X-Trainer-Session", challenged.credential),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.leadPosition").doesNotExist())

		val active = mockMvc.perform(
			post("/api/player/matches/$matchId/lead")
				.header("avalon-token", challenged.token)
				.header("X-Trainer-Session", challenged.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"leadPosition":1}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.battleDeadline").isNotEmpty)
			.andExpect(jsonPath("$.requirements[0].options[0].canTerastallize").value(true))
			.andReturn().response.contentAsString
		val activeRevision = JsonPath.read<Int>(active, "$.revision")

		mockMvc.perform(
			post("/api/player/matches/$matchId/turns")
				.header("avalon-token", challenger.token)
				.header("X-Trainer-Session", challenger.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(turnBody(activeRevision, terastallize = true)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.locked").value(true))
			.andExpect(jsonPath("$.match").doesNotExist())

		mockMvc.perform(
			post("/api/player/matches/$matchId/turns")
				.header("avalon-token", challenged.token)
				.header("X-Trainer-Session", challenged.credential)
				.contentType(MediaType.APPLICATION_JSON)
				.content(turnBody(activeRevision, terastallize = false)),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.match.events[?(@.code == 'participant-terastallized')]").isNotEmpty)
			.andExpect(jsonPath("$.match.events[?(@.code == 'participant-terastallized')].parameters.teraElementId").isNotEmpty)
	}

	private fun turnBody(revision: Int, terastallize: Boolean): String = """
		{
		  "submissionId":"${UUID.randomUUID()}",
		  "expectedRevision":$revision,
		  "actions":[{
		    "actorPosition":1,
		    "type":"USE_SKILL",
		    "skillId":33,
		    "targetPosition":1,
		    "targetYou":false,
		    "terastallize":$terastallize
		  }]
		}
	""".trimIndent()

	private fun createTeam(player: PlayerAccess, name: String): String = mockMvc.perform(
		post("/api/player/trainer-teams")
			.header("avalon-token", player.token)
			.header("X-Trainer-Session", player.credential)
			.contentType(MediaType.APPLICATION_JSON)
			.content(teamBody(name)),
	).andExpect(status().isCreated).andReturn().response.contentAsString

	private fun teamBody(name: String, expectedRevision: Long? = null): String = """
		{
		  "name":"$name",
		  "expectedRevision":${expectedRevision ?: "null"},
		  "members":[{
		    "creatureId":"1",
		    "gender":"MALE",
		    "skinId":"200001",
		    "skillIds":["33"],
		    "abilityId":"65",
		    "itemId":"211",
		    "teraElementId":"12"
		  }]
		}
	""".trimIndent()

	private fun createPlayer(id: Long, username: String, trainerName: String): PlayerAccess {
		users.save(SecurityUser {
			this.id = id
			this.username = username
			passwordHash = "{noop}correct-password"
			displayName = username
			enabled = true
			accountNonLocked = true
		})
		val login = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"$username","password":"correct-password"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val token = JsonPath.read<String>(login, "$.tokenValue")
		val trainer = mockMvc.perform(
			post("/api/player/trainers")
				.header("avalon-token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"${UUID.randomUUID()}","displayName":"$trainerName"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerId = JsonPath.read<String>(trainer, "$.id")
		val session = mockMvc.perform(
			post("/api/player/trainer-session")
				.header("avalon-token", token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"trainerId":"$trainerId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		return PlayerAccess(token, JsonPath.read(session, "$.credential"))
	}

	private data class PlayerAccess(val token: String, val credential: String)
}
