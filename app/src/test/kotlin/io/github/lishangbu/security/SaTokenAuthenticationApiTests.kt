package io.github.lishangbu.security

import com.jayway.jsonpath.JsonPath
import cn.dev33.satoken.stp.StpUtil
import io.github.lishangbu.BackendApplication
import jakarta.servlet.Filter
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.SecurityTokenState
import io.github.lishangbu.security.entity.SecurityAdminAudit
import io.github.lishangbu.security.entity.expiresAt
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.entity.stateKey
import io.github.lishangbu.security.entity.stateValue
import io.github.lishangbu.security.entity.requestPath
import io.github.lishangbu.security.repository.SecurityUserRepository
import io.github.lishangbu.security.token.JimmerSaTokenDao
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Assertions.assertNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.time.Instant

/** 从 HTTP seam 验证 Sa-Token 登录、会话恢复和权限拒绝行为。 */
@Tag("integration")
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
	@Autowired private val tokenDao: JimmerSaTokenDao,
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
				.content("""{"username":"admin","password":"change-me-now"}"""),
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
				.content("""{"username":"admin","password":"change-me-now"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val token = JsonPath.read<String>(loginResponse, "$.tokenValue")

		mockMvc.perform(post("/api/auth/logout").header("avalon-token", token))
			.andExpect(status().isNoContent)

		mockMvc.perform(get("/api/session").header("avalon-token", token))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `locking an account revokes all of its issued tokens after commit`() {
		val username = "locked-account-token-owner"
		userRepository.save(
			SecurityUser {
				id = 13002L
				this.username = username
				passwordHash = "{noop}correct-password"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		val victimLoginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"$username","password":"correct-password"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val victimToken = JsonPath.read<String>(victimLoginResponse, "$.tokenValue")
		val adminLoginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"change-me-now"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val adminToken = JsonPath.read<String>(adminLoginResponse, "$.tokenValue")

		mockMvc.perform(
			post("/api/system/rbac/users/13002/lock")
				.header("avalon-token", adminToken),
		).andExpect(status().isOk)

		assertNull(StpUtil.getStpLogic().getLoginIdByToken(victimToken))
		mockMvc.perform(get("/api/session").header("avalon-token", victimToken))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `concurrent token state writes atomically upsert one row`() {
		val key = "test:concurrent-token-state"
		val start = CountDownLatch(1)
		val executor = Executors.newFixedThreadPool(8)
		try {
			val writes = (1..8).map { index ->
				executor.submit {
					start.await()
					tokenDao.set(key, "value-$index", 300)
				}
			}
			start.countDown()
			writes.forEach { it.get() }
		} finally {
			executor.shutdownNow()
		}

		val states = sqlClient.createQuery(SecurityTokenState::class) {
			where(table.stateKey eq key)
			select(table)
		}.execute()
		org.assertj.core.api.Assertions.assertThat(states).hasSize(1)
		org.assertj.core.api.Assertions.assertThat(states.single().stateValue).startsWith("value-")
	}

	@Test
	fun `reading expired token state removes the stale row`() {
		val key = "test:expired-token-state"
		sqlClient.save(
			SecurityTokenState {
				stateKey = key
				stateValue = "expired"
				expiresAt = Instant.EPOCH
			},
		)

		assertNull(tokenDao.get(key))

		val remaining = sqlClient.createQuery(SecurityTokenState::class) {
			where(table.stateKey eq key)
			select(table.stateKey)
		}.execute()
		org.assertj.core.api.Assertions.assertThat(remaining).isEmpty()
	}

	@Test
	fun `the last effective administrator cannot be locked`() {
		val adminToken = loginAdmin()

		mockMvc.perform(
			post("/api/system/rbac/users/301/lock")
				.header("avalon-token", adminToken),
		).andExpect(status().isConflict)

		org.assertj.core.api.Assertions.assertThat(StpUtil.getStpLogic().getLoginIdByToken(adminToken))
			.isEqualTo("301")
		val auditRecords = sqlClient.createQuery(SecurityAdminAudit::class) {
			where(table.requestPath eq "/api/system/rbac/users/301/lock")
			select(table)
		}.execute()
		org.assertj.core.api.Assertions.assertThat(auditRecords)
			.anyMatch { it.outcome == "FAILURE" && it.responseStatus == 409 }
	}

	@Test
	fun `the last effective administrator cannot lose all roles`() {
		mockMvc.perform(
			put("/api/system/rbac/users/301/roles")
				.header("avalon-token", loginAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"roleCodes":[]}"""),
		).andExpect(status().isConflict)
	}

	@Test
	fun `the last effective administrator role cannot lose the admin access node`() {
		mockMvc.perform(
			put("/api/system/rbac/roles/201")
				.header("avalon-token", loginAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"name":"System administrators","accessNodeCodes":[]}"""),
		).andExpect(status().isConflict)
	}

	@Test
	fun `a non administrator can have an empty role snapshot`() {
		userRepository.save(
			SecurityUser {
				id = 13003L
				username = "empty-role-owner"
				passwordHash = "{noop}correct-password"
				displayName = "Empty role owner"
				enabled = true
				accountNonLocked = true
			},
		)

		mockMvc.perform(
			put("/api/system/rbac/users/13003/roles")
				.header("avalon-token", loginAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"roleCodes":[]}"""),
		).andExpect(status().isOk)
			.andExpect(jsonPath("$.roleCodes").isEmpty)
	}

	private fun loginAdmin(): String {
		val response = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"admin","password":"change-me-now"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		return JsonPath.read(response, "$.tokenValue")
	}

	@Test
	fun `authenticated account without the required permission is forbidden`() {
		val username = "sa-token-game-data-admin"
		userRepository.save(
			SecurityUser {
				id = 13001L
				this.username = username
				passwordHash = "{noop}correct-password"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(13001L, 202L)
		val loginResponse = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"$username","password":"correct-password"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		val token = JsonPath.read<String>(loginResponse, "$.tokenValue")

		mockMvc.perform(get("/api/system/rbac/access-nodes").header("avalon-token", token))
			.andExpect(status().isForbidden)
			.andExpect(jsonPath("$.code").value("authorization.denied"))
	}
}
