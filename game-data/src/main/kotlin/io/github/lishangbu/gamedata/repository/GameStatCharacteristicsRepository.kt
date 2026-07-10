package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameStatCharacteristics
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 数值项特征持久化访问。
 */
interface GameStatCharacteristicsRepository : KRepository<GameStatCharacteristics, Long>
