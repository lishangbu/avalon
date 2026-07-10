package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEncounterConditionValues
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 遭遇条件值持久化访问。
 */
interface GameEncounterConditionValuesRepository : KRepository<GameEncounterConditionValues, Long>
