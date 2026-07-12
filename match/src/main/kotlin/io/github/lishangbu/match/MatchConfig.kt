package io.github.lishangbu.match

import io.github.lishangbu.match.trainer.TrainerService
import io.github.lishangbu.match.trainer.TrainerStore
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.match.trainer.TrainerSessionService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MatchConfig {
	@Bean
	fun trainerSessionRegistry() = TrainerSessionRegistry()

	@Bean
	fun trainerService(store: TrainerStore, sessions: TrainerSessionRegistry) = TrainerService(store, sessions = sessions)

	@Bean
	fun trainerSessionService(store: TrainerStore, sessions: TrainerSessionRegistry) = TrainerSessionService(store, sessions)
}
