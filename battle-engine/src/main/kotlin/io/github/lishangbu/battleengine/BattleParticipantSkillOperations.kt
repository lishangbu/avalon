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
 * 消费当前携带道具。
 *
 * 第一批道具生命周期只需要表达“一次性触发后不再拥有该道具效果”。道具 ID 和所有道具效果一起清空，
 * 因为成员快照只允许一个携带道具；后续如果要支持道具被替换、回收或禁用而不移除，会增加更细的状态字段。
 */
fun BattleParticipant.consumeHeldItem(): BattleParticipant =
	copy(itemId = null, itemEffects = emptyList(), choiceLockedSkillId = null)

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
	return copy(lastSuccessfulSkillId = skillId)
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
