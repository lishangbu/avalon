package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCreatureGameIndices
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 精灵索引持久化访问。
 */
interface GameCreatureGameIndicesRepository : KRepository<GameCreatureGameIndices, Long>
