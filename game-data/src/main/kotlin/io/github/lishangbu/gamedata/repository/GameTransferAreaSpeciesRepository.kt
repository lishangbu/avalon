package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameTransferAreaSpecies
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 迁移区域种类持久化访问。
 */
interface GameTransferAreaSpeciesRepository : KRepository<GameTransferAreaSpecies, Long>
