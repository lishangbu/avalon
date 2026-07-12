package io.github.lishangbu.match.challenge

import io.github.lishangbu.match.trainer.MatchChallenge
import org.babyfish.jimmer.spring.repository.KRepository

/** Challenge 聚合根的 Jimmer Repository。 */
interface MatchChallengeRepository : KRepository<MatchChallenge, Long>
