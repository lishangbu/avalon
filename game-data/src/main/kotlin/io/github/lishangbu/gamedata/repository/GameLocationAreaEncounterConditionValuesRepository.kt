package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameLocationAreaEncounterConditionValues
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 区域遭遇条件绑定持久化访问。
 */
interface GameLocationAreaEncounterConditionValuesRepository : KRepository<GameLocationAreaEncounterConditionValues, Long>
