package io.github.lishangbu.gamedata.repository

import io.github.lishangbu.gamedata.entity.GameEncounterConditions
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 遭遇条件持久化访问。
 */
interface GameEncounterConditionsRepository : KRepository<GameEncounterConditions, Long>
