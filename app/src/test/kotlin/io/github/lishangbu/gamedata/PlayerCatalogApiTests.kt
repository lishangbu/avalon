package io.github.lishangbu.gamedata

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.SecurityApiAccessPostgresTestContainer
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/** 从玩家 HTTP seam 验证只读资料目录的认证、聚合文本和媒体资源契约。 */
@Tag("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = ["spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml"],
)
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
class PlayerCatalogApiTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
	@Autowired private val userRepository: SecurityUserRepository,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUp() {
		val contextFilter = webApplicationContext.getBean("saTokenContextFilterForServlet", Filter::class.java)
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.addFilters<DefaultMockMvcBuilder>(contextFilter)
			.build()
	}

	@Test
	fun `authenticated account reads enabled creature catalog without admin permission`() {
		val token = loginPlayer(14001L, "catalog-creature-player")

		mockMvc.perform(
			get("/api/player/catalog/creatures")
				.header("avalon-token", token)
				.param("q", "bulbasaur")
				.param("size", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.total").value(1))
			.andExpect(jsonPath("$.items[0].id").value("1"))
			.andExpect(jsonPath("$.items[0].code").value("bulbasaur"))
			.andExpect(jsonPath("$.items[0].name").value("妙蛙种子"))
			.andExpect(jsonPath("$.items[0].genus").value("种子精灵"))
			.andExpect(jsonPath("$.items[0].flavorText").isNotEmpty)
			.andExpect(jsonPath("$.items[0].defaultSkin.id").value("200001"))
			.andExpect(jsonPath("$.items[0].defaultSkin.avatarAssetKey").value("reference/creatures/bulbasaur/avatar.webp"))
	}

	@Test
	fun `catalog exposes executable text and item usage for enabled resources`() {
		val token = loginPlayer(14002L, "catalog-rule-player")

		mockMvc.perform(get("/api/player/catalog/skills").header("avalon-token", token).param("q", "tackle"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items[0].code").value("tackle"))
			.andExpect(jsonPath("$.items[0].shortEffect").isNotEmpty)
			.andExpect(jsonPath("$.items[0].effect").isNotEmpty)
			.andExpect(jsonPath("$.items[0].flavorText").isNotEmpty)

		mockMvc.perform(get("/api/player/catalog/abilities").header("avalon-token", token).param("q", "stench"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items[0].code").value("stench"))
			.andExpect(jsonPath("$.items[0].shortEffect").isNotEmpty)

		mockMvc.perform(get("/api/player/catalog/items").header("avalon-token", token).param("q", "leftovers"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.items[0].code").value("leftovers"))
			.andExpect(jsonPath("$.items[0].usageType").value("HELD"))
			.andExpect(jsonPath("$.items[0].iconAssetKey").value("reference/items/leftovers/icon.webp"))
			.andExpect(jsonPath("$.items[0].shortEffect").isNotEmpty)
	}

	@Test
	fun `creature catalog requires an authenticated account`() {
		mockMvc.perform(get("/api/player/catalog/creatures"))
			.andExpect(status().isUnauthorized)
			.andExpect(jsonPath("$.code").value("authentication.required"))
	}

	private fun loginPlayer(id: Long, username: String): String {
		userRepository.save(
			SecurityUser {
				this.id = id
				this.username = username
				passwordHash = "{noop}correct-password"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		val response = mockMvc.perform(
			post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"username":"$username","password":"correct-password"}"""),
		).andExpect(status().isOk).andReturn().response.contentAsString
		return JsonPath.read(response, "$.tokenValue")
	}
}
