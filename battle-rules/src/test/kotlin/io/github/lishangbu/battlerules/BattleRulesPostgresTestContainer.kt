package io.github.lishangbu.battlerules

import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger

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
		private val cosIdContextSequence = AtomicInteger()

		/**
		 * Spring 测试会为不同 service 测试类构建多个 ApplicationContext；在 IDE 或 Gradle 并发执行时，
		 * 多个 initializer 可能同时触碰同一个 companion object 容器。Testcontainers 的 `start()` 虽然能
		 * 处理已启动容器，但并发首次启动时容易出现“一个上下文读取到了尚未完全可连接的随机端口”的窗口。
		 *
		 * 这里用 JVM 级同步把“启动容器”和“读取 JDBC 属性”合成一个不可拆分的小临界区：第一个测试负责拉起
		 * `postgres:latest`，后续上下文只复用已经运行的容器属性。这样既保留最新版镜像要求，也避免偶发的
		 * `Connection refused` 让战斗规则服务测试变成不稳定测试。
		 *
		 * 同一个 Gradle 测试 JVM 会缓存并关闭多个 Spring `ApplicationContext`。CosId 在未配置 `instance-id`
		 * 时默认使用 `host:pid`，这些上下文会共享同一个内存机器状态键；JVM 退出时如果多个上下文触发同一套
		 * guard/revert 流程，就可能抛 `NotFoundMachineStateException` 并污染测试日志。这里给每个真正创建的
		 * 测试上下文分配独立实例号和手动机器号，同时关闭测试不需要的机器号守护线程：服务测试只需要验证
		 * Snowflake 主键生成可用，不需要定时续租或 shutdown 时归还机器状态。
		 */
		@Synchronized
		private fun postgresProperties(): Array<String> {
			if (!postgres.isRunning) {
				postgres.start()
			}
			val cosIdContextId = cosIdContextSequence.getAndIncrement()
			return arrayOf(
				"spring.datasource.url=${postgres.jdbcUrl}",
				"spring.datasource.username=${postgres.username}",
				"spring.datasource.password=${postgres.password}",
				"cosid.machine.instance-id=battle-rules-test-$cosIdContextId",
				"cosid.machine.distributor.manual.machine-id=$cosIdContextId",
				"cosid.machine.guarder.enabled=false",
			)
		}
	}
}
