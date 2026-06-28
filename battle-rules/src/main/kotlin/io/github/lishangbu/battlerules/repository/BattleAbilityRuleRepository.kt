package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleAbilityRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗特性规则资料的 Jimmer Repository。
 */
interface BattleAbilityRuleRepository : KRepository<BattleAbilityRule, Long>
