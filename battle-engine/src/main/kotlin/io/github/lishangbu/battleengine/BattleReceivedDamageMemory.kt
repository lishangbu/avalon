package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import kotlin.math.floor

/**
 * 从事件流读取“本回合已经承受的伤害”。
 *
 * 反打类技能需要知道三个事实：使用者本回合是否被对手直接打掉过 HP、最后一次合格伤害是谁造成的、应该返还多少
 * 伤害。这里不把这些事实缓存进 [BattleState]，而是倒序读取已经追加的 [BattleEvent.DamageApplied]：
 * - [BattleState.events] 是 replay 的权威事实流，按事件流读取可以保证复算和实时结算一致。
 * - 只接受当前回合、目标是使用者、伤害量大于 0 的事件，替身伤害、天气异常伤害和属性免疫 0 伤害都不会误触发。
 * - 通过伤害来源当前技能槽读取伤害类别，避免把资料 ID、技能名称或本地化文本写进纯引擎。
 *
 * 该类只读状态，不追加事件，也不决定命中、保护或属性免疫。目标解析阶段用它找到真实反打目标；直接伤害阶段
 * 再用同一逻辑计算数值，保持两个阶段的判断口径完全一致。
 */
internal class BattleReceivedDamageMemory {
	/**
	 * 查找指定使用者本回合最后一次合格直接受伤记录。
	 *
	 * `targetActorId` 为空时只按规则寻找最新来源，用于命中前把技能目标重定向到真实攻击者；传入具体目标时额外
	 * 要求最后合格事件的来源就是该目标，用于直接伤害阶段防止外层目标和事件来源发生偏移。若来源已经离场、
	 * 倒下、不是对手，或来源技能已经无法确认伤害类别，则返回 null，让调用方按技能失败处理。
	 */
	fun latestReceivedDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		targetActorId: String? = null,
	): BattleReceivedDamageHit? {
		val receivedDamage = skill.receivedDamage ?: return null
		val actorSideId = state.sideOf(actorId)?.sideId ?: return null
		return state.events.asReversed()
			.filterIsInstance<BattleEvent.DamageApplied>()
			.firstNotNullOfOrNull { event ->
				if (event.turnNumber != state.turnNumber ||
					event.targetActorId != actorId ||
					event.amount <= 0 ||
					(targetActorId != null && event.actorId != targetActorId)
				) {
					return@firstNotNullOfOrNull null
				}
				val sourceSide = state.sideOf(event.actorId) ?: return@firstNotNullOfOrNull null
				if (sourceSide.sideId == actorSideId) {
					return@firstNotNullOfOrNull null
				}
				val source = state.participant(event.actorId)
					?.takeIf { state.isActive(it.actorId) && it.canBattle() }
					?: return@firstNotNullOfOrNull null
				val sourceSkill = source.skillSlot(event.skillId) ?: return@firstNotNullOfOrNull null
				if (sourceSkill.damageClass !in receivedDamage.acceptedDamageClasses) {
					return@firstNotNullOfOrNull null
				}
				BattleReceivedDamageHit(
					source = source,
					amount = floor(event.amount * receivedDamage.numerator.toDouble() / receivedDamage.denominator)
						.toInt()
						.coerceAtLeast(1),
				)
			}
	}
}
