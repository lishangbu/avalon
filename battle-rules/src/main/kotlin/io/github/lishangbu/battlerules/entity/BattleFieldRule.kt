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
 * 场上持续效果规则定义。
 *
 * 场上效果包括一方场地上的墙和陷阱，也包括全场空间类效果。`effectScope` 用于区分作用域，
 * `maxLayers` 用于表达可叠加陷阱类效果，避免用自由文本承载结构化规则。
 */
@Entity
@Table(name = "battle_field_rule")
interface BattleFieldRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val effectScope: String
	val effectPolicy: String
	val minTurns: Int?
	val maxTurns: Int?
	val maxLayers: Int?
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
