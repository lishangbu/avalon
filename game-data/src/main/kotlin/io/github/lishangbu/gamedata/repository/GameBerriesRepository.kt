package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameBerries
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 树果资料持久化访问。
 */
interface GameBerriesRepository : KRepository<GameBerries, Long>
