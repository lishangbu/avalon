package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillWeatherPowerModifier
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能天气威力倍率资料的 Jimmer Repository。
 */
interface BattleSkillWeatherPowerModifierRepository : KRepository<BattleSkillWeatherPowerModifier, Long>
