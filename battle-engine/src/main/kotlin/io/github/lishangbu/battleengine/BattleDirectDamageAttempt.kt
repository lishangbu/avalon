package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 直接伤害解析结果。
 *
 * [Hit] 只说明应该写入多少直接伤害，以及是否在伤害后让使用者倒下；它不代表目标一定会失去等量 HP，因为替身、
 * 满 HP 保命特性/道具和当前剩余 HP 都会在后续阶段继续修正。[Failed] 表示资料规则本身判定技能失败，例如双方
 * 当前 HP 差值不满足要求。
 */
internal sealed interface BattleDirectDamageAttempt {
	/**
	 * 直接伤害规则成功给出了本次应尝试写入的伤害量。
	 *
	 * [amount] 是规则模型计算出的原始直接伤害，后续 HP 写入阶段仍会按目标当前 HP、替身、满 HP 保命来源和倒下
	 * 结算顺序截断或改写实际损失。[faintActorAfterHit] 用于同命类“用使用者当前 HP 造成伤害，并在命中后让使用者
	 * 倒下”的技能；该标记不会在解析阶段立刻修改使用者 HP，因为胜负判定、目标倒下和使用者倒下必须由统一伤害
	 * 应用阶段按现代规则顺序写入事件。
	 */
	data class Hit(
		val amount: Int,
		val faintActorAfterHit: Boolean = false,
	) : BattleDirectDamageAttempt

	/**
	 * 直接伤害规则明确判定本次技能失败。
	 *
	 * 这种失败不是命中失败、保护阻挡或属性免疫，而是技能自身资料条件不满足，例如“目标当前 HP 必须高于使用者
	 * 当前 HP”的差值伤害没有正数差值。返回 [Failed] 后调用方会追加稳定 reason 的技能失败事件，并停止普通伤害
	 * 与附加效果结算，避免把本应失败的技能退回标准伤害公式。
	 */
	data class Failed(
		val reason: String,
	) : BattleDirectDamageAttempt
}
