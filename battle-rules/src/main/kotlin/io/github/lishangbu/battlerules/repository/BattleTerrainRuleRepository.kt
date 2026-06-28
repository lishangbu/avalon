package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleTerrainRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗场地规则资料的 Jimmer Repository。
 */
interface BattleTerrainRuleRepository : KRepository<BattleTerrainRule, Long>
