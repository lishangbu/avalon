package io.github.lishangbu

import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger

private val appCosIdContextSequence = AtomicInteger()

/**
 * 为 app 层 Spring 集成测试生成 PostgreSQL 和 CosId 测试属性。
 *
 * app 测试会创建多个真实 Spring `ApplicationContext`，并且这些上下文都会启动 CosId Snowflake。
 * 如果只复用默认 `host:pid` 实例号，多个上下文关闭时可能竞争释放同一份内存机器状态，从而在日志中出现
 * `NotFoundMachineStateException`。这里把容器启动、JDBC 属性读取和 CosId 测试实例号分配收拢到一个小函数：
 * - 容器首次启动和属性读取在同一个同步块内完成，避免并发初始化读到未就绪端口。
 * - 每个上下文拿到独立 `instance-id` 和手动 `machine-id`。
 * - 测试不依赖机器号续租，关闭 guarder 可以减少 JVM 退出时的生命周期噪声。
 */
internal fun PostgreSQLContainer.appPostgresTestProperties(instancePrefix: String): Array<String> =
	synchronized(this) {
		if (!isRunning) {
			start()
		}
		val cosIdContextId = appCosIdContextSequence.getAndIncrement()
		arrayOf(
			"spring.datasource.url=$jdbcUrl",
			"spring.datasource.username=$username",
			"spring.datasource.password=$password",
			"cosid.machine.instance-id=$instancePrefix-$cosIdContextId",
			"cosid.machine.distributor.manual.machine-id=$cosIdContextId",
			"cosid.machine.guarder.enabled=false",
		)
	}
