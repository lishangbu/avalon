package io.github.lishangbu.match.challenge

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.Immutable
import java.time.Instant

/** Challenge 玩家视图不包含任一方 Snapshot、Lead 或成员资料。 */
@Immutable
interface ChallengeResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val direction: ChallengeDirection
	val challengerDisplayName: String
	val challengedDisplayName: String
	val ruleCode: String
	val teamSize: Int
	val status: ChallengeStatus
	val cancellationReason: ChallengeCancellationReason?
	val revision: Long
	val expiresAt: Instant
	val resolvedAt: Instant?
	val createdAt: Instant
}
