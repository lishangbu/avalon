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
 * 战斗道具规则定义。
 *
 * 道具基础资料由 `game_item` 维护，本表只关注携带道具在现代战斗中的触发时机、效果策略、是否消耗和顺序。
 * 它不会保存背包分类、价格或投掷威力之类资料字段，避免规则表变成道具资料的副本。
 *
 * 后续战斗引擎会根据 `triggerTiming` 聚合当前战斗成员的道具规则，并按 `triggerOrder` 排序执行。
 * `consumable` 只表达触发后是否应消耗，真正扣除携带道具的状态变化仍由引擎事务内的战斗状态机完成。
 */
@Entity
@Table(name = "battle_item_rule")
interface BattleItemRule {
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long

	@Key
	val itemId: Long

	@Key
	val triggerTiming: String

	@Key
	val effectPolicy: String

	val consumable: Boolean
	val triggerOrder: Int
	val description: String?
	val enabled: Boolean
	val sortOrder: Int
}
