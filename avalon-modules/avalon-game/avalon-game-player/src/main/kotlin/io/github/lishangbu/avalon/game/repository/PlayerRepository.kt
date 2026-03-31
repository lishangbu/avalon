package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.Player
import org.babyfish.jimmer.spring.repository.KRepository

interface PlayerRepository : KRepository<Player, Long>
