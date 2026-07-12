package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSessionCreateRequest
import io.github.lishangbu.battlerules.dto.BattleSessionRosterParticipantRequest
import io.github.lishangbu.battlerules.dto.BattleSessionRosterSideRequest
import io.github.lishangbu.battlesession.BattleSessionRuntime
import io.github.lishangbu.battlesession.model.TerminationCommand
import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.match.runtime.BattleSessionHost
import io.github.lishangbu.match.runtime.HostedBattleRoster
import java.util.UUID
import org.springframework.stereotype.Component

/** 使用现有规则装配与进程内 Runtime 实现 Match 的 Battle Session Host seam。 */
@Component
class BattleRulesSessionHost(
	private val sessions: BattleSessionService,
	private val runtime: BattleSessionRuntime,
) : BattleSessionHost {
	override fun start(roster: HostedBattleRoster): String = sessions.create(
		BattleSessionCreateRequest(
			formatCode = roster.formatCode,
			sides = roster.sides.map { side ->
				BattleSessionRosterSideRequest(
					activeParticipantIndexes = side.activeParticipantIndexes,
					participants = side.participants.map { participant ->
						BattleSessionRosterParticipantRequest(
							creatureId = participant.creatureId,
							level = participant.level,
							skillIds = participant.skillIds,
							abilityId = participant.abilityId,
							itemId = participant.itemId,
							individualValues = participant.individualValues,
							effortValues = participant.effortValues,
							natureIncreasedStat = participant.natureIncreasedStat,
							natureDecreasedStat = participant.natureDecreasedStat,
						)
					},
				)
			},
		),
	).sessionId

	override fun execute(sessionId: String, command: TurnCommand) = runtime.submitTurn(sessionId, command)

	override fun terminate(sessionId: String) {
		val session = runtime.get(sessionId)
		runtime.terminate(
			sessionId,
			TerminationCommand(UUID.randomUUID().toString(), session.revision, "MATCH_TERMINATED"),
		)
	}
}
