package io.github.lishangbu.match.game

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable

/** 每位查看方针对一个对手成员只维护一份累积公开事实。 */
@Embeddable
interface MatchDisclosureLedgerId {
	@Column(name = "match_id") val matchId: Long
	@Column(name = "viewer_trainer_id") val viewerTrainerId: Long
	@Column(name = "opponent_member_position") val opponentMemberPosition: Int
}
