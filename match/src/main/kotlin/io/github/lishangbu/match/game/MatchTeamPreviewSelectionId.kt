package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.Embeddable
import org.babyfish.jimmer.sql.Column

/** Team Preview 秘密选择的复合标识。 */
@Embeddable
interface MatchTeamPreviewSelectionId {
	@Column(name = "match_id")
	val matchId: Long
	@Column(name = "trainer_id")
	val trainerId: Long
}
