package io.github.lishangbu.migration.db

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为迁移测试启动 PostgreSQL Testcontainer 并写入 Spring 数据源属性。
 */
class MigrationPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		postgres.start()

		TestPropertyValues.of(
			"spring.datasource.url=${postgres.jdbcUrl}",
			"spring.datasource.username=${postgres.username}",
			"spring.datasource.password=${postgres.password}",
		).applyTo(applicationContext.environment)
	}

	companion object {
		private val postgres = PostgreSQLContainer("postgres:18.4")
			.withDatabaseName("backend_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
