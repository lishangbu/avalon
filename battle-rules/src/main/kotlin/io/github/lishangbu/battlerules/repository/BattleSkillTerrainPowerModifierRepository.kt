package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillTerrainPowerModifier
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能场地威力倍率资料的 Jimmer Repository。
 */
interface BattleSkillTerrainPowerModifierRepository : KRepository<BattleSkillTerrainPowerModifier, Long>
