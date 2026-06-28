package io.github.lishangbu.battlerules.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.Table

/**
 * 战斗赛制定义。
 *
 * 赛制是战斗引擎读取规则包的入口，描述双方站位、队伍规模、等级拉平和是否允许叠加自定义规则。
 * 这里不保存具体技能、特性或道具效果；那些事实由后续专门规则表维护，避免一个赛制记录承担过多职责。
 */
@Entity
@Table(name = "battle_format")
interface BattleFormat {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	val id: Long

	@Key
	val code: String

	val name: String
	val description: String?
	val battleMode: String
	val playerCount: Int
	val teamSize: Int
	val activeParticipantCount: Int
	val defaultLevel: Int?
	val allowCustomRules: Boolean
	val enabled: Boolean
	val sortOrder: Int
}
