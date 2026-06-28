package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleFieldRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗场上效果规则资料的 Jimmer Repository。
 */
interface BattleFieldRuleRepository : KRepository<BattleFieldRule, Long>
