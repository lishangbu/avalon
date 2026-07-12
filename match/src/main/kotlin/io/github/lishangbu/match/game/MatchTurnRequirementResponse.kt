package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable

/** 当前 Trainer 必须为一个己方成员完成的人工行动选择。 */
@Immutable
interface MatchTurnRequirementResponse {
	val actorPosition: Int
	val options: List<MatchTurnOptionResponse>
}
