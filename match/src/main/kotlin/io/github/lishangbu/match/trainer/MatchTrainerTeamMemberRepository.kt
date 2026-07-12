package io.github.lishangbu.match.trainer

import org.babyfish.jimmer.spring.repository.KRepository

/** Team Member 写入仓库。 */
interface MatchTrainerTeamMemberRepository : KRepository<MatchTrainerTeamMember, Long>
