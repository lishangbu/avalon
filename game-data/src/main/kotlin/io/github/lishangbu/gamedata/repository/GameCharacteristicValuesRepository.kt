package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameCharacteristicValues
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 个体特征取值持久化访问。
 */
interface GameCharacteristicValuesRepository : KRepository<GameCharacteristicValues, Long>
