package io.github.lishangbu.match.game

import org.babyfish.jimmer.spring.repository.KRepository

/** Match 聚合根的 Jimmer Repository。 */
interface MatchGameRepository : KRepository<MatchGame, Long>
