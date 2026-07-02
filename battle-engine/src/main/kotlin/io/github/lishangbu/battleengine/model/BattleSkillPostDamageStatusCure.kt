package io.github.lishangbu.battleengine.model

/**
 * 技能造成实际伤害后清除目标主要异常的规则。
 *
 * 这类规则只在目标本体实际损失 HP 后触发；未命中、保护、属性无效、替身承受伤害或目标没有对应主要异常时都不会
 * 追加状态解除事件。模型只保存可清除的主要异常集合，不保存技能名称，方便清醒、唤醒巴掌、泡影的咏叹调等技能
 * 共用同一条伤害后状态收口。
 *
 * 与道具治愈、主动变化技能治愈分开建模，是因为这里的触发点绑定在“伤害已经写入目标 HP 后”。如果把它塞进普通
 * 状态附加或 HP 效果里，就很容易在替身、低体力道具、接触特性和倒下判定之间放错顺序。
 */
data class BattleSkillPostDamageStatusCure(
	val statuses: Set<BattleMajorStatus>,
) {
	init {
		require(statuses.isNotEmpty()) { "statuses must not be empty" }
	}
}
