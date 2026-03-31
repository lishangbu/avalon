package io.github.lishangbu.avalon.game.battle.engine.model

/**
 * 战斗单位运行时状态骨架。
 *
 * 设计意图：
 * - 表示单个战斗单位的当前局内状态。
 * - 与静态物种模板、技能数据、effect 定义解耦。
 *
 * @property id 单位唯一标识。
 * @property currentHp 当前生命值。
 * @property maxHp 最大生命值。
 * @property statusId 主状态标识，未附加时为空。
 * @property abilityId 当前特性标识。
 * @property itemId 当前道具标识。
 * @property typeIds 当前属性列表。
 * @property volatileIds 当前挥发状态标识集合。
 * @property conditionIds 当前附着条件标识集合。
 * @property boosts 当前 stage / boost 表。
 * @property stats 当前运行时属性值表。
 * @property movePp 当前招式剩余 PP。
 * @property flags 轻量字符串标记表。
 * @property forceSwitchRequested 当前单位是否被标记为强制替换。
 */
data class UnitState(
    val id: String,
    val currentHp: Int,
    val maxHp: Int,
    val statusId: String? = null,
    val abilityId: String? = null,
    val itemId: String? = null,
    val typeIds: Set<String> = emptySet(),
    val volatileIds: Set<String> = emptySet(),
    val conditionIds: Set<String> = emptySet(),
    val boosts: Map<String, Int> = emptyMap(),
    val stats: Map<String, Int> = emptyMap(),
    val movePp: Map<String, Int> = emptyMap(),
    val flags: Map<String, String> = emptyMap(),
    val forceSwitchRequested: Boolean = false,
)
