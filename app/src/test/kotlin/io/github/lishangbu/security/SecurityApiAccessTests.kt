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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

	private fun issueToken(clientId: String, clientSecret: String, username: String): String {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic(clientId, clientSecret))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", "secret")
				.param("scope", "security:admin"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.access_token")
	}

	private fun insertUser(username: String) {
		val userId = nextUserId.getAndIncrement()
		val now = OffsetDateTime.now(ZoneOffset.UTC)
		userRepository.save(
			SecurityUser {
				id = userId
				this.username = username
				passwordHash = "{noop}secret"
				displayName = username
				enabled = true
				accountNonLocked = true
				createdAt = now
				updatedAt = now
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, 201L)
	}

	private companion object {
		private val nextUserId = AtomicLong(10001)
	}
}
