package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameRegions
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 地区资料持久化访问。
 */
interface GameRegionsRepository : KRepository<GameRegions, Long>
