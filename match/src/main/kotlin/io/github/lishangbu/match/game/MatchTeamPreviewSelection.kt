package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.Instant

/** 一名参与者在 Team Preview 中锁定的秘密首发。 */
@Entity
@Table(name = "match_team_preview_selection")
interface MatchTeamPreviewSelection {
	@Id
	val id: MatchTeamPreviewSelectionId
	val leadPosition: Int
	val selectedAt: Instant
}
