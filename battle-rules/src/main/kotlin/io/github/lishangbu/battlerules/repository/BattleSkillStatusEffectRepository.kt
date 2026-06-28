package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillStatusEffect
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能状态附加效果资料的 Jimmer Repository。
 */
interface BattleSkillStatusEffectRepository : KRepository<BattleSkillStatusEffect, Long>
