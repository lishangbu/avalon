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
import io.github.lishangbu.match.challenge.ChallengeService
import io.github.lishangbu.match.challenge.MatchChallengeRepository
import io.github.lishangbu.match.challenge.MatchTeamSnapshotRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.ApplicationRunner
import org.springframework.transaction.PlatformTransactionManager
import io.github.lishangbu.match.game.*
import io.github.lishangbu.match.runtime.BattleSessionHost
import io.github.lishangbu.match.event.PlayerEventPublisher
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories

@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.match"])
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
	fun challengeService(
		challenges: MatchChallengeRepository,
		snapshots: MatchTeamSnapshotRepository,
		trainers: TrainerService,
		teams: TrainerTeamService,
		presence: TrainerSessionRegistry,
		sqlClient: KSqlClient,
		events: PlayerEventPublisher,
	) = ChallengeService(challenges, snapshots, trainers, teams, presence, sqlClient, events)

	@Bean
	fun matchService(
		games: MatchGameRepository,
		participants: MatchParticipantRepository,
		reservations: MatchActiveAccountReservationRepository,
		turns: MatchTurnSubmissionRepository,
		snapshots: MatchTeamSnapshotRepository,
		teams: TrainerTeamService,
		presence: TrainerSessionRegistry,
		host: BattleSessionHost,
		sqlClient: KSqlClient,
		transactionManager: PlatformTransactionManager,
		events: PlayerEventPublisher,
	) = MatchService(games, participants, reservations, turns, snapshots, teams, presence, host, sqlClient, events, transactionManager)

	@Bean
	fun matchStartupRecovery(sqlClient: KSqlClient, transactionManager: PlatformTransactionManager) =
		MatchStartupRecovery(sqlClient, transactionManager)

	@Bean
	fun matchStartupRecoveryRunner(recovery: MatchStartupRecovery) = ApplicationRunner { recovery.recover() }

	@Bean
	fun trainerTeamService(
		teams: MatchTrainerTeamRepository,
		members: MatchTrainerTeamMemberRepository,
		skills: MatchTrainerTeamMemberSkillRepository,
		sqlClient: KSqlClient,
	) = TrainerTeamService(teams, members, skills, sqlClient)
}
