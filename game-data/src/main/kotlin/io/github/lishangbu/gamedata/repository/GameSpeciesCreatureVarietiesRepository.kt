package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesCreatureVarieties
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类精灵变种持久化访问。
 */
interface GameSpeciesCreatureVarietiesRepository : KRepository<GameSpeciesCreatureVarieties, Long>
