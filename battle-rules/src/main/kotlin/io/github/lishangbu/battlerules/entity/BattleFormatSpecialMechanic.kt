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
 * 赛制和特殊机制之间的绑定。
 *
 * 该表允许同一个特殊机制在不同赛制下分别启用或关闭，后续战斗创建时会读取这些绑定决定规则包能力。
 */
@Entity
@Table(name = "battle_format_special_mechanic")
interface BattleFormatSpecialMechanic {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val formatId: Long

	@Key
	val mechanicId: Long

	val enabled: Boolean
	val sortOrder: Int
}
