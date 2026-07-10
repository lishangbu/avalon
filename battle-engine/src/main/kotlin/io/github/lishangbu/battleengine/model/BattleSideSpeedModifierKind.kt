package io.github.lishangbu.battleengine.model

/**
 * 一侧速度修正种类。
 *
 * 每个枚举值都对应一个已经显式接入引擎的现代规则效果。新增速度类场上效果时应扩展这里并补充公开对照测试，
 * 不应在行动排序中解析资料表字符串。
 */
enum class BattleSideSpeedModifierKind(
	val defaultMultiplier: Double,
) {
	TAILWIND(2.0),
}
