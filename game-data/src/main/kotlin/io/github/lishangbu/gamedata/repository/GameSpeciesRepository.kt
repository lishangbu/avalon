package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpecies
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类资料持久化访问。
 */
interface GameSpeciesRepository : KRepository<GameSpecies, Long>
