package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleRuleFixtureSource
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗规则 Fixture 公开来源的 Jimmer Repository。
 */
interface BattleRuleFixtureSourceRepository : KRepository<BattleRuleFixtureSource, Long>
