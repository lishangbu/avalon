package io.github.lishangbu.match.runtime

import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.model.TurnCommandResult

/** Match 对 Battle Session 的唯一依赖方向。 */
interface BattleSessionHost {
	fun start(roster: HostedBattleRoster): String
	fun execute(sessionId: String, command: TurnCommand): TurnCommandResult
	fun terminate(sessionId: String)
}
