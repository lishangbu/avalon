package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import java.time.Instant

/** Match 玩家视图从不暴露 battleSessionId。 */
@Immutable
interface MatchResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val ruleCode: String
	val status: MatchStatus
	val revision: Long
	val turnNumber: Int
	val previewDeadline: Instant?
	val turnDeadline: Instant?
	val battleDeadline: Instant?
	val interruptionReason: MatchInterruptionReason?
	val participants: List<MatchParticipantResponse>
	val startedAt: Instant?
	val endedAt: Instant?
}
