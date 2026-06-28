package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗规则 Fixture 的公开来源。
 *
 * 每条来源只保存一个稳定 HTTPS 链接和简短说明。业务上允许同一个 Fixture 关联多个公开来源，
 * 因此这里使用独立主键并对 `(fixtureId, sourceUrl)` 做唯一约束。
 */
@Entity
@Table(name = "battle_rule_fixture_source")
interface BattleRuleFixtureSource {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val fixtureId: Long

	@Key
	val sourceUrl: String

	val sourceLabel: String?
	val sourceNote: String?
	val sortOrder: Int
}
