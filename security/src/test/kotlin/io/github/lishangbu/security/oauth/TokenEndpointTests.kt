package io.github.lishangbu.security.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.lishangbu.security.entity.OAuth2Client
import io.github.lishangbu.security.entity.OAuth2Jwk
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accessTokenFormat
import io.github.lishangbu.security.entity.active
import io.github.lishangbu.security.entity.clientId
import io.github.lishangbu.security.entity.keyId
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.JimmerRegisteredClientRepository
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest(
	classes = [SecurityTokenEndpointTestApplication::class],
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=0",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
		"security.issuer=http://localhost:8080",
	],
)
@ContextConfiguration(initializers = [SecurityTokenEndpointPostgresTestContainer::class])
/**
 * 验证授权服务器 token endpoint、数据库客户端和 JWK 签名链路。
 */
class BackendTokenEndpointTests(
	@Autowired private val userRepository: SecurityUserRepository,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val webApplicationContext: WebApplicationContext,
	@Autowired private val objectMapper: ObjectMapper,
	@Autowired private val registeredClientRepository: RegisteredClientRepository,
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
	fun `oauth clients and jwt signing keys are backed by database`() {
		assertThat(registeredClientRepository).isInstanceOf(JimmerRegisteredClientRepository::class.java)
		assertThat(registeredClientRepository.findByClientId("system-admin-jwt")).isNotNull
		assertThat(registeredClientRepository.findByClientId("system-admin-opaque")).isNotNull

		val clientIds = sqlClient.executeQuery(OAuth2Client::class) {
			orderBy(table.clientId)
			select(table.clientId)
		}
		assertThat(clientIds).containsExactly("system-admin-jwt", "system-admin-opaque")

		assertThat(tokenFormat("system-admin-jwt")).isEqualTo("self-contained")
		assertThat(tokenFormat("system-admin-opaque")).isEqualTo("reference")

		val activeKeyIds = sqlClient.executeQuery(OAuth2Jwk::class) {
			where(table.active eq true)
			orderBy(table.keyId)
			select(table.keyId)
		}
		assertThat(activeKeyIds).hasSize(1)
	}

	@Test
	fun `jwt client receives self contained access token`() {
		insertUser("jwt-admin")

		val response = token(
			clientId = "system-admin-jwt",
			clientSecret = "system-admin-jwt-secret",
			username = "jwt-admin",
			password = "secret",
		)

		assertThat(response.tokenType).isEqualTo("Bearer")
		assertThat(response.accessToken.split(".")).hasSize(3)
	}

	@Test
	fun `opaque client receives reference access token`() {
		insertUser("opaque-admin")

		val response = token(
			clientId = "system-admin-opaque",
			clientSecret = "system-admin-opaque-secret",
			username = "opaque-admin",
			password = "secret",
		)

		assertThat(response.tokenType).isEqualTo("Bearer")
		assertThat(response.accessToken).doesNotContain(".")
	}

	private fun token(
		clientId: String,
		clientSecret: String,
		username: String,
		password: String,
	): TokenResponse {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic(clientId, clientSecret))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", PASSWORD_GRANT_TYPE.value)
				.param("username", username)
				.param("password", password)
				.param("scope", "security:admin"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return objectMapper.readValue(response, TokenResponse::class.java)
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

	private fun tokenFormat(clientId: String): String? =
		sqlClient.executeQuery(OAuth2Client::class, limit = 1) {
			where(table.clientId eq clientId)
			select(table.accessTokenFormat)
		}.firstOrNull()

	/**
	 * token endpoint 响应中测试关心的最小字段。
	 */
	data class TokenResponse(
		@JsonProperty("access_token")
		val accessToken: String,
		@JsonProperty("token_type")
		val tokenType: String,
	)

	private companion object {
		private val nextUserId = AtomicLong(20001)
	}
}
