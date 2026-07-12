package io.github.lishangbu.match.game

import io.github.lishangbu.match.trainer.MatchParticipant
import io.github.lishangbu.match.trainer.MatchParticipantId
import org.babyfish.jimmer.spring.repository.KRepository

/** Match Participant 的 Jimmer Repository。 */
interface MatchParticipantRepository : KRepository<MatchParticipant, MatchParticipantId>
