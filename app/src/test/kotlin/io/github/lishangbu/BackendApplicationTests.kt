package io.github.lishangbu

import io.github.lishangbu.security.config.SecurityProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 验证应用装配能加载核心配置默认值，并避免把引导密钥留在运行配置中。
 */
@SpringBootTest(
	properties = [
		"spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,io.github.lishangbu.scheduler.SchedulerAutoConfiguration",
		"spring.liquibase.enabled=false",
		"backend.security.enabled=false",
		"management.health.db.enabled=false",
	],
)
class BackendApplicationTests(
	@Autowired private val securityProperties: SecurityProperties,
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	@Test
	fun contextLoads() {
		assertThat(securityProperties.issuer).isEqualTo("http://localhost:8080")
	}

	/**
	 * 验证生产探活端点不依赖业务鉴权、数据库迁移或 Quartz 调度初始化。
	 *
	 * 这个测试刻意复用当前应用上下文并通过 MockMvc 访问真实的 Web 映射，避免只检查配置字符串却漏掉 actuator
	 * 自动装配、HTTP 暴露范围或安全链调整造成的回归。部署平台和本地联调都只需要健康探活，因此这里固定
	 * `/actuator/health` 返回 `UP`，不额外暴露其它 actuator 端点。
	 */
	@Test
	fun `actuator health endpoint stays available for deployment probes`() {
		MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.build()
			.perform(get("/actuator/health"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.status").value("UP"))
	}

	@Test
	fun `player event metrics endpoint is exposed for monitoring`() {
		MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.build()
			.perform(get("/actuator/metrics/avalon.player.events.connections.active"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("avalon.player.events.connections.active"))
	}

	@Test
	fun `application yaml does not keep bootstrap oauth clients`() {
		val applicationYaml = javaClass.getResource("/application.yaml")!!.readText()

		assertThat(applicationYaml).doesNotContain("bootstrap-clients")
		assertThat(applicationYaml).doesNotContain("system-admin-jwt-secret")
		assertThat(applicationYaml).doesNotContain("system-admin-opaque-secret")
		assertThat(applicationYaml).doesNotContain("catalog:")
		assertThat(applicationYaml).doesNotContain("battle:")
	}
}
