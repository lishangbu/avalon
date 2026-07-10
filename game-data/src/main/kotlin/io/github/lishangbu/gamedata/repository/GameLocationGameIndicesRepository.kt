package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocationGameIndices
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 地点索引持久化访问。
 */
interface GameLocationGameIndicesRepository : KRepository<GameLocationGameIndices, Long>
