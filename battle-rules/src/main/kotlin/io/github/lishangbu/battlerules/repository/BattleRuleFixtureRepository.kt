package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleRuleFixture
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗规则公开对照 Fixture 的 Jimmer Repository。
 */
interface BattleRuleFixtureRepository : KRepository<BattleRuleFixture, Long>
