package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Column
import org.babyfish.jimmer.sql.Embeddable

/** Team Member 与技能槽位组成的复合标识。 */
@Embeddable
interface MatchTrainerTeamMemberSkillId {
	@Column(name = "team_member_id")
	val teamMemberId: Long
	/** 从 1 开始的技能位置。 */
	val position: Int
}
