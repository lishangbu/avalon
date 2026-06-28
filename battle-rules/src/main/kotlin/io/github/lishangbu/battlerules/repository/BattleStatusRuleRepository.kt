package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleStatusRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗状态规则资料的 Jimmer Repository。
 */
interface BattleStatusRuleRepository : KRepository<BattleStatusRule, Long>
