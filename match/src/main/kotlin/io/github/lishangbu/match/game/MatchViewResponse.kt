package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import java.time.Instant

/** 按当前 Trainer 投影的 Match View，不包含内部 side 与 Battle Session 标识。 */
@Immutable
interface MatchViewResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val ruleCode: String
	val status: MatchStatus
	val revision: Long
	val turnNumber: Int
	val turnDeadline: Instant?
	val result: String?
	val completionReason: MatchCompletionReason?
	val interruptionReason: MatchInterruptionReason?
	val sides: List<MatchViewSideResponse>
	val requirements: List<MatchTurnRequirementResponse>
}
