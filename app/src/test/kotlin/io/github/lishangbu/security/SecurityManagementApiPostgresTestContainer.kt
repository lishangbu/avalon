package io.github.lishangbu.security

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为安全管理 API 测试提供真实 PostgreSQL 数据库。
 */
class SecurityManagementApiPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		postgres.start()

		TestPropertyValues.of(
			"spring.datasource.url=${postgres.jdbcUrl}",
			"spring.datasource.username=${postgres.username}",
			"spring.datasource.password=${postgres.password}",
		).applyTo(applicationContext.environment)
	}

	companion object {
		private val postgres = PostgreSQLContainer("postgres:latest")
			.withDatabaseName("backend_security_management_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
