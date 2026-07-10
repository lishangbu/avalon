package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocations
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 地点资料持久化访问。
 */
interface GameLocationsRepository : KRepository<GameLocations, Long>
