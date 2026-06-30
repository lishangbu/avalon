package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗规则公开对照 Fixture。
 *
 * Fixture 主表只保存场景元数据、输入摘要和期望摘要；运行结果拆到 `battle_rule_test_run`，
 * 避免把多次运行记录压进一个反范式字段。
 */
@Entity
@Table(name = "battle_rule_fixture")
interface BattleRuleFixture {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val category: String
	val fixtureType: String
	val formatCode: String?
	val description: String?
	val inputSummary: String
	val expectedSummary: String
	val enabled: Boolean
	val sortOrder: Int
}
