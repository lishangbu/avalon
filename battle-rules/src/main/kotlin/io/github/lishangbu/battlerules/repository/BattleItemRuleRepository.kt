package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleItemRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗道具规则资料的 Jimmer Repository。
 */
interface BattleItemRuleRepository : KRepository<BattleItemRule, Long>
