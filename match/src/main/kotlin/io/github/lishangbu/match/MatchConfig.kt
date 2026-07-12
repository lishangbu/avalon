package io.github.lishangbu.match

import io.github.lishangbu.match.trainer.TrainerService
import io.github.lishangbu.match.trainer.MatchTrainerRepository
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.MatchTrainerTeamMemberRepository
import io.github.lishangbu.match.trainer.MatchTrainerTeamMemberSkillRepository
import io.github.lishangbu.match.trainer.MatchTrainerTeamRepository
import io.github.lishangbu.match.trainer.TrainerTeamService
import io.github.lishangbu.match.trainer.PublicTrainerService
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

	@Bean
	fun publicTrainerService(trainers: TrainerService, sessions: TrainerSessionService, registry: TrainerSessionRegistry) =
		PublicTrainerService(trainers, sessions, registry)

	@Bean
	fun trainerTeamService(
		teams: MatchTrainerTeamRepository,
		members: MatchTrainerTeamMemberRepository,
		skills: MatchTrainerTeamMemberSkillRepository,
		sqlClient: KSqlClient,
	) = TrainerTeamService(teams, members, skills, sqlClient)
}
