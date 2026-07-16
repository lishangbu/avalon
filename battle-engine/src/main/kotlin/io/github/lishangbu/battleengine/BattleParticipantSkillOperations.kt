package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/*
 * `BattleParticipant` 的技能选择、PP、蓄力、休整、锁招和携带道具运行态操作。
 *
 * 这些函数维护“成员下一次技能行动会被怎样约束”的状态，例如讲究类道具锁定、蓄力目标、休整计数和连续锁招。
 * 它们保持不可变快照风格，只返回新的成员状态；是否产生技能使用、PP 消耗、疲劳混乱或道具触发事件仍由回合流程决定。
 */
/**
 * 替换一个技能槽，通常用于 PP 消耗。
 */
fun BattleParticipant.replaceSkillSlot(slot: BattleSkillSlot): BattleParticipant =
	copy(skillSlots = skillSlots.map { current -> if (current.skillId == slot.skillId) slot else current })

/**
 * 记录本次上场后的技能行动尝试。
 *
 * 这里的“尝试”发生在睡眠、畏缩、麻痹、混乱等行动前状态判定之前：只要成员已经轮到一次技能行动，它就不再处于
 * Fake Out / First Impression 所要求的“刚上场后的第一次行动”之外。主动替换不走技能行动流程，因此不会调用本函数；
 * 换出再换入会通过 [io.github.lishangbu.battleengine.enterBattlefield] 重新把计数归零。
 */
fun BattleParticipant.recordSkillActionAttempt(): BattleParticipant =
	copy(activeSkillActionCount = activeSkillActionCount + 1)

/**
 * 判断本次技能行动是否仍是成员换入后的第一次技能行动。
 *
 * 公开成熟引擎用 `activeMoveActions` 判定这类技能能否成功；本引擎对应保存为 [BattleParticipant.activeSkillActionCount]。
 * 因为 [recordSkillActionAttempt] 会先递增再进入技能宣告和失败 gate，所以成功条件正好是计数等于 1。
 */
fun BattleParticipant.isFirstSkillActionSinceEntering(): Boolean =
	activeSkillActionCount == 1

/**
 * 消费当前携带道具。
 *
 * 当前成员快照只允许一个携带道具，因此一次性触发后会同时清空道具 ID、道具效果和讲究类技能锁定。
 * 这表示“成员已经不再持有该道具”，不同于暂时禁用、回收或替换道具；那些生命周期需要额外字段时应单独建模，
 * 不应让已消费道具继续残留可执行效果。
 */
fun BattleParticipant.consumeHeldItem(): BattleParticipant {
	val consumedItemId = itemId ?: return this
	val consumedItemEffects = itemEffects
	val berryConsumed = consumedItemEffects.any { it is BattleItemEffect.BerryMarker }
	val berryHealAmount = if (berryConsumed) {
		abilityEffects.filterIsInstance<io.github.lishangbu.battleengine.model.BattleAbilityEffect.BerryConsumptionHeal>()
			.sumOf { effect -> (maxHp / effect.healDenominator).coerceAtLeast(1) }
	} else {
		0
	}
	return copy(
		itemId = null,
		itemEffects = emptyList(),
		lastConsumedItemId = consumedItemId,
		lastConsumedItemEffects = consumedItemEffects,
		itemLostSinceEntering = true,
		choiceLockedSkillId = null,
		currentHp = (currentHp + berryHealAmount).coerceAtMost(maxHp),
	)
	}

/** 单纯移除或转移携带道具，不把它记录为已消费。 */
fun BattleParticipant.removeHeldItem(): BattleParticipant =
	copy(
		itemId = null,
		itemEffects = emptyList(),
		itemLostSinceEntering = itemLostSinceEntering || itemId != null,
		choiceLockedSkillId = null,
	)

/** 把最近消费的树果恢复为当前携带道具。 */
fun BattleParticipant.restoreLastConsumedBerry(): BattleParticipant {
	val restoredItemId = lastConsumedItemId ?: return this
	if (itemId != null || lastConsumedItemEffects.none { it is BattleItemEffect.BerryMarker }) return this
	return copy(
		itemId = restoredItemId,
		itemEffects = lastConsumedItemEffects,
		lastConsumedItemId = null,
		lastConsumedItemEffects = emptyList(),
		itemLostSinceEntering = false,
	)
}

/**
 * 按讲究类道具规则记录首次成功宣告的技能。
 *
 * 只有携带对应道具效果且尚未锁定时才会写入。已经锁定的成员保持原技能，避免一次异常行动覆盖既有选择。
 */
fun BattleParticipant.lockChoiceSkillIfNeeded(skillId: Long): BattleParticipant {
	require(skillId > 0) { "skillId must be positive" }
	val hasChoiceLock = itemEffects.any { it is BattleItemEffect.ChoiceSkillLock }
	return if (!hasChoiceLock || choiceLockedSkillId != null) {
		this
	} else {
		copy(choiceLockedSkillId = skillId)
	}
}

/**
 * 判断讲究类道具是否禁止本次技能选择。
 */
fun BattleParticipant.choiceLockedToAnotherSkill(skillId: Long): Boolean {
	require(skillId > 0) { "skillId must be positive" }
	return choiceLockedSkillId != null && choiceLockedSkillId != skillId
}

/**
 * 查找本成员可使用的技能槽。
 */
fun BattleParticipant.skillSlot(skillId: Long): BattleSkillSlot? =
	skillSlots.firstOrNull { it.skillId == skillId }

/**
 * 标记成员进入技能休整状态。
 *
 * 现代规则中，部分强力技能在成功造成伤害后会让使用者下一次技能行动前休整一次。该计数只保存“未来还会
 * 阻止几次技能行动”，当前成功使用技能的回合不计入。若成员已经处于休整状态，调用方不应重复叠加。
 */
fun BattleParticipant.startRecharge(turnsRemainingAfterCurrent: Int = 1): BattleParticipant {
	require(turnsRemainingAfterCurrent > 0) { "turnsRemainingAfterCurrent must be positive" }
	return copy(rechargeTurnsRemaining = turnsRemainingAfterCurrent)
}

/**
 * 消耗一次技能休整阻止行动。
 */
fun BattleParticipant.consumeRechargeTurn(): BattleParticipant =
	if (rechargeTurnsRemaining > 0) copy(rechargeTurnsRemaining = rechargeTurnsRemaining - 1) else this

/**
 * 标记成员进入技能蓄力状态。
 *
 * 该状态表示本次技能已经支付 PP 并完成宣告，但真正效果要在未来技能行动中释放。`turnsRemainingBeforeUse`
 * 保存未来还要等待几次技能行动；现代常见蓄力技能为 1。目标保存为首次选择的目标槽位，以便下一回合复用
 * 现有目标重定向规则。
 */
fun BattleParticipant.startChargingSkill(
	skillId: Long,
	targetActorId: String,
	turnsRemainingBeforeUse: Int = 1,
): BattleParticipant {
	require(skillId > 0) { "skillId must be positive" }
	require(targetActorId.isNotBlank()) { "targetActorId must not be blank" }
	require(turnsRemainingBeforeUse > 0) { "turnsRemainingBeforeUse must be positive" }
	return copy(
		chargingSkillId = skillId,
		chargingTargetActorId = targetActorId,
		chargingTurnsRemaining = turnsRemainingBeforeUse,
	)
}

/**
 * 消耗一次技能蓄力等待。
 */
fun BattleParticipant.consumeChargingTurn(): BattleParticipant =
	when {
		chargingTurnsRemaining > 1 -> copy(chargingTurnsRemaining = chargingTurnsRemaining - 1)
		chargingTurnsRemaining == 1 -> clearChargingSkill()
		else -> this
	}

/**
 * 清除技能蓄力运行态。
 */
fun BattleParticipant.clearChargingSkill(): BattleParticipant =
	if (chargingTurnsRemaining == 0 && chargingSkillId == null && chargingTargetActorId == null) {
		this
	} else {
		copy(
			chargingSkillId = null,
			chargingTargetActorId = null,
			chargingTurnsRemaining = 0,
		)
	}

/**
 * 记录本成员最近一次真正进入使用流程的技能。
 *
 * 定身法、再来一次等规则需要读取“目标上一次成功宣告并消耗 PP 的技能”。睡眠、麻痹、挑衅等在 PP 消耗前
 * 阻止的行动不会调用该函数，因此不会污染最后成功技能。充能释放等不消耗 PP 的后续动作仍属于同一次技能
 * 的成功使用，记录同一个 skillId 也不会改变可观察结果。
 */
fun BattleParticipant.markSuccessfulSkill(skillId: Long): BattleParticipant {
	require(skillId > 0) { "skillId must be positive" }
	return copy(
		lastSuccessfulSkillId = skillId,
		consecutiveSuccessfulSkillUses = if (lastSuccessfulSkillId == skillId) consecutiveSuccessfulSkillUses + 1 else 1,
	)
}

/**
 * 记录使用者对一个目标建立命中锁定。
 *
 * Lock-On / Mind Reader 这类现代规则不是永久标记：效果在命中的当前回合建立，经过当前回合末后仍保留到下一回合，
 * 并在下一回合结束时清除。因此这里默认写入 2 个回合末递减单位；回合末流水线会把它推进为 1，再在下一次回合末
 * 清掉。目标或使用者离场会更早清理该状态。
 */
fun BattleParticipant.lockAccuracyOnTarget(
	targetActorId: String,
	turnsRemaining: Int = 2,
): BattleParticipant {
	require(targetActorId.isNotBlank()) { "targetActorId must not be blank" }
	require(turnsRemaining > 0) { "turnsRemaining must be positive" }
	return copy(
		accuracyLockTargetActorId = targetActorId,
		accuracyLockTurnsRemaining = turnsRemaining,
	)
}

/**
 * 判断本成员当前是否锁定了指定目标的命中判定。
 */
fun BattleParticipant.hasAccuracyLockOn(targetActorId: String): Boolean =
	accuracyLockTargetActorId == targetActorId && accuracyLockTurnsRemaining > 0

/**
 * 回合末推进命中锁定持续时间。
 */
fun BattleParticipant.decrementAccuracyLockEndTurn(): BattleParticipant =
	when {
		accuracyLockTurnsRemaining > 1 -> copy(accuracyLockTurnsRemaining = accuracyLockTurnsRemaining - 1)
		accuracyLockTurnsRemaining == 1 -> clearAccuracyLock()
		else -> this
	}

/**
 * 清除命中锁定运行态。
 */
fun BattleParticipant.clearAccuracyLock(): BattleParticipant =
	if (accuracyLockTargetActorId == null && accuracyLockTurnsRemaining == 0) {
		this
	} else {
		copy(accuracyLockTargetActorId = null, accuracyLockTurnsRemaining = 0)
	}

/**
 * 开始锁定连续使用某个技能。
 *
 * `turnsRemainingAfterCurrent` 只记录未来还会被强制行动几次；首次使用的当前回合不保存在计数中。
 */
fun BattleParticipant.startLockedMove(
	skillId: Long,
	targetActorId: String,
	turnsRemainingAfterCurrent: Int,
	confusesOnEnd: Boolean,
): BattleParticipant {
	require(skillId > 0) { "skillId must be positive" }
	require(targetActorId.isNotBlank()) { "targetActorId must not be blank" }
	require(turnsRemainingAfterCurrent > 0) { "turnsRemainingAfterCurrent must be positive" }
	return copy(
		lockedMoveSkillId = skillId,
		lockedMoveTargetActorId = targetActorId,
		lockedMoveTurnsRemaining = turnsRemainingAfterCurrent,
		lockedMoveConfusesOnEnd = confusesOnEnd,
	)
}

/**
 * 消耗一次锁招强制行动。
 *
 * 若返回值的 `lockedMoveTurnsRemaining` 变为 0，调用方需要根据原状态决定是否追加疲劳混乱。
 */
fun BattleParticipant.consumeLockedMoveTurn(): BattleParticipant =
	when {
		lockedMoveTurnsRemaining > 1 -> copy(lockedMoveTurnsRemaining = lockedMoveTurnsRemaining - 1)
		lockedMoveTurnsRemaining == 1 -> clearLockedMove()
		else -> this
	}

/**
 * 清除锁招运行态。
 */
fun BattleParticipant.clearLockedMove(): BattleParticipant =
	if (lockedMoveTurnsRemaining == 0 && lockedMoveSkillId == null && lockedMoveTargetActorId == null && !lockedMoveConfusesOnEnd) {
		this
	} else {
		copy(
			lockedMoveSkillId = null,
			lockedMoveTargetActorId = null,
			lockedMoveTurnsRemaining = 0,
			lockedMoveConfusesOnEnd = false,
		)
	}
