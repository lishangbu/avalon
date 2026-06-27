package io.github.lishangbu.gamedata

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.atomic.AtomicLong

/**
 * 游戏资料 API 集成测试的公共认证夹具。
 */
abstract class GameDataApiTestSupport(
	private val userRepository: SecurityUserRepository,
	private val sqlClient: KSqlClient,
	private val webApplicationContext: WebApplicationContext,
) {
	protected lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUpMockMvc() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(webApplicationContext)
			.apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
			.build()
	}

	protected fun issueGameDataToken(username: String): String {
		insertGameDataUser(username)
		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic("system-admin-jwt", "system-admin-jwt-secret"))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", "secret")
				.param("scope", "game-data:admin"),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.access_token")
	}

	protected fun creatureJson(
		code: String,
		name: String,
		speciesId: Long = 1,
		height: Int = 10,
		weight: Int = 20,
	): String =
		"""
		{
		  "code": "$code",
		  "name": "$name",
		  "species_id": $speciesId,
		  "height": $height,
		  "weight": $weight,
		  "base_experience": 1,
		  "sort_order": 9999,
		  "default_form": true,
		  "enabled": true
		}
		""".trimIndent()

	protected fun nextCode(prefix: String): String =
		"$prefix-${nextSuffix.getAndIncrement()}"

	private fun insertGameDataUser(username: String) {
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
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, 202L)
	}

	private companion object {
		private val nextUserId = AtomicLong(12001)
		private val nextSuffix = AtomicLong(1)
	}
}
