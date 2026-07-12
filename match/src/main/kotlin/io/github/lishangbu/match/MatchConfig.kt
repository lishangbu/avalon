package io.github.lishangbu.match

import io.github.lishangbu.match.trainer.TrainerService
import io.github.lishangbu.match.trainer.MatchTrainerRepository
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.match.trainer.TrainerSessionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories

@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.match.trainer"])
class MatchConfig {
	@Bean
	fun trainerSessionRegistry() = TrainerSessionRegistry()

	@Bean
	fun trainerService(repository: MatchTrainerRepository, sqlClient: KSqlClient, sessions: TrainerSessionRegistry) =
		TrainerService(repository, sqlClient, sessions = sessions)

	@Bean
	fun trainerSessionService(trainers: TrainerService, sessions: TrainerSessionRegistry) = TrainerSessionService(trainers, sessions)
}
