package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocationAreas
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 地点区域持久化访问。
 */
interface GameLocationAreasRepository : KRepository<GameLocationAreas, Long>
