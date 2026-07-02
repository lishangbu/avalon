package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain

/**
 * 单目标技能结算前的阻止与吸收规则。
 *
 * 本类只处理“技能已经宣告使用、已经选中某个实际目标，但尚未进入普通命中/伤害/附加效果之前”的防守侧规则。
 * 这些规则包括粉末类技能的属性免疫、一击必杀类技能的专用属性免疫、由特性提升优先度的变化技能被恶属性免疫、
 * 精神场地阻止先制技能、目标侧特性阻止先制/声音技能，以及指定属性技能被目标特性吸收。它不处理保护、普通
 * 命中率、属性克制无效或伤害公式；那些阶段仍留在 [BattleEngine.resolveSkillAgainstTarget] 的显式顺序里。
 *
 * 将这些规则集中起来有两个目的：
 * - 让主引擎的单目标流程更短，只负责按现代规则顺序调用各个阻止点。
 * - 让“是否无视目标特性”这一共享判断只由主引擎提供一次，避免先制免疫、声音免疫和吸收特性各自复制目标侧关系。
 *
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性。
 */
internal class BattleSkillBlockEffects(
	private val skillIgnoresTargetAbilityEffects: (BattleState, BattleParticipant, BattleParticipant) -> Boolean,
) {
	/**
	 * 判断技能是否被场地规则阻挡。
	 *
	 * 现代精神场地会保护接地成员免受对手先制技能影响。该判断按目标逐个执行：范围技能中某个目标被阻挡时，
	 * 其它不满足条件的目标仍可继续结算。函数只返回布尔值，具体阻止事件由主流程按当前阶段追加。
	 */
	fun skillBlockedByTerrain(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		priorityContext: SkillPriorityContext,
	): Boolean =
		state.environment.terrain == BattleTerrain.PSYCHIC &&
			priorityContext.effectivePriority > 0 &&
			target.grounded &&
			state.sideOf(actor.actorId)?.sideId != state.sideOf(target.actorId)?.sideId

	/**
	 * 返回阻止本次先制技能影响目标侧的特性拥有者。
	 *
	 * 这类特性保护拥有者所在一侧的当前上场成员；同侧成员主动对自己或伙伴使用先制技能时不触发。返回具体拥有者
	 * 是为了让事件流在双打中能区分“目标自身阻挡”和“伙伴特性保护”。若攻击方本次无视目标特性，则直接返回 null。
	 */
	fun priorityMoveAbilityBlocker(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		priorityContext: SkillPriorityContext,
	): BattleParticipant? {
		if (priorityContext.effectivePriority <= 0) {
			return null
		}
		val actorSide = state.sideOf(actor.actorId) ?: return null
		val targetSide = state.sideOf(target.actorId) ?: return null
		if (actorSide.sideId == targetSide.sideId) {
			return null
		}
		if (skillIgnoresTargetAbilityEffects(state, actor, target)) {
			return null
		}
		return targetSide.activeParticipants()
			.firstOrNull { participant ->
				val effect = participant.abilityEffects
					.filterIsInstance<BattleAbilityEffect.PriorityMoveImmunityForSide>()
					.firstOrNull() ?: return@firstOrNull false
				participant.canBattle() && (participant.actorId == target.actorId || effect.protectsAllies)
			}
	}

	/**
	 * 返回阻止本次声音类技能影响目标的特性拥有者。
	 *
	 * 声音免疫是目标自身特性，不保护伙伴；只要技能来源不是目标本人，且技能槽声明为声音类，就会在命中、伤害
	 * 和附加效果之前阻止本次影响。若攻击方本次技能无视目标特性，则该免疫被跳过。
	 */
	fun soundBasedSkillAbilityBlocker(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleParticipant? {
		if (!skill.soundBased || actor.actorId == target.actorId) {
			return null
		}
		if (skillIgnoresTargetAbilityEffects(state, actor, target)) {
			return null
		}
		return target.takeIf { participant ->
			participant.abilityEffects.any { it is BattleAbilityEffect.SoundBasedSkillImmunity }
		}
	}

	/**
	 * 判断粉末类技能是否被目标属性免疫。
	 *
	 * 现代规则中，草属性成员天然免疫粉末/孢子类技能。这里返回实际触发免疫的属性 ID，便于事件流记录；如果规则
	 * 快照缺少草属性 ID，则不猜测资料编号，也不启用该免疫。
	 */
	fun powderBlockedElementId(state: BattleState, target: BattleParticipant, skill: BattleSkillSlot): Long? {
		val grassElementId = state.rules.elementId("grass") ?: return null
		return if (skill.powderBased && target.hasElement(grassElementId)) {
			grassElementId
		} else {
			null
		}
	}

	/**
	 * 判断一击必杀技能是否被目标当前属性天然免疫。
	 *
	 * 大多数一击必杀技能只依赖普通属性相性无效，例如普通属性技能打不到幽灵属性、地面属性技能打不到飞行属性；
	 * 那些仍由后续属性相性阶段统一处理。这里专门承载现代规则里的额外“同属性目标无效”例外：资料模型只声明
	 * [io.github.lishangbu.battleengine.model.BattleOneHitKnockOut.blocksSameElementTarget]，纯引擎用本次实际技能属性
	 * 与目标属性集合判断，不需要知道具体技能名称或资料库 ID。
	 */
	fun oneHitKnockOutBlockedElementId(state: BattleState, target: BattleParticipant, skill: BattleSkillSlot): Long? {
		val oneHitKnockOut = skill.oneHitKnockOut ?: return null
		val skillElementId = skill.effectiveElementId(state.environment.weather)
		return if (oneHitKnockOut.blocksSameElementTarget && target.hasElement(skillElementId)) {
			skillElementId
		} else {
			null
		}
	}

	/**
	 * 判断由特性提升优先度的对手变化技能是否被目标恶属性免疫。
	 *
	 * 该免疫只绑定“特性把变化技能提升为先制”这一事实；普通基础先制度的变化技能、未被特性提升的技能以及同侧
	 * 辅助技能都不会触发。这里返回恶属性 ID，便于复用属性阻挡事件并保持事件流不依赖本地化属性名称。
	 */
	fun darkPriorityBlockedElementId(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		priorityContext: SkillPriorityContext,
	): Long? {
		val darkElementId = state.rules.elementId("dark") ?: return null
		if (!priorityContext.statusPriorityBoostedByAbility || !priorityContext.darkElementTargetsImmune) {
			return null
		}
		val actorSide = state.sideOf(actor.actorId) ?: return null
		val targetSide = state.sideOf(target.actorId) ?: return null
		return if (actorSide.sideId != targetSide.sideId && target.hasElement(darkElementId)) {
			darkElementId
		} else {
			null
		}
	}

	/**
	 * 应用目标特性对指定属性技能的吸收回复。
	 *
	 * 这类特性发生在技能通过保护和命中判定之后，但早于普通伤害、状态和能力阶级效果写入。满 HP 或被回复封锁的
	 * 目标仍然会吸收并阻止技能继续结算，只是事件中的实际回复量为 0；这样 replay 能区分“未触发”和“触发但
	 * 无需回复/不能回复”。
	 */
	fun elementSkillAbsorbHeal(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState? {
		val effect = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbHeal>()
			.firstOrNull { it.elementId == skill.effectiveElementId(state.environment.weather) }
			?: return null
		if (target.healingBlocked()) {
			return state.appendEvent(
				BattleEvent.SkillAbsorbedByAbility(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					abilityHolderActorId = target.actorId,
					abilityId = target.abilityId,
					elementId = effect.elementId,
					healAmount = 0,
				),
			)
		}
		val rawHealAmount = (target.maxHp / effect.healDenominator).coerceAtLeast(1)
		val healedTarget = target.heal(rawHealAmount)
		val actualHealAmount = healedTarget.currentHp - target.currentHp
		return state
			.replaceParticipant(healedTarget)
			.appendEvent(
				BattleEvent.SkillAbsorbedByAbility(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					abilityHolderActorId = target.actorId,
					abilityId = target.abilityId,
					elementId = effect.elementId,
					healAmount = actualHealAmount,
				),
			)
	}

	/**
	 * 应用目标特性对指定属性技能的吸收和自身能力阶级提升。
	 *
	 * 技能被吸收后不会继续进入普通伤害或附加效果流程。能力阶级提升独立夹取，目标已经达到上限时只记录吸收
	 * 事件；没有阶级变化事件表示“触发了吸收，但提阶被上限吃掉”，而不是技能继续生效。
	 */
	fun elementSkillAbsorbStatStage(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
	): BattleState? {
		val effect = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ElementSkillAbsorbStatStage>()
			.firstOrNull { it.elementId == skill.effectiveElementId(state.environment.weather) }
			?: return null
		val absorbedState = state.appendEvent(
			BattleEvent.SkillAbsorbedByAbility(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				abilityHolderActorId = target.actorId,
				abilityId = target.abilityId,
				elementId = effect.elementId,
				healAmount = 0,
			),
		)
		val beforeStage = target.statStage(effect.stat)
		val updatedTarget = target.changeStatStage(effect.stat, effect.stageDelta)
		val afterStage = updatedTarget.statStage(effect.stat)
		return if (beforeStage == afterStage) {
			absorbedState
		} else {
			absorbedState
				.replaceParticipant(updatedTarget)
				.appendEvent(
					BattleEvent.StatStageChanged(
						turnNumber = state.turnNumber,
						actorId = target.actorId,
						targetActorId = target.actorId,
						stat = effect.stat,
						delta = afterStage - beforeStage,
						currentStage = afterStage,
					),
				)
		}
	}
}
