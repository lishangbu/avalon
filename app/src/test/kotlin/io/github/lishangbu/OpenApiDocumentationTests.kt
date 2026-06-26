package io.github.lishangbu

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 验证运行时 OpenAPI 文档能直接服务于接口说明、联调和前端类型生成。
 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [io.github.lishangbu.security.SecurityManagementApiPostgresTestContainer::class])
class OpenApiDocumentationTests(
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private val mockMvc: MockMvc by lazy {
		MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
	}

	@Test
	fun `openapi document exposes system api contract and oauth password security`() {
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.info.title").value("Avalon Backend API"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("oauth2"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.tokenUrl").value("/oauth2/token"))
			.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.flows.password.scopes['security:admin']").value("系统管理 API 访问权限"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].get.summary").value("查询用户列表"))
			.andExpect(jsonPath("$.paths['/api/system/rbac/users'].post.summary").value("创建用户"))
			.andExpect(jsonPath("$.paths['/api/system/oauth/clients/{clientId}/secret'].put.summary").value("重置 OAuth client secret"))
			.andExpect(jsonPath("$.paths['/api/system/scheduler/tasks/{taskId}/trigger'].post.responses['202'].description").value("已接受触发请求"))
	}

	@Test
	fun `swagger ui token request uses backend password grant type`() {
		mockMvc.perform(get("/swagger-ui/swagger-initializer.js"))
			.andExpect(status().isOk)
			.andExpect(content().string(containsString("requestInterceptor")))
			.andExpect(content().string(containsString("urn:security:params:oauth:grant-type:password")))
	}
}
