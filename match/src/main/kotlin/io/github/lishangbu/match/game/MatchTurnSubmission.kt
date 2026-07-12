package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.*
import java.time.Instant

/** 玩家已锁定的回合行动；对方永远不能读取该行内容或接受时间。 */
@Entity
@Table(name = "match_turn_submission")
interface MatchTurnSubmission {
	@Id val id: MatchTurnSubmissionId
	val submissionId: String
	@Serialized val actions: List<MatchTurnAction>
	val acceptedAt: Instant
}
