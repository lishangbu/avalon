package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleHpDerivedDamage
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleProportionalDamage
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 不进入普通伤害公式的直接伤害数值解析器。
 *
 * 直接伤害技能已经在主状态机中完成行动可用性、PP、保护、命中、属性吸收和属性免疫判定；本类只回答“这个技能
 * 是否有直接伤害模型，以及本次应写入多少伤害”。它不修改 [io.github.lishangbu.battleengine.model.BattleState]，
 * 不写事件，也不处理替身、保命、低体力道具或倒下。这样直接伤害的资料解释和后续 HP 写入阶段保持解耦：资料模型
 * 变更时改这里，现代战斗阶段顺序变更时仍由 [BattleEngine] 编排。
 */
internal class BattleDirectDamage(
	private val receivedDamageMemory: BattleReceivedDamageMemory,
) {
	/**
	 * 计算本次直接伤害尝试。
	 *
	 * 固定伤害读取技能规则给出的固定值或使用者等级；比例伤害读取目标当前 HP 并按资料声明的分数向下取整；
	 * HP 派生伤害读取双方当前 HP，并可能声明技能失败或使用者在命中后倒下；已受伤害反打读取本回合事件流中
	 * 最后一段合格 HP 伤害；一击必杀在命中后读取目标当前 HP 作为本次直接伤害。返回 null 表示该技能没有直接
	 * 伤害模型，调用方应继续走普通伤害公式。
	 */
	fun attempt(
		state: BattleState,
		skill: BattleSkillSlot,
		actor: BattleParticipant,
		target: BattleParticipant,
	): BattleDirectDamageAttempt? =
		if (skill.receivedDamage != null) {
			receivedDamageMemory.latestReceivedDamage(state, actor.actorId, skill, target.actorId)
				?.let { BattleDirectDamageAttempt.Hit(it.amount) }
				?: BattleDirectDamageAttempt.Failed("received-damage-memory-unavailable")
		} else if (skill.oneHitKnockOut != null) {
			BattleDirectDamageAttempt.Hit(target.currentHp)
		} else {
			when (val fixedDamage = skill.fixedDamage) {
				is BattleFixedDamage.FixedAmount -> BattleDirectDamageAttempt.Hit(fixedDamage.amount)
				BattleFixedDamage.UserLevel -> BattleDirectDamageAttempt.Hit(actor.level)
				null -> proportionalOrHpDerivedDamage(skill, actor, target)
			}
		}

	/**
	 * 解析比例伤害和 HP 派生伤害。
	 *
	 * 这两类规则都需要读取当前 HP，但语义不同：比例伤害只看目标当前 HP，HP 派生伤害可能比较双方 HP 或把使用者
	 * 当前 HP 作为伤害与自损来源。拆成私有函数可以让公开入口的固定伤害分支保持直观。
	 */
	private fun proportionalOrHpDerivedDamage(
		skill: BattleSkillSlot,
		actor: BattleParticipant,
		target: BattleParticipant,
	): BattleDirectDamageAttempt? =
		when (val proportionalDamage = skill.proportionalDamage) {
			is BattleProportionalDamage.TargetCurrentHpFraction ->
				BattleDirectDamageAttempt.Hit(
					fractionAmount(
						value = target.currentHp,
						numerator = proportionalDamage.numerator,
						denominator = proportionalDamage.denominator,
					).coerceAtLeast(proportionalDamage.minimumDamage),
				)
			null -> hpDerivedDamage(skill, actor, target)
		}

	/**
	 * 解析按双方当前 HP 推导的直接伤害。
	 *
	 * “目标当前 HP 减使用者当前 HP”在差值不为正时视为技能失败，调用方会写入专用失败事件并中断锁招；“使用者当前
	 * HP 且使用者倒下”会把自损事实一起返回，后续 HP 写入阶段再统一处理使用者倒下、目标倒下和胜负判定。
	 */
	private fun hpDerivedDamage(
		skill: BattleSkillSlot,
		actor: BattleParticipant,
		target: BattleParticipant,
	): BattleDirectDamageAttempt? =
		when (skill.hpDerivedDamage) {
			BattleHpDerivedDamage.TargetCurrentHpMinusUserCurrentHp -> {
				val amount = target.currentHp - actor.currentHp
				if (amount <= 0) {
					BattleDirectDamageAttempt.Failed("target-hp-not-greater-than-user-hp")
				} else {
					BattleDirectDamageAttempt.Hit(amount)
				}
			}
			BattleHpDerivedDamage.UserCurrentHpAndUserFaints ->
				BattleDirectDamageAttempt.Hit(
					amount = actor.currentHp,
					faintActorAfterHit = true,
				)
			null -> null
		}
}

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
