package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleWeatherRule
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗天气规则资料的 Jimmer Repository。
 */
interface BattleWeatherRuleRepository : KRepository<BattleWeatherRule, Long>
