package io.github.lishangbu.security

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import jakarta.servlet.Filter
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
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

/** 从 HTTP seam 验证 Sa-Token 登录、会话恢复和权限拒绝行为。 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = ["spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml"],
)
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
class SaTokenAuthenticationApiTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
	@Autowired @Qualifier("saTokenContextFilterForServlet") private val saTokenContextFilter: Filter,
	@Autowired private val userRepository: SecurityUserRepository,
	@Autowired private val sqlClient: KSqlClient,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.addFilters<DefaultMockMvcBuilder>(saTokenContextFilter)
			.build()
	}

	@Test
	fun `valid credentials issue a token that restores the current session`() {
		val loginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"123456"}"""),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.tokenName").value("avalon-token"))
			.andExpect(jsonPath("$.tokenValue").isNotEmpty)
			.andReturn()
			.response
			.contentAsString
		val token = JsonPath.read<String>(loginResponse, "$.tokenValue")

		mockMvc.perform(get("/api/session").header("avalon-token", token))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.user.username").value("admin"))
			.andExpect(jsonPath("$.accessNodeCodes").isArray)
	}

	@Test
	fun `invalid credentials are rejected without issuing a token`() {
		mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"wrong-password"}"""),
		)
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `current session requires authentication`() {
		mockMvc.perform(get("/api/session"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("authentication.required"))
	}

	@Test
	fun `logout invalidates the issued token`() {
		val loginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"123456"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val token = JsonPath.read<String>(loginResponse, "$.tokenValue")

		mockMvc.perform(post("/api/auth/logout").header("avalon-token", token))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/session").header("avalon-token", token))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `authenticated account without the required permission is forbidden`() {
		val username = "sa-token-game-data-admin"
		userRepository.save(
			SecurityUser {
				id = 13001L
				this.username = username
				passwordHash = "{noop}secret"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(13001L, 202L)
		val loginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"$username","password":"secret"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val token = JsonPath.read<String>(loginResponse, "$.tokenValue")

		mockMvc.perform(get("/api/system/rbac/access-nodes").header("avalon-token", token))
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("authorization.denied"))
	}
}
