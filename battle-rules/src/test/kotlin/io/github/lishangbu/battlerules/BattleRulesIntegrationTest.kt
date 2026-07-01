package io.github.lishangbu.battlerules

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

/**
 * 战斗规则模块 Spring 集成测试的统一入口注解。
 *
 * 这些测试都需要同一套上下文：最小测试应用、Liquibase 初始 schema/数据、Jimmer Kotlin + PostgreSQL 方言、
 * CosId Snowflake 主键生成和 Testcontainers PostgreSQL。之前每个测试类都复制一份 `@SpringBootTest`
 * 属性，导致修复 CosId 测试生命周期或数据库容器参数时要改很多文件。本注解把公共配置集中起来；每个测试类只
 * 负责表达自己验证哪个 service 或运行时装配边界。
 *
 * 手动机器号和实例号不在这里写死，而是由 [BattleRulesPostgresTestContainer] 在每个真实创建的测试上下文里
 * 分配，避免多个 Spring 上下文关闭时互相释放同一份 CosId 内存机器状态。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SpringBootTest(
	classes = [BattleRulesTestApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
		"jimmer.language=kotlin",
		"jimmer.dialect=org.babyfish.jimmer.sql.dialect.PostgresDialect",
		"cosid.machine.enabled=true",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
@ContextConfiguration(initializers = [BattleRulesPostgresTestContainer::class])
internal annotation class BattleRulesIntegrationTest
