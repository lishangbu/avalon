package io.github.lishangbu.match.challenge

import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.Immutable
import io.swagger.v3.oas.annotations.media.Schema

/** Challenge API 的稳定错误体。 */
@Immutable
interface ChallengeErrorResponse {
	val code: String
	val message: String
	@JsonConverter(LongToStringConverter::class)
	@get:Schema(type = "string")
	val matchId: Long?
}
