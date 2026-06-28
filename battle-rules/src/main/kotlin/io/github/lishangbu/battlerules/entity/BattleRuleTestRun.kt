package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table
import java.time.OffsetDateTime

/**
 * 战斗规则 Fixture 测试运行结果。
 *
 * 该表保存一次执行的结构化摘要：状态、执行器、命令、提交号、时间和失败摘要。它不保存控制台原文或
 * 任意 JSON，避免把测试运行日志当作不可查询的业务事实。
 */
@Entity
@Table(name = "battle_rule_test_run")
interface BattleRuleTestRun {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val runCode: String

	val fixtureId: Long
	val runStatus: String
	val executor: String
	val command: String?
	val engineCommit: String?
	val startedAt: OffsetDateTime
	val finishedAt: OffsetDateTime?
	val durationMs: Long?
	val assertionCount: Int?
	val failureMessage: String?
	val sortOrder: Int
}
