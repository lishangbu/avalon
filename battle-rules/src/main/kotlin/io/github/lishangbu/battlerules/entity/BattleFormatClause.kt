package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 可复用的赛制条款。
 *
 * 条款表达“唯一种类”“唯一道具”“队伍预览”这类可在多个赛制之间复用的规则开关。
 * 它本身不决定在哪个赛制生效，实际启用关系由 [BattleFormatClauseBinding] 维护。
 */
@Entity
@Table(name = "battle_format_clause")
interface BattleFormatClause {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val clauseType: String
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
