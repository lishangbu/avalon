package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFormat
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗赛制资料的 Jimmer Repository。
 */
interface BattleFormatRepository : KRepository<BattleFormat, Long>
