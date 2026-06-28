package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSpecialMechanic
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗特殊机制资料的 Jimmer Repository。
 */
interface BattleSpecialMechanicRepository : KRepository<BattleSpecialMechanic, Long>
