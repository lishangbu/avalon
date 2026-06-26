package io.github.lishangbu.scheduler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.lishangbu.scheduler.repository.ScheduledTaskExecutionRecordRepository
import io.github.lishangbu.scheduler.repository.ScheduledTaskRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.quartz.Scheduler
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.quartz.autoconfigure.SchedulerFactoryBeanCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.quartz.SchedulerFactoryBean

/**
 * 定时任务模块的 Spring Boot 自动配置。
 *
 * 自动配置注册 Quartz 适配层和管理端任务 Repository，真正的 Scheduler 仍由 Spring Boot Quartz 自动配置创建。
 */
@AutoConfiguration(
	afterName = ["org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration"],
	beforeName = ["org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration"],
)
@ConditionalOnClass(Scheduler::class, SchedulerFactoryBean::class)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.scheduler.repository"])
class SchedulerAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	fun scheduledTaskObjectMapper(): ObjectMapper =
		jacksonObjectMapper()

	@Bean
	@ConditionalOnMissingBean
	fun scheduledTaskRegistry(handlers: ObjectProvider<ScheduledTaskHandler>): ScheduledTaskRegistry =
		ScheduledTaskRegistry(handlers.orderedStream().toList())

	@Bean
	@ConditionalOnMissingBean
	fun scheduledTaskOperations(
		scheduler: Scheduler,
		registry: ScheduledTaskRegistry,
		objectMapper: ObjectMapper,
	): ScheduledTaskOperations =
		QuartzScheduledTaskOperations(scheduler, registry, objectMapper)

	@Bean
	@ConditionalOnMissingBean
	fun scheduledTaskExecutionRecorder(
		repository: ScheduledTaskExecutionRecordRepository,
		objectMapper: ObjectMapper,
	): ScheduledTaskExecutionRecorder =
		JimmerScheduledTaskExecutionRecorder(repository, objectMapper)

	@Bean
	@ConditionalOnMissingBean
	fun scheduledTaskManagementService(
		taskRepository: ScheduledTaskRepository,
		executionRepository: ScheduledTaskExecutionRecordRepository,
		sqlClient: KSqlClient,
		operations: ScheduledTaskOperations,
		registry: ScheduledTaskRegistry,
		scheduler: Scheduler,
		objectMapper: ObjectMapper,
	): ScheduledTaskManagementService =
		ScheduledTaskManagementService(
			taskRepository,
			executionRepository,
			sqlClient,
			operations,
			registry,
			scheduler,
			objectMapper,
		)

	@Bean
	fun scheduledTaskJobFactoryCustomizer(
		registry: ScheduledTaskRegistry,
		objectMapper: ObjectMapper,
		executionRecorder: ScheduledTaskExecutionRecorder,
	): SchedulerFactoryBeanCustomizer =
		SchedulerFactoryBeanCustomizer { schedulerFactoryBean ->
			schedulerFactoryBean.setJobFactory(
				DelegatingScheduledTaskJobFactory(registry, objectMapper, executionRecorder),
			)
		}

	@Bean
	fun scheduledTaskReconcileRunner(
		serviceProvider: ObjectProvider<ScheduledTaskManagementService>,
	): ApplicationRunner =
		ApplicationRunner {
			serviceProvider.ifAvailable(ScheduledTaskManagementService::reconcileEnabledTasks)
		}
}
