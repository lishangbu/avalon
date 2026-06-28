package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗技能规则资料的 Jimmer Repository。
 */
interface BattleSkillRuleRepository : KRepository<BattleSkillRule, Long>
