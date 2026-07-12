package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/** Team Member 的有序技能槽位。 */
@Entity
@Table(name = "match_trainer_team_member_skill")
interface MatchTrainerTeamMemberSkill {
	@Id
	val id: MatchTrainerTeamMemberSkillId
	val skillId: Long
}
