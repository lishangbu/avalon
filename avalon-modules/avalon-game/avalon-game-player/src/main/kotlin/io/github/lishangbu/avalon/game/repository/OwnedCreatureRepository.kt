package io.github.lishangbu.avalon.game.repository

import io.github.lishangbu.avalon.game.entity.OwnedCreature
import org.babyfish.jimmer.spring.repository.KRepository

interface OwnedCreatureRepository : KRepository<OwnedCreature, Long>
