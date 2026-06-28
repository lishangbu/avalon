package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleSkillWeatherAccuracyOverride
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 技能天气命中覆盖资料的 Jimmer Repository。
 */
interface BattleSkillWeatherAccuracyOverrideRepository : KRepository<BattleSkillWeatherAccuracyOverride, Long>
