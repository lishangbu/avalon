package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.OwnedCreatureMove
import org.babyfish.jimmer.spring.repository.KRepository

interface OwnedCreatureMoveRepository : KRepository<OwnedCreatureMove, Long>
