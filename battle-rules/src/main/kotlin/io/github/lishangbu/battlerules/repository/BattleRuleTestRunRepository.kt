package io.github.lishangbu.battlerules.repository

import io.github.lishangbu.battlerules.entity.BattleRuleTestRun
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 战斗规则 Fixture 测试运行结果的 Jimmer Repository。
 */
interface BattleRuleTestRunRepository : KRepository<BattleRuleTestRun, Long>
