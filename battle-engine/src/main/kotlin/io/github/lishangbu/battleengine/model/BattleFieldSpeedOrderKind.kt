package io.github.lishangbu.battleengine.model

/**
 * 全场速度顺序效果种类。
 *
 * `reversesSpeedOrder` 明确表达排序方向，避免行动队列直接识别技能 ID 或资料表 effectPolicy。
 */
enum class BattleFieldSpeedOrderKind(
	val reversesSpeedOrder: Boolean,
) {
	TRICK_ROOM(reversesSpeedOrder = true),
}
