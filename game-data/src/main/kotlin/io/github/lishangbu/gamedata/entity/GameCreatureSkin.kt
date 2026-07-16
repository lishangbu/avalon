package io.github.lishangbu.gamedata.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.babyfish.jimmer.jackson.JsonConverter
import org.babyfish.jimmer.jackson.LongToStringConverter
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.GeneratedValue
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table

/**
 * 隶属于 Creature 且不改变战斗事实的纯外观资料。
 *
 * 对应 `game_creature_skin` 表；聚合边界只保存稳定资源键，不保存玩家拥有状态或战斗状态。
 * 新增记录的主键统一由 CosId 生成。
 */
@Entity
@Table(name = "game_creature_skin")
interface GameCreatureSkin {
	/** 由 CosId 生成并以字符串输出的皮肤主键。 */
	@Id
	@GeneratedValue(generatorType = CosIdLongUserIdGenerator::class)
	@JsonConverter(LongToStringConverter::class)
	val id: Long
	/** 拥有该皮肤资料的 Content Pack 标识。 */
	val contentPackId: Long
	/** 皮肤所属精灵形态的标识。 */
	val creatureId: Long
	/** 皮肤在队伍快照和资源地址中使用的稳定代码。 */
	val code: String
	/** 中文玩家界面展示的皮肤名称。 */
	val name: String
	/** 玩家目录使用的头像资源键。 */
	val avatarAssetKey: String
	/** 战斗中面向玩家一侧使用的正面资源键。 */
	val frontAssetKey: String
	/** 战斗中玩家己方使用的背面资源键。 */
	val backAssetKey: String
	/** 是否为所属精灵唯一的默认皮肤。 */
	val defaultSkin: Boolean
	/** 是否允许玩家在当前目录中选择。 */
	val enabled: Boolean
	/** 同一精灵下皮肤的稳定展示顺序。 */
	val sortOrder: Int
}
