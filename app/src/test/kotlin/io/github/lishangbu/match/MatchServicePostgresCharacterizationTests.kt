package io.github.lishangbu.match

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import jakarta.servlet.Filter
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

@Tag("integration")
@SpringBootTest(classes = [BackendApplication::class])
@ContextConfiguration(initializers = [MatchPostgresTestContainer::class])
class MatchServicePostgresCharacterizationTests(
	@Autowired private val context: WebApplicationContext,
	@Autowired @Qualifier("saTokenContextFilterForServlet") private val saTokenFilter: Filter,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(context)
			.addFilters<DefaultMockMvcBuilder>(saTokenFilter)
			.build()
	}

	@Test
	fun `new trainer has an empty authoritative match history`() {
		val accessToken = login()
		val trainerResponse = mockMvc.perform(
			post("/api/player/trainers")
				.header("avalon-token", accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"commandId":"${UUID.randomUUID()}","displayName":"Char Trainer"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerId = JsonPath.read<String>(trainerResponse, "$.id")
		val sessionResponse = mockMvc.perform(
			post("/api/player/trainer-session")
				.header("avalon-token", accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"trainerId":"$trainerId"}"""),
		).andExpect(status().isCreated).andReturn().response.contentAsString
		val trainerCredential = JsonPath.read<String>(sessionResponse, "$.credential")

		mockMvc.perform(
			get("/api/player/matches/history")
				.header("avalon-token", accessToken)
				.header("X-Trainer-Session", trainerCredential),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$").isEmpty)
	}

	private fun login(): String {
		val response = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"change-me-now"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		return JsonPath.read(response, "$.tokenValue")
	}
}
