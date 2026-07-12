package io.github.lishangbu.match.game

import io.github.lishangbu.match.trainer.MatchActiveAccountReservation
import org.babyfish.jimmer.spring.repository.KRepository

/** 每账户唯一 Active Match 容量保留的 Jimmer Repository。 */
interface MatchActiveAccountReservationRepository : KRepository<MatchActiveAccountReservation, Long>
