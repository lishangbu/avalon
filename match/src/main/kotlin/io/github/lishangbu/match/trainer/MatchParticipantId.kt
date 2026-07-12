package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable

/** Match Participant 的复合标识，确保同一 Trainer 在同一 Match 中只出现一次。 */
@Embeddable
interface MatchParticipantId {
	@Column(name = "match_id")
	/** 所属 Match。 */
	val matchId: Long
	@Column(name = "trainer_id")
	/** 参与 Match 的 Trainer。 */
	val trainerId: Long
}
