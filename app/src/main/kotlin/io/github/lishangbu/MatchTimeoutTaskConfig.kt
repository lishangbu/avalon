package io.github.lishangbu

import io.github.lishangbu.match.game.MatchService
import io.github.lishangbu.scheduler.*
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.ObjectProvider
import java.time.Duration

/** 将 Match deadline 扫描注册到现有 Quartz 边界，避免另建本地线程调度器。 */
@Configuration(proxyBeanMethods = false)
class MatchTimeoutTaskConfig {
	@Bean
	fun matchTimeoutTaskHandler(matches: MatchService) = object : ScheduledTaskHandler {
		override val code = "match-turn-timeout"
		override fun execute(execution: ScheduledTaskExecution) {
			matches.adjudicateExpiredPreviews()
			matches.adjudicateExpiredBattles()
			matches.adjudicateExpiredTurns()
		}
	}

	@Bean
	fun matchTimeoutTaskRegistration(operations: ObjectProvider<ScheduledTaskOperations>) = ApplicationRunner {
		val scheduler = operations.ifAvailable ?: return@ApplicationRunner
		val reference = ScheduledTaskReference(taskId = "match-turn-timeout", group = "match-runtime")
		// Quartz 持久 Job 会跨进程保留；重启时复用既有注册，避免集群恢复期间替换同名 Trigger。
		if (scheduler.exists(reference)) return@ApplicationRunner
		scheduler.schedule(ScheduledTaskRequest(
			taskId = "match-turn-timeout",
			taskCode = "match-turn-timeout",
			schedule = ScheduledTaskSchedule.FixedInterval(Duration.ofSeconds(1)),
			group = "match-runtime",
			description = "裁定已超过绝对回合期限的 Match",
		))
	}
}
