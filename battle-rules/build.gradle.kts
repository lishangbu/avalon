plugins {
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.ksp)
}

dependencies {
	implementation(project(":battle-engine"))
	implementation(project(":common-persistence"))
	implementation(project(":common-web"))
	implementation(libs.spring.boot.starter)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jimmer.spring.boot.starter)
	implementation(libs.kotlin.reflect)
	implementation(libs.springdoc.openapi.starter.webmvc.api)
	ksp(libs.jimmer.ksp)
	runtimeOnly(libs.postgresql)
	testImplementation(project(":migration"))
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.starter.liquibase)
	testImplementation(platform(libs.testcontainers.bom))
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
}

tasks.withType<Test> {
	// battle-rules 的 Spring 集成测试会频繁启动带 CosId Snowflake 的上下文，并通过 Testcontainers 访问数据库。
	// WSL 与容器组合下系统时钟偶尔会出现数百毫秒以上的短暂回拨；CosId 默认超过 500ms 就判定时钟损坏并抛出
	// ClockTooManyBackwardsException，导致 CRUD 测试随机失败。这里仅放宽测试任务的回拨容忍度，让测试线程等待
	// 时钟追平；生产配置、业务 ID 结构和数据库数据都不受影响。
	systemProperty("cosid.machine.clock-backwards.broken-threshold", "60000")
}
