package io.github.lishangbu.match.runtime

import io.github.lishangbu.battlesession.model.TurnCommand
import io.github.lishangbu.battlesession.model.TurnCommandResult
import io.github.lishangbu.battlesession.roster.SessionRosterLayout

/** Match 对 Battle Session 的唯一依赖方向。 */
interface BattleSessionHost {
	fun start(roster: SessionRosterLayout): String
	fun execute(sessionId: String, command: TurnCommand): TurnCommandResult
	fun terminate(sessionId: String)
}
