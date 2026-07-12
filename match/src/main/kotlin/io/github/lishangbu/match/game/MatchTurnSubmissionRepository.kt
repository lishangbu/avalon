package io.github.lishangbu.match.game

import org.babyfish.jimmer.spring.repository.KRepository

/** Match Turn Submission 的 Jimmer 写入边界。 */
interface MatchTurnSubmissionRepository : KRepository<MatchTurnSubmission, MatchTurnSubmissionId>
