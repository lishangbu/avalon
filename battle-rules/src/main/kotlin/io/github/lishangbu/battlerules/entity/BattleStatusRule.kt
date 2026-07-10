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
 * 状态规则定义。
 *
 * 状态规则覆盖主要异常、临时状态和本回合行动限制。`effectPolicy` 是引擎实现类识别的稳定策略编码，
 * 数据库只保存可维护配置，不把算法细节塞进文本字段。
 */
@Entity
@Table(name = "battle_status_rule")
interface BattleStatusRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val statusKind: String
	val effectPolicy: String
	val minTurns: Int?
	val maxTurns: Int?
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
