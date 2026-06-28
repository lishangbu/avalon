package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFormatSpecialMechanic
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗赛制特殊机制绑定资料的 Jimmer Repository。
 */
interface BattleFormatSpecialMechanicRepository : KRepository<BattleFormatSpecialMechanic, Long>
