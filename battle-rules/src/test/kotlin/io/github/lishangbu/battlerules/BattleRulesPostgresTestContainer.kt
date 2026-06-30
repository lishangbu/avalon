package io.github.lishangbu.battlerules

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * 为战斗规则模块测试提供独立 PostgreSQL 数据库。
 */
class BattleRulesPostgresTestContainer : ApplicationContextInitializer<ConfigurableApplicationContext> {
	override fun initialize(applicationContext: ConfigurableApplicationContext) {
		TestPropertyValues.of(
			*postgresProperties(),
		).applyTo(applicationContext.environment)
	}

	companion object {
		private val postgres = PostgreSQLContainer("postgres:latest")
			.withDatabaseName("backend_battle_rules_test")
			.withUsername("backend")
			.withPassword("backend")

		/**
		 * Spring 测试会为不同 service 测试类构建多个 ApplicationContext；在 IDE 或 Gradle 并发执行时，
		 * 多个 initializer 可能同时触碰同一个 companion object 容器。Testcontainers 的 `start()` 虽然能
		 * 处理已启动容器，但并发首次启动时容易出现“一个上下文读取到了尚未完全可连接的随机端口”的窗口。
		 *
		 * 这里用 JVM 级同步把“启动容器”和“读取 JDBC 属性”合成一个不可拆分的小临界区：第一个测试负责拉起
		 * `postgres:latest`，后续上下文只复用已经运行的容器属性。这样既保留最新版镜像要求，也避免偶发的
		 * `Connection refused` 让战斗规则服务测试变成不稳定测试。
		 */
		@Synchronized
		private fun postgresProperties(): Array<String> {
			if (!postgres.isRunning) {
				postgres.start()
			}
			return arrayOf(
				"spring.datasource.url=${postgres.jdbcUrl}",
				"spring.datasource.username=${postgres.username}",
				"spring.datasource.password=${postgres.password}",
			)
		}
	}
}
