package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameItemGameIndices
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 道具索引持久化访问。
 */
interface GameItemGameIndicesRepository : KRepository<GameItemGameIndices, Long>
