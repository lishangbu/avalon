package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameSpeciesColor
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 种类颜色持久化访问。
 */
interface GameSpeciesColorRepository : KRepository<GameSpeciesColor, Long>
