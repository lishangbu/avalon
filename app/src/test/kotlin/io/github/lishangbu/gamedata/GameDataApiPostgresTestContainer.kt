package io.github.lishangbu.gamedata

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为游戏资料 API 测试提供独立 PostgreSQL 数据库。
 */
class GameDataApiPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
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
			.withDatabaseName("backend_game_data_api_test")
			.withUsername("backend")
			.withPassword("backend")
	}
}
