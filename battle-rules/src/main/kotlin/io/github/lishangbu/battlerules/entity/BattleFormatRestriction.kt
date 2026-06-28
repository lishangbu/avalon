package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 赛制上的可执行限制。
 *
 * 限制描述等级上限、队伍人数、禁用资料 code 等赛制约束。第一版用结构化操作数表达规则，
 * 不保存原始 JSON；战斗引擎后续会把 `restrictionType` 和 `restrictionOperator` 映射到明确校验器。
 */
@Entity
@Table(name = "battle_format_restriction")
interface BattleFormatRestriction {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val formatId: Long

	@Key
	val code: String

	val name: String
	val restrictionType: String
	val restrictionOperator: String
	val operandText: String?
	val operandNumber: Int?
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
