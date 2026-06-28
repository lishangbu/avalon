package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFormatClauseBinding
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗赛制条款绑定资料的 Jimmer Repository。
 */
interface BattleFormatClauseBindingRepository : KRepository<BattleFormatClauseBinding, Long>
