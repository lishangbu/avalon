package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEventStats
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 活动能力项持久化访问。
 */
interface GameEventStatsRepository : KRepository<GameEventStats, Long>
