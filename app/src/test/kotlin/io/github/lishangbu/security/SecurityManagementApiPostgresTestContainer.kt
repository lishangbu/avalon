package io.github.lishangbu.security

import io.github.lishangbu.appPostgresTestProperties
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为安全管理 API 测试提供真实 PostgreSQL 数据库。
 */
class SecurityManagementApiPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		TestPropertyValues.of(
			*postgres.appPostgresTestProperties("security-management-test"),
		).applyTo(applicationContext.environment)
	}

	companion object {
		private val postgres = PostgreSQLContainer("postgres:18.4")
			.withDatabaseName("backend_security_management_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
