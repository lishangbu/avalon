package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureHeldItems
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵持有道具持久化访问。
 */
interface GameCreatureHeldItemsRepository : KRepository<GameCreatureHeldItems, Long>
