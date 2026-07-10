package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItem
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具资料持久化访问。
 */
interface GameItemRepository : KRepository<GameItem, Long>
