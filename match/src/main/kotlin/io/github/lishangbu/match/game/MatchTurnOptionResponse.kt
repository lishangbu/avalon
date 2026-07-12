package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter

/** 将 Runtime actorId 隐藏为 position 后的可选行动。 */
@Immutable
interface MatchTurnOptionResponse {
	val type: String
	@JsonConverter(LongToStringConverter::class)
	val skillId: Long?
	val targetPosition: Int
	val targetYou: Boolean
}
