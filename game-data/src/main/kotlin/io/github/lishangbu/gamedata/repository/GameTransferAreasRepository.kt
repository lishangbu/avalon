package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameTransferAreas
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 迁移区域持久化访问。
 */
interface GameTransferAreasRepository : KRepository<GameTransferAreas, Long>
