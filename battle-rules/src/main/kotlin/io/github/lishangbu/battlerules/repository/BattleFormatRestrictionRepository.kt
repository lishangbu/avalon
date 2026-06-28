package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFormatRestriction
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗赛制限制资料的 Jimmer Repository。
 */
interface BattleFormatRestrictionRepository : KRepository<BattleFormatRestriction, Long>
