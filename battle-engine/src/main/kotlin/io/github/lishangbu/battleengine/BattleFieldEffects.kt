package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideProtectionApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 负责技能命中后建立一侧场上状态和全场速度顺序效果。
 *
 * 这些效果都不是即时伤害或异常状态，而是把“之后会被其它阶段读取”的结构化状态写入 [BattleState]：防守屏障
 * 供伤害公式读取，速度修正供行动排序读取，入场陷阱供换入流程读取，全场速度顺序供速度比较器读取。拆出后
 * [BattleEngine] 主流程只保留触发顺序和概率判定，本类专注于目标侧解析、持续回合延长、去重和事件生成。
 */
internal class BattleFieldEffects {
	/**
	 * 将命中后的技能一侧防守屏障写入对应战斗侧。
	 *
	 * 目标侧解析在这里完成：使用者侧屏障不依赖本次目标是否仍可战斗；目标侧屏障跟随实际命中的目标所属侧。
	 * 若技能资料声明了天气前置条件但当前天气不匹配，技能本身已经完成使用和 PP 阶段，此时应记录失败事件，而不是
	 * 静默跳过。这个分支主要覆盖极光类屏障：它不是“附加效果没触发”，而是公开规则中的明确失败条件。
	 * 若同种屏障已存在，状态保持不变且不产生开始事件，但要追加稳定的技能失败事件。这里刻意把“没有刷新”
	 * 和“没有命中/没有使用”区分开：回放端仍能看到技能确实被使用过，裁判或排障日志也能解释为什么状态没有变化。
	 * 携带者若有匹配屏障种类的持续时间延长道具，则只在首次成功建立屏障时改写即将写入的完整持续回合。
	 */
	fun applySideCondition(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideConditionApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reason = "side-condition-required-weather-unmet",
				),
			)
		}
		val side = sideFor(state, actorId, targetActorId, application.targetSide) ?: return state
		val damageReduction = extendedSideDamageReduction(state, actorId, application.damageReduction)
		val changed = state.addSideDamageReduction(side.sideId, damageReduction)
			?: return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reason = "side-damage-reduction-already-active",
				),
			)
		return changed.appendEvent(
			BattleEvent.SideDamageReductionStarted(
				turnNumber = state.turnNumber,
				actorId = actorId,
				sideId = side.sideId,
				skillId = skill.skillId,
				kind = damageReduction.kind,
				turnsRemaining = damageReduction.turnsRemaining,
			),
		)
	}

	/**
	 * 将命中后的技能一侧速度修正写入对应战斗侧。
	 *
	 * 速度修正影响下一次行动排序，不影响当前已经确定的行动顺序；因此这里只写入状态和事件，不重新规划当前回合。
	 * 与防守屏障一样，同种速度修正已存在时由 [BattleState] 拒绝重复写入；本类再把这种拒绝翻译成技能失败事件，
	 * 避免生产回放里只看到 PP 消耗和回合推进，却缺少“为什么没有刷新顺风”的事实。
	 */
	fun applySideSpeedModifier(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideSpeedModifierApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = sideFor(state, actorId, targetActorId, application.targetSide) ?: return state
		val changed = state.addSideSpeedModifier(side.sideId, application.speedModifier)
			?: return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reason = "side-speed-modifier-already-active",
				),
			)
		return changed.appendEvent(
			BattleEvent.SideSpeedModifierStarted(
				turnNumber = state.turnNumber,
				actorId = actorId,
				sideId = side.sideId,
				skillId = skill.skillId,
				kind = application.speedModifier.kind,
				multiplier = application.speedModifier.multiplier,
				turnsRemaining = application.speedModifier.turnsRemaining,
			),
		)
	}

	/**
	 * 将命中后的技能一侧防护写入对应战斗侧。
	 *
	 * 防护效果的生命周期和其它一侧持续状态一致；同种防护已经存在时，技能不会刷新持续回合。这里把这种拒绝写成
	 * 稳定失败事件，方便生产日志解释“技能被使用但场上状态没有变化”的情况。
	 */
	fun applySideProtection(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideProtectionApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = sideFor(state, actorId, targetActorId, application.targetSide) ?: return state
		val changed = state.addSideProtection(side.sideId, application.protection)
			?: return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reason = "side-protection-already-active",
				),
			)
		return changed.appendEvent(
			BattleEvent.SideProtectionStarted(
				turnNumber = state.turnNumber,
				actorId = actorId,
				sideId = side.sideId,
				skillId = skill.skillId,
				kind = application.protection.kind,
				turnsRemaining = application.protection.turnsRemaining,
			),
		)
	}

	/**
	 * 将命中后的技能入场陷阱写入对应战斗侧。
	 *
	 * 入场陷阱只在后续成员换入时触发，因此这里仅负责建立或叠层。若同类陷阱无法再叠层，状态保持不变，不产生
	 * 层数变化事件，并追加稳定失败事件；这样 replay 既不会误读成状态刷新，也不会丢掉“技能已使用但失败”的事实。
	 */
	fun applySideEntryHazard(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		application: BattleSideEntryHazardApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val side = sideFor(state, actorId, targetActorId, application.targetSide) ?: return state
		val change = state.addSideEntryHazard(side.sideId, application.hazard)
			?: return state.appendEvent(
				BattleEvent.SkillFailed(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					skillId = skill.skillId,
					reason = "entry-hazard-already-maxed",
				),
			)
		return change.state.appendEvent(
			BattleEvent.SideEntryHazardChanged(
				turnNumber = state.turnNumber,
				actorId = actorId,
				sideId = side.sideId,
				skillId = skill.skillId,
				kind = change.hazard.kind,
				layers = change.hazard.layers,
				maxLayers = change.hazard.maxLayers,
			),
		)
	}

	/**
	 * 将命中后的全场速度顺序效果写入环境。
	 *
	 * 公开成熟实现中，戏法空间在已经存在时再次使用会解除该全场效果；未存在时建立新的持续效果。若已经存在的
	 * 是其它速度顺序效果，本次保持状态不变，避免两个互斥全场速度规则同时存在。
	 */
	fun applyFieldSpeedOrder(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		application: BattleFieldSpeedOrderApplication,
	): BattleState {
		if (application.requiredWeather != null && state.environment.weather != application.requiredWeather) {
			return state
		}
		val current = state.environment.fieldSpeedOrderEffect
		if (current?.kind == application.speedOrderEffect.kind) {
			return state
				.copy(environment = state.environment.copy(fieldSpeedOrderEffect = null))
				.appendEvent(
					BattleEvent.FieldSpeedOrderEnded(
						turnNumber = state.turnNumber,
						kind = current.kind,
						actorId = actorId,
						skillId = skill.skillId,
					),
				)
		}
		if (current != null) {
			return state
		}
		return state
			.copy(environment = state.environment.copy(fieldSpeedOrderEffect = application.speedOrderEffect))
			.appendEvent(
				BattleEvent.FieldSpeedOrderStarted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					skillId = skill.skillId,
					kind = application.speedOrderEffect.kind,
					turnsRemaining = application.speedOrderEffect.turnsRemaining,
				),
			)
	}

	/**
	 * 计算一侧伤害减免屏障建立时最终写入的持续回合。
	 *
	 * 普通屏障技能会在资料中声明基础持续回合；携带者若拥有匹配屏障种类的延长道具效果，则用道具声明的最长回合
	 * 覆盖基础值。基础持续回合为空时保持为空，避免未来永久屏障规则被普通道具强行改成有限回合。
	 */
	private fun extendedSideDamageReduction(
		state: BattleState,
		actorId: String,
		damageReduction: BattleSideDamageReduction,
	): BattleSideDamageReduction {
		if (damageReduction.turnsRemaining == null) {
			return damageReduction
		}
		val actor = state.participant(actorId) ?: return damageReduction
		val turnsRemaining = actor.itemEffects
			.filterIsInstance<BattleItemEffect.SideDamageReductionDurationExtension>()
			.filter { damageReduction.kind in it.kinds }
			.maxOfOrNull { it.turnsRemaining }
			?: damageReduction.turnsRemaining
		return damageReduction.copy(turnsRemaining = turnsRemaining)
	}

	private fun sideFor(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		targetSide: BattleSideConditionTarget,
	) =
		when (targetSide) {
			BattleSideConditionTarget.USER_SIDE -> state.sideOf(actorId)
			BattleSideConditionTarget.TARGET_SIDE -> state.sideOf(targetActorId)
		}
}
