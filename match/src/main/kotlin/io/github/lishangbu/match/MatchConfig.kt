package io.github.lishangbu.match

import io.github.lishangbu.match.trainer.TrainerService
import io.github.lishangbu.match.trainer.MatchTrainerRepository
import io.github.lishangbu.match.trainer.TrainerSessionRegistry
import io.github.lishangbu.match.trainer.TrainerSessionService
import io.github.lishangbu.match.trainer.MatchTrainerTeamMemberRepository
import io.github.lishangbu.match.trainer.MatchTrainerTeamMemberSkillRepository
import io.github.lishangbu.match.trainer.MatchTrainerTeamRepository
import io.github.lishangbu.match.trainer.TrainerTeamService
import io.github.lishangbu.match.trainer.TrainerTeamShareService
import io.github.lishangbu.match.trainer.MatchTrainerTeamShareRepository
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
import java.time.Clock
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.match"])
class MatchConfig {
	@Bean
	fun trainerSessionRegistry() = TrainerSessionRegistry()

	@Bean
	fun trainerService(repository: MatchTrainerRepository, sqlClient: KSqlClient, sessions: TrainerSessionRegistry, clock: Clock) =
		TrainerService(repository, sqlClient, sessions = sessions, clock = clock)

	@Bean
	fun trainerSessionService(trainers: TrainerService, sessions: TrainerSessionRegistry, clock: Clock) = TrainerSessionService(trainers, sessions, clock)

	@Bean
	fun publicTrainerService(trainers: TrainerService, sessions: TrainerSessionService, registry: TrainerSessionRegistry, clock: Clock) =
		PublicTrainerService(trainers, sessions, registry, clock)

	@Bean
	fun challengeService(
		challenges: MatchChallengeRepository,
		snapshots: MatchTeamSnapshotRepository,
		trainers: TrainerService,
		teams: TrainerTeamService,
		presence: TrainerSessionRegistry,
		sqlClient: KSqlClient,
		events: PlayerEventPublisher,
		clock: Clock,
	) = ChallengeService(challenges, snapshots, trainers, teams, presence, sqlClient, events, clock)

	@Bean
	fun matchService(
		games: MatchGameRepository,
		participants: MatchParticipantRepository,
		reservations: MatchActiveAccountReservationRepository,
		turns: MatchTurnSubmissionRepository,
		previewSelections: MatchTeamPreviewSelectionRepository,
		snapshots: MatchTeamSnapshotRepository,
		teams: TrainerTeamService,
		presence: TrainerSessionRegistry,
		host: BattleSessionHost,
		sqlClient: KSqlClient,
		transactionManager: PlatformTransactionManager,
		events: PlayerEventPublisher,
		battleEvents: BattleEventProjector,
		clock: Clock,
	) = MatchService(games, participants, reservations, turns, previewSelections, snapshots, teams, presence, host, sqlClient, events, battleEvents, transactionManager, clock)

	@Bean
	fun battleEventProjector(objectMapper: ObjectMapper) = BattleEventProjector(objectMapper)

	@Bean
	fun matchStartupRecovery(sqlClient: KSqlClient, transactionManager: PlatformTransactionManager, clock: Clock) =
		MatchStartupRecovery(sqlClient, transactionManager, clock)

	@Bean
	fun matchStartupRecoveryRunner(recovery: MatchStartupRecovery) = ApplicationRunner { recovery.recover() }

	@Bean
	fun trainerTeamService(
		teams: MatchTrainerTeamRepository,
		members: MatchTrainerTeamMemberRepository,
		skills: MatchTrainerTeamMemberSkillRepository,
		sqlClient: KSqlClient,
	) = TrainerTeamService(teams, members, skills, sqlClient)

	@Bean
	fun trainerTeamShareService(
		shares: MatchTrainerTeamShareRepository,
		teams: TrainerTeamService,
		sqlClient: KSqlClient,
	) = TrainerTeamShareService(shares, teams, sqlClient)
}
