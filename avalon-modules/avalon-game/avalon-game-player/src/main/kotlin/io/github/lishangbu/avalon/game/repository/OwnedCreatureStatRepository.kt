package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.OwnedCreatureStat
import org.babyfish.jimmer.spring.repository.KRepository

interface OwnedCreatureStatRepository : KRepository<OwnedCreatureStat, Long>
