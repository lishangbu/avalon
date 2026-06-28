package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 可由赛制开关控制的特殊机制。
 *
 * 代码标识符保持中性命名，避免把来源品牌术语写入表名、类名或常量名。
 * 如果机制名称来自资料文本，展示文案可以保留必要的中文叫法。
 */
@Entity
@Table(name = "battle_special_mechanic")
interface BattleSpecialMechanic {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
