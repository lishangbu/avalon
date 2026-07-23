package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleGender
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.makesEffectiveContact
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 伤害后接触特性与携带道具结算器。
 *
 * 本类的边界是“HP 已经因为某个阶段发生变化之后”。它不计算普通伤害、不处理属性倍率、不做击中要害、不判定
 * 免死，也不追加倒下事件；这些仍由主伤害流程和 [BattleState.handleFaintsAndResult] 负责。这里集中处理的是
 * 多个伤害入口都会复用的后续效果：
 * - 目标受到接触技能后，按目标特性概率给攻击方附加主要异常状态。
 * - 目标受到接触技能后，按目标特性或道具让攻击方承受最大 HP 比例反伤。
 * - 目标受到伤害后，触发低体力一次性回复道具。
 * - 使用者造成伤害后，触发生命宝珠类固定反伤。
 * - 整次技能造成伤害后，触发按实际伤害量回复的携带道具。
 *
 * 这些效果容易被误拆成“普通伤害一套、混乱自伤一套、回合末伤害一套、入场陷阱一套”。集中在这里后，各阶段
 * 仍然可以通过明确的方法选择自己需要的 hook，而真正的道具消费、回复封锁、间接伤害免疫和事件顺序只有一份。
 *
 * @property majorStatusEffects 接触特性附加主要异常状态时复用的主要异常状态结算器。
 * @property skillIgnoresTargetAbilityEffects 判断本次技能是否无视目标侧防守特性；它同时服务伤害和接触特性，
 * 因此从主引擎以回调形式传入，避免本类复制目标侧判定。
 */
internal class BattlePostDamageEffects(
	private val majorStatusEffects: BattleMajorStatusEffects,
	private val volatileStatusEffects: BattleVolatileStatusEffects,
	private val environmentEffects: BattleEnvironmentEffects,
	private val skillIgnoresTargetAbilityEffects: (BattleState, String, String) -> Boolean,
) {
	fun applyReceivedDamageFormRetaliation(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		random: BattleRandom,
	): BattleState {
		if (damageAmount <= 0 || skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) return state
		val target = state.participant(targetActorId) ?: return state
		val effect = target.abilityEffects.filterIsInstance<BattleAbilityEffect.ReceivedDamageFormRetaliation>()
			.firstOrNull { target.battleFormProfiles[it.triggerFormCode]?.creatureId == target.creatureId }
			?: return state
		val returnProfile = target.battleFormProfiles[effect.returnFormCode] ?: return state
		var current = state.replaceParticipant(target.changeBattleForm(returnProfile)).appendEvent(
			BattleEvent.FormChanged(state.turnNumber, targetActorId, target.creatureId, returnProfile.creatureId),
		)
		val attacker = current.participant(actorId) ?: return current
		if (!attacker.hasIndirectDamageImmunity()) {
			val amount = (attacker.maxHp / effect.damageDenominator).coerceAtLeast(1).coerceAtMost(attacker.currentHp)
			current = current.replaceParticipant(attacker.receiveDamage(amount)).appendEvent(
				BattleEvent.AbilityRetaliationDamageApplied(state.turnNumber, targetActorId, actorId, amount),
			)
		}
		if (effect.attackerStat != null && effect.attackerStatStageDelta != 0) {
			val latest = current.participant(actorId) ?: return current
			val changed = latest.changeStatStage(effect.attackerStat, effect.attackerStatStageDelta)
			val delta = changed.statStage(effect.attackerStat) - latest.statStage(effect.attackerStat)
			if (delta != 0) {
				current = current.replaceParticipant(changed).appendEvent(
					BattleEvent.StatStageChanged(state.turnNumber, targetActorId, actorId, effect.attackerStat, delta, changed.statStage(effect.attackerStat)),
				)
			}
		}
		val status = effect.attackerMajorStatus ?: return current
		val latest = current.participant(actorId) ?: return current
		return majorStatusEffects.applyMajorStatus(
			current, targetActorId, latest, status, random, "form retaliation status for $targetActorId",
		)
	}

	fun applyReceivedPhysicalDamageHazard(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass != BattleDamageClass.PHYSICAL) return state
		val target = state.participant(targetActorId) ?: return state
		val effect = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ReceivedPhysicalDamageOpponentSideHazard>()
			.firstOrNull() ?: return state
		val sideId = state.sideOf(actorId)?.sideId ?: return state
		val change = state.addSideEntryHazard(sideId, BattleSideEntryHazard(effect.kind)) ?: return state
		return change.state.appendEvent(
			BattleEvent.SideEntryHazardChanged(
				turnNumber = state.turnNumber,
				actorId = target.actorId,
				sideId = sideId,
				skillId = skill.skillId,
				kind = change.hazard.kind,
				layers = change.hazard.layers,
				maxLayers = change.hazard.maxLayers,
			),
		)
	}

	fun applyReceivedDamageElementChange(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.typelessDamage) return state
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (
			target.terastallized ||
			target.abilityEffects.none { it is BattleAbilityEffect.ReceivedDamageElementChange }
		) return state
		val elementId = skill.effectiveElementId(state.effectiveWeatherFor(actor), state.environment.terrain, actor)
		if (target.elementIds == setOf(elementId)) return state
		val updatedTarget = target.copy(elementIds = setOf(elementId))
		return state.replaceParticipant(updatedTarget).appendEvent(
			BattleEvent.ParticipantElementsChanged(
				state.turnNumber,
				target.actorId,
				skill.skillId,
				target.elementIds,
				updatedTarget.elementIds,
			),
		)
	}
	/**
	 * 处理目标方“受到接触技能后影响攻击方”的特性效果。
	 *
	 * 当前覆盖概率附加主要异常状态。该 hook 应在伤害事件之后、攻击方反伤和倒下判定之前执行，这样可以表达“攻击
	 * 方命中并接触目标后被麻痹/灼伤/中毒”等常见场景。函数会先检查技能是否接触、目标是否仍存在、攻击方是否
	 * 免疫接触副作用、攻击方是否无视目标特性；概率失败时只消费随机数，不追加任何事件。
	 */
	fun applyContactAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!skill.makesEffectiveContact(actor) || actor.hasContactSideEffectImmunity()) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) {
			return state
		}
		val afterInfatuation = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ContactInfatuationOnAttacker>()
			.fold(state) { current, effect ->
				val latestActor = current.participant(actorId) ?: return@fold current
				val oppositeGender = latestActor.gender != BattleGender.GENDERLESS &&
					target.gender != BattleGender.GENDERLESS && latestActor.gender != target.gender
				if (!latestActor.canBattle() || !oppositeGender ||
					!chanceSucceeds(effect.chancePercent, random, "contact infatuation for $targetActorId")) {
					current
				} else {
					volatileStatusEffects.applyVolatileStatus(
						state = current,
						actorId = targetActorId,
						recipient = latestActor,
						status = BattleVolatileStatus.INFATUATION,
						random = random,
						randomReason = "contact infatuation for $targetActorId",
					)
				}
			}
		val afterFixedStatuses = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.fold(afterInfatuation) { current, effect ->
				val latestActor = current.participant(actorId) ?: return@fold current
				if (!latestActor.canBattle() || latestActor.majorStatus != null) {
					current
				} else if (!chanceSucceeds(effect.chancePercent, random, "contact status for $targetActorId")) {
					current
				} else {
					majorStatusEffects.applyMajorStatus(
						state = current,
						actorId = targetActorId,
						recipient = latestActor,
						status = effect.status,
						random = random,
						randomReason = "contact sleep duration for $targetActorId",
					)
				}
			}
		return target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.RandomContactStatusOnAttacker>()
			.fold(afterFixedStatuses) { current, effect ->
				val latestActor = current.participant(actorId) ?: return@fold current
				if (!latestActor.canBattle() || latestActor.majorStatus != null) {
					current
				} else if (!chanceSucceeds(effect.chancePercent, random, "random contact status for $targetActorId")) {
					current
				} else {
					val status = effect.statuses[random.nextInt(effect.statuses.size, "random contact status choice for $targetActorId")]
					majorStatusEffects.applyMajorStatus(
						state = current,
						actorId = targetActorId,
						recipient = latestActor,
						status = status,
						random = random,
						randomReason = "random contact sleep duration for $targetActorId",
					)
				}
			}
	}

	/**
	 * 处理目标方“受到接触技能后让攻击方损失 HP”的特性和道具效果。
	 *
	 * 该 hook 和 [applyContactAbilityEffects] 使用相同的触发边界：只有目标本体被有效接触后才会运行；攻击方的
	 * 接触副作用免疫会阻止整条链；攻击方的间接伤害免疫会阻止实际扣血。特性反伤还会受到“本次技能忽略目标特性”
	 * 的影响，道具反伤则不受该能力影响。反伤数值始终按攻击方最大 HP 计算，不读取目标本次实际损失 HP。
	 */
	fun applyContactDamageEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (
			!skill.makesEffectiveContact(actor) ||
			actor.hasContactSideEffectImmunity() ||
			actor.hasIndirectDamageImmunity()
		) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		val afterAbilityDamage = if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) {
			state
		} else {
			target.abilityEffects
				.filterIsInstance<BattleAbilityEffect.ContactDamageToAttacker>()
				.fold(state) { current, effect ->
					applyContactDamageToAttacker(current, actorId, effect.damageDenominator)
				}
		}
		val latestTarget = afterAbilityDamage.participant(targetActorId) ?: return afterAbilityDamage
		return latestTarget.itemEffects
			.filterIsInstance<BattleItemEffect.ContactDamageToAttacker>()
			.fold(afterAbilityDamage) { current, effect ->
				applyContactDamageToAttacker(current, actorId, effect.damageDenominator)
			}
	}

	/**
	 * 处理目标方“被无道具攻击方接触后转移当前携带道具”的道具效果。
	 *
	 * 该 hook 当前用于附着针。它和接触反伤共享同一组触发边界：必须对目标本体造成了实际伤害、技能本次形成有效
	 * 接触、攻击方没有接触副作用免疫、目标仍存在且目标当前确实持有道具。攻击方必须没有携带道具，避免凭空覆盖
	 * 讲究锁、抗性树果等已有生命周期。转移时复制目标当前的 `itemId` 和 `itemEffects`，再清空目标道具；这比把
	 * 道具 ID 写进效果对象更稳，因为被转移的一定是运行态里还真实存在的那件道具。
	 */
	fun applyContactItemTransferEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		val actor = state.participant(actorId) ?: return state
		if (!skill.makesEffectiveContact(actor) || actor.hasContactSideEffectImmunity()) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (
			target.itemId == null &&
			target.abilityEffects.any { it is BattleAbilityEffect.ContactStealAttackerHeldItem } &&
			!skillIgnoresTargetAbilityEffects(state, actorId, targetActorId) &&
			actor.itemId != null &&
			actor.abilityEffects.none {
				it is BattleAbilityEffect.HeldItemTransferImmunity || it is BattleAbilityEffect.HeldItemRemovalImmunity
			}
		) {
			val stolenItemId = requireNotNull(actor.itemId)
			val updatedTarget = target.copy(itemId = stolenItemId, itemEffects = actor.itemEffects)
			return state.replaceParticipant(actor.removeHeldItem()).replaceParticipant(updatedTarget).appendEvent(
				BattleEvent.HeldItemTransferred(state.turnNumber, actor.actorId, target.actorId, stolenItemId),
			)
		}
		if (actor.itemId != null) return state
		val itemId = target.itemId ?: return state
		if (target.abilityEffects.any {
			it is BattleAbilityEffect.HeldItemTransferImmunity || it is BattleAbilityEffect.HeldItemRemovalImmunity
		}) {
			return state
		}
		if (target.itemEffects.none { it is BattleItemEffect.ContactTransferToAttacker }) {
			return state
		}
		val transferredActor = actor.copy(
			itemId = itemId,
			itemEffects = target.itemEffects,
			choiceLockedSkillId = null,
		)
		return state
			.replaceParticipant(target.removeHeldItem())
			.replaceParticipant(transferredActor)
			.appendEvent(
				BattleEvent.HeldItemTransferred(
					turnNumber = state.turnNumber,
					fromActorId = target.actorId,
					toActorId = actor.actorId,
					itemId = itemId,
				),
			)
	}

	/**
	 * 处理造成单次伤害后的携带道具效果。
	 *
	 * 当前 hook 覆盖生命宝珠类道具：成功造成伤害后按使用者最大 HP 固定比例反伤。贝壳之铃类道具需要读取整次
	 * 技能的总实际伤害，因此由多段命中循环之后的 [applyPostMoveDamageDealtHealingItem] 单独处理。变化类技能
	 * 或没有造成实际 HP 损失时直接短路，避免“0 伤害也反伤”的错误事件。
	 */
	fun applyPostDamageItemEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageBoostWithRecoil -> applyDamageBoostRecoilItem(current, actorId, effect)
					else -> current
				}
			}
	}

	/**
	 * 处理整次技能结束后的“按造成伤害回复”携带道具效果。
	 *
	 * 公开规则中贝壳之铃类道具按本次技能总实际伤害回复，而不是每一段命中各自回复。因此该 hook 放在多段循环
	 * 之后，只读取主伤害流程汇总出的普通伤害和替身伤害总量。该道具不被消费，也不改变锁招、反伤或目标侧触发
	 * 流程；如果使用者倒下、满 HP、被回复封锁或没有实际造成伤害，就保持状态不变。
	 */
	fun applyPostMoveDamageDealtHealingItem(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.fold(state) { current, effect ->
				when (effect) {
					is BattleItemEffect.DamageDealtHeal -> applyDamageDealtHealingItem(
						state = current,
						actorId = actorId,
						damageAmount = damageAmount,
						effect = effect,
					)
					else -> current
				}
			}
	}

	/**
	 * 处理低体力一次性回复类携带道具。
	 *
	 * 现代规则中，这类道具在持有者受到伤害后，如果 HP 降到触发线及以下且仍未倒下，会立刻回复并被消费。该方法
	 * 被普通伤害、混乱自伤、入场陷阱、异常状态伤害和天气伤害共同调用；它不关心伤害来源，只读取最新成员快照
	 * 判断道具是否触发。主动使用道具、紧张感等更复杂来源应在资料快照中表达为道具是否有效，而不是在调用方传
	 * 自由文本开关。
	 */
	fun applyLowHpHealingItem(state: BattleState, actorId: String, random: BattleRandom?): BattleState {
		val participant = state.participant(actorId) ?: return state
		if (!participant.canBattle() || participant.currentHp == participant.maxHp || participant.healingBlocked()) {
			return state
		}
		val effect = participant.itemEffects.filterIsInstance<BattleItemEffect.LowHpHeal>().firstOrNull() ?: return state
		if (!effect.shouldTrigger(participant.currentHp, participant.maxHp) &&
			!participant.expandedQuarterHpItemThresholdReached(effect.triggerHpNumerator, effect.triggerHpDenominator)) {
			return state
		}
		val healAmount = (effect.healAmount(participant.maxHp) * participant.heldBerryEffectMultiplier()).toInt()
			.coerceAtMost(participant.maxHp - participant.currentHp)
		if (healAmount <= 0) {
			return state
		}
		val healed = participant.heal(healAmount).consumeHeldItem()
		val afterHealing = state
			.replaceParticipant(healed)
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					amount = healAmount,
				),
			)
		if (
			effect.confusesIfNatureDecreases == null ||
			effect.confusesIfNatureDecreases != participant.natureDecreasedStat ||
			participant.confusionTurnsRemaining > 0
		) {
			return afterHealing
		}
		val confusionTurns = requireNotNull(random) { "flavor berry confusion requires battle random" }
			.nextInt(4, "flavor berry confusion duration for ${participant.actorId}") + 2
		return afterHealing
			.replaceParticipant(healed.copy(confusionTurnsRemaining = confusionTurns))
			.appendEvent(
				BattleEvent.VolatileStatusApplied(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					targetActorId = participant.actorId,
					status = BattleVolatileStatus.CONFUSION,
				),
			)
	}

	fun applyDamagingSkillItemStealAbility(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) return state
		val actor = state.participant(actorId) ?: return state
		if (actor.itemId != null || actor.abilityEffects.none {
				it is BattleAbilityEffect.DamagingSkillStealTargetHeldItem
			}) return state
		val target = state.participant(targetActorId) ?: return state
		val itemId = target.itemId ?: return state
		if (target.abilityEffects.any {
			it is BattleAbilityEffect.HeldItemTransferImmunity || it is BattleAbilityEffect.HeldItemRemovalImmunity
		}) return state
		val updatedActor = actor.copy(itemId = itemId, itemEffects = target.itemEffects)
		return state.replaceParticipant(target.removeHeldItem()).replaceParticipant(updatedActor).appendEvent(
			BattleEvent.HeldItemTransferred(state.turnNumber, target.actorId, actor.actorId, itemId),
		)
	}

	fun applyContactAbilityReplacement(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0) return state
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (!skill.makesEffectiveContact(actor) || actor.hasContactSideEffectImmunity()) return state
		if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) return state
		val actorProtected = actor.itemEffects.any { it is BattleItemEffect.AbilityIgnoreProtection }
		val targetProtected = target.itemEffects.any { it is BattleItemEffect.AbilityIgnoreProtection }
		if (target.abilityEffects.any { it is BattleAbilityEffect.ContactSwapAbilities }) {
			if (actorProtected || targetProtected) return state
			val updatedActor = actor.copy(abilityId = target.abilityId, abilityEffects = target.abilityEffects)
			val updatedTarget = target.copy(abilityId = actor.abilityId, abilityEffects = actor.abilityEffects)
			return state.replaceParticipant(updatedActor).replaceParticipant(updatedTarget).appendEvents(
				listOf(
					BattleEvent.AbilityChanged(state.turnNumber, actor.actorId, target.actorId, actor.abilityId, target.abilityId),
					BattleEvent.AbilityChanged(state.turnNumber, target.actorId, actor.actorId, target.abilityId, actor.abilityId),
				),
			)
		}
		if (
			actorProtected ||
			target.abilityEffects.none { it is BattleAbilityEffect.ContactReplaceAttackerAbilityWithHolder }
		) return state
		val updatedActor = actor.copy(abilityId = target.abilityId, abilityEffects = target.abilityEffects)
		return state.replaceParticipant(updatedActor).appendEvent(
			BattleEvent.AbilityChanged(state.turnNumber, actor.actorId, target.actorId, actor.abilityId, target.abilityId),
		)
	}

	fun applyReceivedDamageAbilityStatChanges(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) return state
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) return state
		val elementId = skill.effectiveElementId(state.effectiveWeatherFor(actor), state.environment.terrain, actor)
		val afterHolderChanges = target.abilityEffects.filterIsInstance<BattleAbilityEffect.ReceivedDamageStatStageChange>()
			.fold(state) { current, effect ->
				if (
					(effect.elementIds.isNotEmpty() && elementId !in effect.elementIds) ||
					(effect.damageClasses.isNotEmpty() && skill.damageClass !in effect.damageClasses) ||
					(effect.requiresContact && !skill.makesEffectiveContact(actor))
				) {
					return@fold current
				}
				val recipientId = if (effect.changesAttacker) actorId else targetActorId
				var recipient = current.participant(recipientId) ?: return@fold current
				val events = mutableListOf<BattleEvent>()
				effect.stageChanges.forEach { (stat, delta) ->
					val before = recipient.statStage(stat)
					recipient = recipient.changeStatStage(stat, delta)
					val actualDelta = recipient.statStage(stat) - before
					if (actualDelta != 0) {
						events += BattleEvent.StatStageChanged(
							turnNumber = current.turnNumber,
							actorId = targetActorId,
							targetActorId = recipientId,
							stat = stat,
							delta = actualDelta,
							currentStage = recipient.statStage(stat),
						)
					}
				}
				current.replaceParticipant(recipient).appendEvents(events)
			}
		val eventStartIndex = afterHolderChanges.events.size
		val afterAllOtherChanges = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ReceivedDamageAllOtherStatStageChange>()
			.fold(afterHolderChanges) { current, effect ->
				current.sides.flatMap { it.activeParticipants() }.filterNot { it.actorId == targetActorId }
					.fold(current) { changeState, snapshot ->
						val recipient = changeState.participant(snapshot.actorId) ?: return@fold changeState
						if (
							changeState.statStageDropBlockedByAbility(targetActorId, recipient, effect.stat, effect.stageDelta) ||
							changeState.statStageDropBlockedByItem(targetActorId, recipient, effect.stageDelta)
						) return@fold changeState
						val before = recipient.statStage(effect.stat)
						val updated = recipient.changeStatStage(effect.stat, effect.stageDelta)
						val delta = updated.statStage(effect.stat) - before
						if (delta == 0) changeState else changeState.replaceParticipant(updated).appendEvent(
							BattleEvent.StatStageChanged(
								changeState.turnNumber,
								targetActorId,
								recipient.actorId,
								effect.stat,
								delta,
								updated.statStage(effect.stat),
							),
						).applyNegativeStatStageResetItem(recipient.actorId)
					}
			}
		return afterAllOtherChanges.applyOpponentStatReductionReactiveAbilities(eventStartIndex, targetActorId)
	}

	fun applyReceivedDamagePerishCountdown(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) return state
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (!actor.canBattle() || !target.canBattle() || !skill.makesEffectiveContact(actor)) return state
		val effect = target.abilityEffects.filterIsInstance<BattleAbilityEffect.ContactSharedPerishCountdown>()
			.firstOrNull() ?: return state
		return listOf(target.actorId, actor.actorId).fold(state) { current, recipientId ->
			val recipient = current.participant(recipientId) ?: return@fold current
			if (!recipient.canBattle() || recipient.perishTurnsRemaining > 0) return@fold current
			current.replaceParticipant(recipient.copy(perishTurnsRemaining = effect.turns)).appendEvent(
				BattleEvent.PerishCountdownStarted(
					current.turnNumber,
					target.actorId,
					recipient.actorId,
					effect.turns,
				),
			)
		}
	}

	fun applyReceivedDamageDisableAbilities(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		random: BattleRandom,
	): BattleState {
		if (damageAmount <= 0 || skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)) return state
		val target = state.participant(targetActorId) ?: return state
		return target.abilityEffects.filterIsInstance<BattleAbilityEffect.ReceivedDamageDisableAttackerSkill>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (
					!actor.canBattle() || actor.disabledSkillTurnsRemaining > 0 ||
					!chanceSucceeds(effect.chancePercent, random, "received-damage-disable:$targetActorId")
				) return@fold current
				val updated = actor.copy(disabledSkillId = skill.skillId, disabledSkillTurnsRemaining = effect.turns)
				current.replaceParticipant(updated).appendEvent(
					BattleEvent.SkillDisabled(current.turnNumber, targetActorId, actorId, skill.skillId, effect.turns),
				)
			}
	}

	fun applyReceivedDamageThresholdAbilityChanges(
		state: BattleState,
		targetActorId: String,
		damageAmount: Int,
		criticalHit: Boolean,
	): BattleState {
		if (damageAmount <= 0) return state
		var target = state.participant(targetActorId) ?: return state
		if (!target.canBattle()) return state
		val events = mutableListOf<BattleEvent>()
		if (criticalHit) {
			target.abilityEffects.filterIsInstance<BattleAbilityEffect.CriticalDamageSetStatStage>().forEach { effect ->
				val before = target.statStage(effect.stat)
				target = target.setStatStage(effect.stat, effect.stage)
				val delta = target.statStage(effect.stat) - before
				if (delta != 0) {
					events += BattleEvent.StatStageChanged(
						state.turnNumber, target.actorId, target.actorId, effect.stat, delta, target.statStage(effect.stat),
					)
				}
			}
		}
		val hpBeforeDamage = (target.currentHp + damageAmount).coerceAtMost(target.maxHp)
		target.abilityEffects.filterIsInstance<BattleAbilityEffect.DamageCrossedHpThresholdStatStageChange>()
			.filter { effect ->
				hpBeforeDamage * effect.thresholdDenominator > target.maxHp * effect.thresholdNumerator &&
					target.currentHp * effect.thresholdDenominator <= target.maxHp * effect.thresholdNumerator
			}
			.forEach { effect ->
				effect.stageChanges.forEach { (stat, requestedDelta) ->
					val before = target.statStage(stat)
					target = target.changeStatStage(stat, requestedDelta)
					val delta = target.statStage(stat) - before
					if (delta != 0) {
						events += BattleEvent.StatStageChanged(
							state.turnNumber, target.actorId, target.actorId, stat, delta, target.statStage(stat),
						)
					}
				}
			}
		return state.replaceParticipant(target).appendEvents(events)
	}

	fun applyReceivedDamageEnvironmentAbilities(
		state: BattleState,
		targetActorId: String,
		damageAmount: Int,
		skill: BattleSkillSlot,
	): BattleState {
		if (damageAmount <= 0) return state
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle()) return state
		val afterCharge = target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ReceivedDamageNextElementDamageBoost>()
			.filter { !it.windOnly || skill.windBased }
			.fold(state) { current, effect ->
				val latest = current.participant(targetActorId) ?: return@fold current
				current.replaceParticipant(
					latest.copy(chargedElementId = effect.elementId, chargedDamageMultiplier = effect.multiplier),
				)
			}
		val afterWeather = target.abilityEffects.filterIsInstance<BattleAbilityEffect.ReceivedDamageWeatherChange>()
			.fold(afterCharge) { current, effect ->
				environmentEffects.applyReceivedDamageWeatherChange(current, target.actorId, effect)
			}
		return target.abilityEffects.filterIsInstance<BattleAbilityEffect.ReceivedDamageTerrainChange>()
			.fold(afterWeather) { current, effect ->
				environmentEffects.applyReceivedDamageTerrainChange(current, target.actorId, effect)
			}
	}

	fun applyDealtDamageStatusAbilities(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
		random: BattleRandom,
	): BattleState {
		if (damageAmount <= 0) return state
		val actor = state.participant(actorId) ?: return state
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle()) return state
		return actor.abilityEffects.filterIsInstance<BattleAbilityEffect.DealtDamageMajorStatusChance>()
			.fold(state) { current, effect ->
				val latestTarget = current.participant(targetActorId) ?: return@fold current
				if (
					latestTarget.majorStatus != null ||
					(effect.requiresContact && !skill.makesEffectiveContact(actor)) ||
					!chanceSucceeds(effect.chancePercent, random, "dealt-damage-status:$actorId:$targetActorId")
				) return@fold current
				majorStatusEffects.applyMajorStatus(
					current,
					actorId,
					latestTarget,
					effect.status,
					random,
					"dealt-damage-status-duration:$actorId:$targetActorId",
					skill,
				)
			}
	}

	fun applyFaintRetaliationAbilities(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0) return state
		val target = state.participant(targetActorId) ?: return state
		if (target.canBattle()) return state
		val explosionEffectsSuppressed = state.sides.flatMap { it.activeParticipants() }
			.any { participant ->
				participant.canBattle() && participant.abilityEffects.any {
					it is BattleAbilityEffect.ExplosionEffectSuppression
				}
			}
		return target.abilityEffects.filterIsInstance<BattleAbilityEffect.FaintAttackerDamage>()
			.fold(state) { current, effect ->
				if (effect.suppressedByExplosionSuppression && explosionEffectsSuppressed) return@fold current
				val actor = current.participant(actorId) ?: return@fold current
				if (
					!actor.canBattle() || actor.hasIndirectDamageImmunity() ||
					(effect.requiresContact && !skill.makesEffectiveContact(actor))
				) return@fold current
				val requestedDamage = if (effect.usesDamageTaken) {
					damageAmount
				} else {
					(actor.maxHp / requireNotNull(effect.attackerMaxHpDenominator)).coerceAtLeast(1)
				}
				val amount = requestedDamage.coerceAtMost(actor.currentHp)
				current.replaceParticipant(actor.receiveDamage(amount)).appendEvent(
					BattleEvent.AbilityRetaliationDamageApplied(
						current.turnNumber,
						targetActorId,
						actorId,
						amount,
					),
				)
			}
	}

	/**
	 * 统一处理受到伤害后所有依赖低体力触发线的一次性携带道具。
	 *
	 * 回复类道具先结算；如果它已经消费道具，后续能力树果自然看不到残留效果。当前成员只可能携带一个道具，
	 * 因而这个顺序不会让同一成员在一次伤害后消费两个不同来源。
	 */
	fun applyLowHpItemEffects(state: BattleState, actorId: String, random: BattleRandom?): BattleState {
		if (state.berryConsumptionBlocked(actorId)) return state
		val afterHealing = applyLowHpHealingItem(state, actorId, random)
		val afterStatBerry = applyLowHpStatStageItem(afterHealing, actorId)
		val afterRandomStatBerry = applyLowHpRandomStatStageItem(afterStatBerry, actorId, random)
		val afterAccuracyBerry = applyLowHpNextSkillAccuracyItem(afterRandomStatBerry, actorId)
		return applyLowHpCriticalHitItem(afterAccuracyBerry, actorId)
	}

	private fun applyLowHpNextSkillAccuracyItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		val effect = participant.itemEffects.filterIsInstance<BattleItemEffect.LowHpNextSkillAccuracyBoost>()
			.firstOrNull() ?: return state
		if (!participant.canBattle() || (
			!effect.shouldTrigger(participant.currentHp, participant.maxHp) &&
				!participant.expandedQuarterHpItemThresholdReached(effect.triggerHpNumerator, effect.triggerHpDenominator)
		)) return state
		val berryMultiplier = participant.heldBerryEffectMultiplier()
		val accuracyMultiplier = 1.0 + (effect.multiplier - 1.0) * berryMultiplier
		return state.replaceParticipant(
			participant.copy(nextSkillAccuracyMultiplier = accuracyMultiplier).consumeHeldItem(),
		)
	}

	private fun applyLowHpRandomStatStageItem(state: BattleState, actorId: String, random: BattleRandom?): BattleState {
		val participant = state.participant(actorId) ?: return state
		val effect = participant.itemEffects.filterIsInstance<BattleItemEffect.LowHpRandomStatStageBoost>()
			.firstOrNull() ?: return state
		if (!participant.canBattle() || (
			!effect.shouldTrigger(participant.currentHp, participant.maxHp) &&
				!participant.expandedQuarterHpItemThresholdReached(effect.triggerHpNumerator, effect.triggerHpDenominator)
		)) return state
		val eligibleStats = BattleStat.entries.filter { it in effect.stats && participant.statStage(it) < 6 }
		if (eligibleStats.isEmpty()) return state
		val selected = eligibleStats[requireNotNull(random) { "random stat berry requires battle random" }
			.nextInt(eligibleStats.size, "low hp random stat boost for $actorId")]
		val before = participant.statStage(selected)
		val stageDelta = (effect.stageDelta * participant.heldBerryEffectMultiplier()).toInt()
		val changed = participant.changeStatStage(selected, stageDelta)
		val after = changed.statStage(selected)
		return state.replaceParticipant(changed.consumeHeldItem()).appendEvent(
			BattleEvent.StatStageChanged(state.turnNumber, actorId, actorId, selected, after - before, after),
		)
	}

	/** 受到本体伤害后消费以“受伤”为生命周期终点的携带道具。 */
	fun applyDamageTriggeredItemConsumption(state: BattleState, actorId: String, damageAmount: Int): BattleState {
		if (damageAmount <= 0) return state
		val participant = state.participant(actorId) ?: return state
		if (participant.itemId == null || participant.itemEffects.none { it is BattleItemEffect.AirborneUntilDamaged }) {
			return state
		}
		return state.replaceParticipant(participant.consumeHeldItem())
	}

	/** 处理受到匹配属性或效果绝佳伤害后触发的能力提升道具。 */
	fun applyReceivedDamageStatItem(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.typelessDamage) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (target.itemId == null) {
			return state
		}
		val effect = target.itemEffects
			.filterIsInstance<BattleItemEffect.ReceivedDamageStatStageBoost>()
			.firstOrNull() ?: return state
		val actor = state.participant(actorId) ?: return state
		val skillElementId = skill.effectiveElementId(state.effectiveWeatherFor(actor), state.environment.terrain, actor)
		val effectiveness = effectiveTypeEffectiveness(
			state.rules,
			skillElementId,
			actor,
			target,
			state.effectiveEnvironmentFor(target),
		)
		if (
			(effect.elementId != null && effect.elementId != skillElementId) ||
			(effect.requiresSuperEffective && effectiveness <= 1.0)
		) {
			return state
		}
		var updated = target
		val changes = mutableListOf<Triple<BattleStat, Int, Int>>()
		effect.stageChanges.forEach { (stat, delta) ->
			val before = updated.statStage(stat)
			updated = updated.changeStatStage(stat, delta)
			val after = updated.statStage(stat)
			if (before != after) {
				changes += Triple(stat, after - before, after)
			}
		}
		if (changes.isEmpty()) {
			return state
		}
		val events = changes.map { (stat, delta, currentStage) ->
			BattleEvent.StatStageChanged(
				turnNumber = state.turnNumber,
				actorId = target.actorId,
				targetActorId = target.actorId,
				stat = stat,
				delta = delta,
				currentStage = currentStage,
			)
		}
		return state.replaceParticipant(updated.consumeHeldItem()).appendEvents(events)
	}

	private fun applyLowHpStatStageItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		if (!participant.canBattle()) {
			return state
		}
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.LowHpStatStageBoost>()
			.firstOrNull() ?: return state
		if (!effect.shouldTrigger(participant.currentHp, participant.maxHp) &&
			!participant.expandedQuarterHpItemThresholdReached(effect.triggerHpNumerator, effect.triggerHpDenominator)) {
			return state
		}
		val beforeStage = participant.statStage(effect.stat)
		val stageDelta = (effect.stageDelta * participant.heldBerryEffectMultiplier()).toInt()
		val boosted = participant.changeStatStage(effect.stat, stageDelta)
		val afterStage = boosted.statStage(effect.stat)
		if (beforeStage == afterStage) {
			return state
		}
		return state
			.replaceParticipant(boosted.consumeHeldItem())
			.appendEvent(
				BattleEvent.StatStageChanged(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					targetActorId = participant.actorId,
					stat = effect.stat,
					delta = afterStage - beforeStage,
					currentStage = afterStage,
				),
			)
	}

	private fun applyLowHpCriticalHitItem(state: BattleState, actorId: String): BattleState {
		val participant = state.participant(actorId) ?: return state
		val itemId = participant.itemId ?: return state
		if (!participant.canBattle()) {
			return state
		}
		val effect = participant.itemEffects
			.filterIsInstance<BattleItemEffect.LowHpCriticalHitStageBoost>()
			.firstOrNull() ?: return state
		val stageBonus = (effect.stageBonus * participant.heldBerryEffectMultiplier()).toInt()
		if (
			(!effect.shouldTrigger(participant.currentHp, participant.maxHp) &&
				!participant.expandedQuarterHpItemThresholdReached(effect.triggerHpNumerator, effect.triggerHpDenominator)) ||
			participant.criticalHitStageBonus >= stageBonus
		) {
			return state
		}
		val boosted = participant.copy(criticalHitStageBonus = stageBonus).consumeHeldItem()
		return state
			.replaceParticipant(boosted)
			.appendEvent(
				BattleEvent.CriticalHitStageBoostedByItem(
					turnNumber = state.turnNumber,
					actorId = participant.actorId,
					itemId = itemId,
					stageBonus = stageBonus,
				),
			)
	}

	/**
	 * 处理生命宝珠类道具造成的最大 HP 比例反伤。
	 *
	 * 生命宝珠类现代主系列规则不是“按造成伤害反伤”，而是“按使用者最大 HP 反伤”；这个函数故意只读取成员快照
	 * 中的 `maxHp`，避免伤害随机浮动、属性倍率或屏障倍率改变反伤数值。反伤造成的倒下不在这里追加倒下事件，
	 * 调用方会把攻击方作为候选交给统一倒下收口。
	 */
	private fun applyDamageBoostRecoilItem(
		state: BattleState,
		actorId: String,
		effect: BattleItemEffect.DamageBoostWithRecoil,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity()) {
			return state
		}
		val recoil = (actor.maxHp / effect.recoilDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.currentHp)
		return state
			.replaceParticipant(actor.receiveDamage(recoil))
			.appendEvent(
				BattleEvent.RecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = recoil,
				),
			)
	}

	/**
	 * 执行一次接触反伤扣血。
	 *
	 * 该函数只接收分母，不接收来源名称。原因是引擎层不关心来源是粗糙皮肤、铁刺还是凸凸头盔；规则差异已经在
	 * 上层通过“特性是否被忽略”和“道具是否存在”表达。事件继续复用 [BattleEvent.RecoilDamageApplied]，表示
	 * 攻击方因本次主动攻击后的副作用损失 HP，避免为了同一类 HP 写入新增并行事件结构。
	 */
	private fun applyContactDamageToAttacker(
		state: BattleState,
		actorId: String,
		damageDenominator: Int,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.hasIndirectDamageImmunity()) {
			return state
		}
		val damage = (actor.maxHp / damageDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.currentHp)
		return state
			.replaceParticipant(actor.receiveDamage(damage))
			.appendEvent(
				BattleEvent.RecoilDamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = damage,
				),
			)
	}

	/**
	 * 处理按实际造成伤害量回复的携带道具。
	 *
	 * 回复量使用本次实际 HP 损失向下取整，最少 1 点，并夹取到使用者缺失 HP。调用方已经保证 `damageAmount > 0`，
	 * 因此本函数只需要处理使用者倒下、满 HP 或被回复封锁的情况。回复事件使用普通 [BattleEvent.HealingApplied]，
	 * 与其它非天气/非场地回复保持一致。
	 */
	private fun applyDamageDealtHealingItem(
		state: BattleState,
		actorId: String,
		damageAmount: Int,
		effect: BattleItemEffect.DamageDealtHeal,
	): BattleState {
		val actor = state.participant(actorId) ?: return state
		if (!actor.canBattle() || actor.currentHp == actor.maxHp || actor.healingBlocked()) {
			return state
		}
		val healAmount = (damageAmount / effect.healDenominator)
			.coerceAtLeast(1)
			.coerceAtMost(actor.maxHp - actor.currentHp)
		if (healAmount <= 0) {
			return state
		}
		return state
			.replaceParticipant(actor.heal(healAmount))
			.appendEvent(
				BattleEvent.HealingApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					amount = healAmount,
				),
			)
	}
}

/**
 * 判断攻击方是否免疫“因为主动接触目标而承受”的副作用。
 *
 * 这里不改变 [BattleSkillSlot.makesEffectiveContact] 的结果，因为部位护具这类道具不会让技能失去接触事实；它只让
 * 攻击方免受目标侧接触反制。将判断作为本文件私有扩展，可以把副作用免疫限定在接触后 hook 内，避免误扩散到保护
 * 绕过、接触类伤害倍率或其它仍应看到真实接触事实的规则。
 */
private fun io.github.lishangbu.battleengine.model.BattleParticipant.hasContactSideEffectImmunity(): Boolean =
	itemEffects.any { it is BattleItemEffect.ContactSideEffectImmunity }
