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
import io.github.lishangbu.security.repository.OAuth2JwkRepository
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
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
	@Autowired private val authorizationService: OAuth2AuthorizationService,
	@Autowired private val jwkRepository: OAuth2JwkRepository,
	@Autowired private val jwkKeyFactory: OAuth2JwkKeyFactory,
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
		assertThat(registeredClientRepository.findByClientId("avalon-web")).isNotNull

		val clientIds = sqlClient.executeQuery(OAuth2Client::class) {
			orderBy(table.clientId)
			select(table.clientId)
		}
		assertThat(clientIds).containsExactly("avalon-web", "system-admin-jwt", "system-admin-opaque")

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
	fun `concurrent jwk source initialization creates exactly one active key`() {
		jwkRepository.deleteAll()
		val sourceCount = 6
		val executor = Executors.newFixedThreadPool(sourceCount)
		val ready = CountDownLatch(sourceCount)
		val start = CountDownLatch(1)

		try {
			val futures = (1..sourceCount).map {
				executor.submit {
					ready.countDown()
					check(start.await(10, TimeUnit.SECONDS)) { "JWK initialization start latch timed out" }
					JwkSource(jwkRepository, sqlClient, jwkKeyFactory).afterPropertiesSet()
				}
			}

			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue()
			start.countDown()
			futures.forEach { it.get(30, TimeUnit.SECONDS) }

			val activeKeyIds = sqlClient.executeQuery(OAuth2Jwk::class) {
				where(table.active eq true)
				select(table.keyId)
			}
			assertThat(activeKeyIds).hasSize(1)
		} finally {
			executor.shutdownNow()
		}
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

	@Test
	fun `public web client authenticates without a client secret`() {
		val accountId = insertUser("web-player")

		val response = mockMvc.perform(
			post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("client_id", "avalon-web")
				.param("grant_type", PASSWORD_GRANT_TYPE.value)
				.param("username", "web-player")
				.param("password", "secret")
				.param("scope", "player"),
		)
			.andExpect(status().isOk)
			.andReturn().response.contentAsString

		val token = objectMapper.readValue(response, TokenResponse::class.java)
		assertThat(token.accessToken).isNotBlank()
		assertThat(token.refreshToken).isNotBlank()
		val authorization = authorizationService.findByToken(token.accessToken, OAuth2TokenType.ACCESS_TOKEN)
		assertThat(authorization?.accessToken?.claims).containsEntry("account_id", accountId.toString())
	}

	@Test
	fun `public web client rotates refresh tokens without a secret`() {
		insertUser("refresh-player")
		val loginResponse = publicPasswordToken("refresh-player")

		val refreshResponse = mockMvc.perform(
			post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("client_id", "avalon-web")
				.param("grant_type", "refresh_token")
				.param("refresh_token", requireNotNull(loginResponse.refreshToken)),
		)
			.andExpect(status().isOk)
			.andReturn().response.contentAsString

		val rotated = objectMapper.readValue(refreshResponse, TokenResponse::class.java)
		assertThat(rotated.accessToken).isNotEqualTo(loginResponse.accessToken)
		assertThat(rotated.refreshToken).isNotBlank().isNotEqualTo(loginResponse.refreshToken)
	}

	private fun publicPasswordToken(username: String): TokenResponse {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("client_id", "avalon-web")
				.param("grant_type", PASSWORD_GRANT_TYPE.value)
				.param("username", username)
				.param("password", "secret")
				.param("scope", "player"),
		)
			.andExpect(status().isOk)
			.andReturn().response.contentAsString
		return objectMapper.readValue(response, TokenResponse::class.java)
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

	private fun insertUser(username: String): Long {
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
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, 201L)
		return userId
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
		@JsonProperty("refresh_token")
		val refreshToken: String? = null,
		@JsonProperty("token_type")
		val tokenType: String,
	)

	private companion object {
		private val nextUserId = AtomicLong(20001)
	}
}
