package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCharacteristics
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 个体特征持久化访问。
 */
interface GameCharacteristicsRepository : KRepository<GameCharacteristics, Long>
