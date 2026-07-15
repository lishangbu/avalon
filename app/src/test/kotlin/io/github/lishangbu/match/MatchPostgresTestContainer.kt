package io.github.lishangbu.match

import io.github.lishangbu.appPostgresTestProperties
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

class MatchPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		TestPropertyValues.of(*postgres.appPostgresTestProperties("match-characterization-test"))
			.applyTo(applicationContext.environment)
	}

	private companion object {
		val postgres = PostgreSQLContainer("postgres:latest")
			.withDatabaseName("backend_match_characterization_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
