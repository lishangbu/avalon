package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 赛制和条款之间的绑定。
 *
 * 该表把可复用条款挂到具体赛制上，并标记条款是否允许在自定义赛制中关闭。
 * 绑定关系有独立主键，是因为管理端需要把它作为独立资料维护和删除。
 */
@Entity
@Table(name = "battle_format_clause_binding")
interface BattleFormatClauseBinding {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val formatId: Long

	@Key
	val clauseId: Long

	val required: Boolean
	val sortOrder: Int
}
