package io.github.lishangbu.match.game

import com.fasterxml.jackson.annotation.JsonInclude
import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import java.time.Instant

/** Match History 列表项；结果始终按查询 Trainer 的视角表达。 */
@Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
interface MatchHistoryResponse {
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	val opponentDisplayName: String
	val status: MatchStatus
	val result: String?
	val interruptionReason: MatchInterruptionReason?
	val startedAt: Instant?
	val endedAt: Instant?
	val turnNumber: Int
}
