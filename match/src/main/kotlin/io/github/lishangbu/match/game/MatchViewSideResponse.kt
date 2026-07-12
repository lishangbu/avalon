package io.github.lishangbu.match.game

import org.babyfish.jimmer.Immutable

/** Match View 中不公开内部 side 编号的一方。 */
@Immutable
interface MatchViewSideResponse {
	val displayName: String
	val you: Boolean
	val participants: List<MatchViewParticipantResponse>
}
