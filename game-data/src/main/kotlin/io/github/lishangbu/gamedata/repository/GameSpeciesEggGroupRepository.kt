package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesEggGroup
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类分组绑定持久化访问。
 */
interface GameSpeciesEggGroupRepository : KRepository<GameSpeciesEggGroup, Long>
