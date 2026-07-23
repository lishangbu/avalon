package io.github.lishangbu.battleengine.model

/**
 * 战斗快照中的成员性别。
 *
 * 性别只承载会影响回合规则的稳定身份，不保存展示文本或资料库 ID。无性别成员显式使用 [GENDERLESS]，避免用
 * `null` 同时表达“无性别”和“调用方漏传”。
 */
enum class BattleGender {
	MALE,
	FEMALE,
	GENDERLESS,
}
