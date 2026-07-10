package io.github.lishangbu.scheduler

import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean

/**
 * 定时任务管理集成测试使用的最小 Spring Boot 应用。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
class SchedulerManagementTestApplication {
	@Bean
	fun schedulerManagementTestHandler(): ScheduledTaskHandler =
		SchedulerManagementTestHandler()
}
