package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameElementGameIndices
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 属性索引持久化访问。
 */
interface GameElementGameIndicesRepository : KRepository<GameElementGameIndices, Long>
