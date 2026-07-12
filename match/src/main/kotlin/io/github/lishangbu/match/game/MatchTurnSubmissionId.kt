package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable

/** 同一 Match、回合与 Trainer 只能锁定一份行动。 */
@Embeddable
interface MatchTurnSubmissionId {
	@Column(name = "match_id") val matchId: Long
	@Column(name = "turn_number") val turnNumber: Int
	@Column(name = "trainer_id") val trainerId: Long
}
