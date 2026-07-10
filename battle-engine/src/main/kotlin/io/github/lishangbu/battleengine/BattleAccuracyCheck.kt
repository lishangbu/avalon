package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleMode
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 命中判定结果。
 *
 * [roll] 只在真正消费命中随机数时存在；必中、天气必中或修正命中率达到 100 的场景都返回 null。事件层在未命中时
 * 会把 null 兜底成 0，但 resolver 保留 null，可以让测试精确区分“没有掷骰”和“掷出 0”这类不可能结果。
 */
internal data class BattleAccuracyCheck(
	val hit: Boolean,
	val roll: Int?,
)
