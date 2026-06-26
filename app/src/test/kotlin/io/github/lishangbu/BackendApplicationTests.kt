package io.github.lishangbu

import io.github.lishangbu.security.config.SecurityProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

/**
 * 验证应用装配能加载核心配置默认值，并避免把引导密钥留在运行配置中。
 */
@SpringBootTest(
	properties = [
		"spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,io.github.lishangbu.scheduler.SchedulerAutoConfiguration",
		"spring.liquibase.enabled=false",
		"backend.security.enabled=false",
	],
)
class BackendApplicationTests(
	@Autowired private val securityProperties: SecurityProperties,
) {
	@Test
	fun contextLoads() {
		assertThat(securityProperties.issuer).isEqualTo("http://localhost:8080")
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
