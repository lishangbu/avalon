package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFormatClause
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗赛制条款资料的 Jimmer Repository。
 */
interface BattleFormatClauseRepository : KRepository<BattleFormatClause, Long>
