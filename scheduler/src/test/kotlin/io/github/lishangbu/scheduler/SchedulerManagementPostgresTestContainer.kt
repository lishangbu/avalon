package io.github.lishangbu.scheduler

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

class SchedulerManagementPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
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
			.withDatabaseName("backend_scheduler_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
