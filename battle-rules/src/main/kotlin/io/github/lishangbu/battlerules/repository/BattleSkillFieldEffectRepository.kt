package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillFieldEffect
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能场上效果资料的 Jimmer Repository。
 */
interface BattleSkillFieldEffectRepository : KRepository<BattleSkillFieldEffect, Long>
