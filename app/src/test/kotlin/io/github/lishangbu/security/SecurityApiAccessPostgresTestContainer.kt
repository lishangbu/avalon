package io.github.lishangbu.security

import io.github.lishangbu.appPostgresTestProperties
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为 API 访问控制测试提供真实 PostgreSQL 数据库。
 */
class SecurityApiAccessPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		TestPropertyValues.of(
			*postgres.appPostgresTestProperties("security-api-access-test"),
		).applyTo(applicationContext.environment)
	}

	companion object {
		private val postgres = PostgreSQLContainer("postgres:18.4")
			.withDatabaseName("backend_security_api_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
