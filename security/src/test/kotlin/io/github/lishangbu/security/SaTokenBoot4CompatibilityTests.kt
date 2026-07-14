package io.github.lishangbu.security

import cn.dev33.satoken.config.SaTokenConfig
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

/** 验证 Sa-Token starter 可以在当前 Spring Boot 4 基线上完成自动配置。 */
@SpringBootTest(
	classes = [SaTokenBoot4CompatibilityTests.TestApplication::class],
	properties = [
		"sa-token.token-name=avalon-token",
		"spring.autoconfigure.exclude=org.babyfish.jimmer.spring.cfg.JimmerAutoConfiguration," +
			"org.babyfish.jimmer.spring.cfg.JimmerSpringGraphQLAutoConfiguration," +
			"org.babyfish.jimmer.spring.cfg.ServletControllerConfiguration," +
			"org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
	],
)
class SaTokenBoot4CompatibilityTests(
	@Autowired private val config: SaTokenConfig,
	@Autowired private val contextFilter: SaTokenContextFilterForJakartaServlet,
) {
	@Test
	fun `boot 4 context loads sa token configuration`() {
		assertThat(config.tokenName).isEqualTo("avalon-token")
		assertThat(contextFilter).isNotNull()
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	class TestApplication
}
