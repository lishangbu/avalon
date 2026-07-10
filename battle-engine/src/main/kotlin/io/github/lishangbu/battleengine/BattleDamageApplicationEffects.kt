package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFatalDamageSurvivalSource
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 技能伤害写入与伤害后处理。
 *
 * [BattleEngine] 负责决定“什么时候进入普通伤害、直接伤害或替身伤害”；一旦确定要写 HP，本组件负责把伤害写入
 * [BattleState] 并串起所有共享后续流程。这样主状态机不再混杂 HP 写入细节，同时普通公式伤害、固定/比例直接伤害
 * 和替身伤害仍走同一套事件顺序：
 * - 目标本体伤害先处理满 HP 保命，再追加 [BattleEvent.DamageApplied] 和保命事件。
 * - 替身伤害只修改替身 HP，并追加 [BattleEvent.SubstituteDamageApplied] / [BattleEvent.SubstituteBroken]。
 * - 两条路径最后都回到 [finishPostDamageEffects]，统一处理吸取、反伤、休整、低体力道具、接触特性、倒下和胜负。
 *
 * 本组件不判断命中、保护、属性免疫、伤害公式或目标选择；那些仍属于主流程和各自 resolver 的阶段职责。
 */
internal class BattleDamageApplicationEffects(
	private val damageDefenseEffects: BattleDamageDefenseEffects,
	private val skillHpEffects: BattleSkillHpEffects,
	private val postDamageEffects: BattlePostDamageEffects,
) {
	/**
	 * 向目标本体写入一次技能伤害，并追加标准伤害事件。
	 *
	 * 普通公式伤害和固定/比例/HP 派生直接伤害在“伤害数值怎么来”上不同，但一旦决定要扣目标本体 HP，后续必须共享
	 * 同一套顺序：
	 * - 先让满 HP 保命类特性/道具把原始伤害夹成实际可写入伤害。
	 * - 再写入目标 HP，计算真实扣血量，避免过量伤害污染吸血、贝壳之铃等按实际伤害读取的规则。
	 * - 然后追加 [BattleEvent.DamageApplied]，最后追加保命事件，让 replay 能看到“伤害事实”和“为什么没有倒下”。
	 *
	 * 替身伤害故意不走这里。替身不触发目标低体力道具、接触特性或目标倒下判定，并且事件类型不同；把替身塞进这个
	 * helper 反而会让调用方用布尔参数区分两套语义。
	 */
	fun applyDamageToTarget(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		effectiveness: Double,
		targetMultiplier: Double,
		criticalHit: Boolean = false,
		ignoreTargetAbilityEffects: Boolean,
	): BattleTargetDamageApplication {
		val skillLimitedDamageAmount = damageAmountAfterSkillTargetFloor(target, skill, damageAmount)
		val endure = fatalDamageEndure(
			state = state,
			actor = actor,
			target = target,
			skill = skill,
			damageAmount = skillLimitedDamageAmount,
		)
		val survival = damageDefenseEffects.fatalDamageSurvival(
			state = state,
			actor = actor,
			target = endure.target,
			skill = skill,
			damageAmount = endure.damageAmount,
			ignoreTargetAbilityEffects = ignoreTargetAbilityEffects,
		)
		val damagedTarget = survival.target.receiveDamage(survival.damageAmount)
		val actualDamageAmount = survival.target.currentHp - damagedTarget.currentHp
		val damagedState = state
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = actualDamageAmount,
					effectiveness = effectiveness,
					targetMultiplier = targetMultiplier,
					criticalHit = criticalHit,
				),
			)
			.appendEvents(listOfNotNull(survival.event))
			.appendEvents(listOfNotNull(endure.event))
		return BattleTargetDamageApplication(
			state = damagedState,
			damagedTarget = damagedTarget,
			actualDamageAmount = actualDamageAmount,
		)
	}

	/**
	 * 让目标替身吸收本段伤害。
	 *
	 * 替身受击时目标本体 HP 不变，也不会触发目标低体力道具、接触反制特性或目标倒下判定。造成的替身 HP 损失
	 * 仍作为本次实际伤害传给吸取回复、休整和攻击方道具反伤等“成功造成伤害后”的来源规则，符合公开实现中
	 * 吸取类技能可以从替身伤害中回复的行为。
	 */
	fun resolveDamageAgainstSubstitute(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean = false,
	): BattleState {
		val actualDamageAmount = damageAmount.coerceAtMost(target.substituteHp)
		val damagedTarget = target.damageSubstitute(actualDamageAmount)
		val damagedState = state
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.SubstituteDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = actualDamageAmount,
					substituteHpRemaining = damagedTarget.substituteHp,
				),
			)
			.let { current ->
				if (target.substituteHp > 0 && damagedTarget.substituteHp == 0) {
					current.appendEvent(
						BattleEvent.SubstituteBroken(
							turnNumber = state.turnNumber,
							actorId = target.actorId,
						),
					)
				} else {
					current
				}
			}
		return finishPostDamageEffects(
			state = damagedState,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			damageAmount = actualDamageAmount,
			faintActorAfterHit = faintActorAfterHit,
			targetCanFaint = false,
			allowTargetLowHpItem = false,
			allowContactAbilities = false,
			random = null,
		)
	}

	/**
	 * 收拢普通伤害和替身伤害共享的“造成实际伤害后”流程。
	 *
	 * 目标本体受伤时启用低体力道具、接触特性和倒下判定；替身受伤时关闭这些目标侧 hook，但仍保留攻击方技能
	 * HP 后效、休整和道具反伤，避免两条伤害路径出现重复实现。该函数只返回推进后的状态，不接收回合临时上下文，
	 * 因为它不会修改保护集合或连续保护计数等回合内编排字段。
	 */
	fun finishPostDamageEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		faintActorAfterHit: Boolean = false,
		targetCanFaint: Boolean,
		allowTargetLowHpItem: Boolean,
		allowContactAbilities: Boolean,
		random: BattleRandom?,
	): BattleState {
		val afterActorSelfSacrifice = if (faintActorAfterHit) {
			skillHpEffects.applySelfSacrificeDamage(state, actorId, skill)
		} else {
			state
		}
		val afterUserElementRemoval = applyUserElementRemovalAfterDamage(
			state = afterActorSelfSacrifice,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val afterSkillHpEffects = skillHpEffects.applyPostDamageSkillHpEffects(
			state = afterUserElementRemoval,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val afterSkillRecharge = skillHpEffects.applyRechargeAfterDamage(
			state = afterSkillHpEffects,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val afterTargetLowHpItem = if (allowTargetLowHpItem && damageAmount > 0) {
			postDamageEffects.applyLowHpHealingItem(afterSkillRecharge, targetActorId)
		} else {
			afterSkillRecharge
		}
		val afterContactAbilities = if (allowContactAbilities && random != null) {
			postDamageEffects.applyContactAbilityEffects(
				state = afterTargetLowHpItem,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				random = random,
			)
		} else {
			afterTargetLowHpItem
		}
		val afterContactItemTransfer = if (allowContactAbilities) {
			postDamageEffects.applyContactItemTransferEffects(
				state = afterContactAbilities,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				damageAmount = damageAmount,
			)
		} else {
			afterContactAbilities
		}
		val afterContactDamage = if (allowContactAbilities) {
			postDamageEffects.applyContactDamageEffects(
				state = afterContactItemTransfer,
				actorId = actorId,
				targetActorId = targetActorId,
				skill = skill,
				damageAmount = damageAmount,
			)
		} else {
			afterContactItemTransfer
		}
		val afterRecoil = postDamageEffects.applyPostDamageItemEffects(
			state = afterContactDamage,
			actorId = actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		val faintCandidates = buildList {
			if (targetCanFaint) {
				afterRecoil.participant(targetActorId)?.let(::add)
			}
			afterRecoil.participant(actorId)?.let(::add)
		}
		return afterRecoil.handleFaintsAndResult(faintCandidates)
	}

	/**
	 * 在写入目标本体 HP 前应用“最多保留 1 HP”的技能限制。
	 *
	 * 这个限制必须放在满 HP 保命特性/道具之前：点到为止这类技能本身不会造成致命伤害，所以不应该触发“原本会被
	 * 一击打倒才保留 1 HP”的特性或携带道具事件。返回值只影响目标本体伤害写入；替身伤害入口不会调用本函数，
	 * 因为替身没有“保留 1 HP”的现代规则。目标已经只有 1 HP 时，实际写入伤害会被夹到 0，后续低体力道具和
	 * 反伤 hook 也会因为没有实际伤害而短路。
	 */
	private fun damageAmountAfterSkillTargetFloor(
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): Int {
		if (!skill.leavesTargetAtOneHp) {
			return damageAmount
		}
		return damageAmount.coerceAtMost((target.currentHp - 1).coerceAtLeast(0))
	}

	/**
	 * 在写入目标本体 HP 前应用挺住姿态。
	 *
	 * 挺住只影响当前回合的技能伤害，所以它挂在普通技能伤害写入入口，而不是通用 `receiveDamage` 或回合末伤害里。
	 * 判断顺序放在满 HP 特性/道具保命之前：一旦挺住已经把致命伤害夹到剩余 1 HP，后续满 HP 保命来源就不应该再
	 * 被触发或消耗。目标已经只有 1 HP 时，实际伤害会被夹到 0，但仍追加保命事件，用于表达本次攻击确实被挺住
	 * 姿态拦下；吸取、反伤和低体力道具随后会因为实际伤害为 0 而自然短路。
	 */
	private fun fatalDamageEndure(
		state: BattleState,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleFatalDamageEndureResult {
		val endureSkillId = target.fatalDamageEndureSkillId
		if (endureSkillId == null || damageAmount <= 0 || !target.canBattle() || damageAmount < target.currentHp) {
			return BattleFatalDamageEndureResult(target = target, damageAmount = damageAmount)
		}
		val adjustedDamage = (target.currentHp - 1).coerceAtLeast(0)
		return BattleFatalDamageEndureResult(
			target = target,
			damageAmount = adjustedDamage,
			event = BattleEvent.FatalDamageSurvived(
				turnNumber = state.turnNumber,
				actorId = actor.actorId,
				targetActorId = target.actorId,
				skillId = skill.skillId,
				source = BattleFatalDamageSurvivalSource.SKILL,
				sourceId = endureSkillId,
				consumed = false,
				incomingDamage = damageAmount,
				preventedDamage = damageAmount - adjustedDamage,
			),
		)
	}

	/**
	 * 处理技能成功造成伤害后移除使用者自身属性。
	 *
	 * 这个效果绑定在“本次技能动作已经造成实际伤害”之后，而不是目标本体 HP 是否下降之后：替身承受伤害同样说明
	 * 技能成功命中并造成了伤害，因此也会触发属性移除。移除的是技能基础属性 [BattleSkillSlot.elementId]，
	 * 不读取天气球等临时属性覆盖；燃尽和电光双击这类规则描述的都是使用者失去自己当前的火/电属性，而不是失去
	 * 本次伤害公式临时使用的属性。
	 */
	private fun applyUserElementRemovalAfterDamage(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (!skill.removesUserElementAfterDamage || damageAmount <= 0) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (skill.elementId !in actor.elementIds) {
			return state
		}
		val updatedActor = actor.copy(elementIds = actor.elementIds - skill.elementId)
		return state
			.replaceParticipant(updatedActor)
			.appendEvent(
				BattleEvent.ParticipantElementsChanged(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					skillId = skill.skillId,
					previousElementIds = actor.elementIds,
					newElementIds = updatedActor.elementIds,
				),
			)
	}
}
