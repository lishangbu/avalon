package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameStat
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 数值项持久化访问。
 */
interface GameStatRepository : KRepository<GameStat, Long>
