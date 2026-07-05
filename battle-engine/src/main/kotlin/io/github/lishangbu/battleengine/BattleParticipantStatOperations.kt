package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat

/*
 * `BattleParticipant` 的能力阶级、连续保护、剧毒计数与离场清理操作。
 *
 * 这些状态都属于成员在场期间的战斗修正。它们与 HP、技能 PP、主要异常状态分开，是为了让离场、能力变化和回合末
 * 持续效果可以独立维护，同时保持所有调用点仍通过 `participant.xxx()` 读取同一套领域语言。
 */
/**
 * 改变一个能力阶级，并夹取到现代规则允许的 -6..6。
 */
fun BattleParticipant.changeStatStage(stat: BattleStat, delta: Int): BattleParticipant {
	require(delta != 0) { "delta must not be zero" }
	val next = ((statStages[stat] ?: 0) + delta).coerceIn(-6, 6)
	return copy(statStages = statStages + (stat to next))
}

/**
 * 直接设置一个能力阶级，并夹取到现代规则允许的 -6..6。
 *
 * 清除、复制、交换和取反等技能效果需要按“结果值”写回能力阶级，而不是按 delta 叠加。0 阶级会从 Map 中移除，
 * 让默认值和显式 0 保持同一份运行态表示，避免后续比较出现伪差异。
 */
fun BattleParticipant.setStatStage(stat: BattleStat, stage: Int): BattleParticipant {
	val next = stage.coerceIn(-6, 6)
	return if (next == 0) {
		copy(statStages = statStages - stat)
	} else {
		copy(statStages = statStages + (stat to next))
	}
}

/**
 * 读取指定能力项当前阶级。
 */
fun BattleParticipant.statStage(stat: BattleStat): Int =
	statStages[stat] ?: 0

/**
 * 标记本成员本回合成功使用保护类行动。
 *
 * `protectionChain` 表示连续成功保护次数，用于下一次保护类行动计算递减成功率。
 */
fun BattleParticipant.markProtectionSuccess(): BattleParticipant =
	copy(protectionChain = protectionChain + 1)

/**
 * 清空连续保护计数。
 *
 * 只要本成员在一个回合内没有成功建立保护屏障，回合末就应调用该函数，确保下一次保护重新从必定成功开始。
 */
fun BattleParticipant.resetProtectionChain(): BattleParticipant =
	if (protectionChain == 0) this else copy(protectionChain = 0)

/**
 * 推进剧毒计数。
 *
 * 剧毒伤害在每次回合末结算后递增一次。若当前成员不是剧毒状态，则保持原样，避免普通中毒误用计数。
 */
fun BattleParticipant.advanceBadPoisonCounter(): BattleParticipant =
	if (majorStatus == BattleMajorStatus.BAD_POISON) {
		copy(badPoisonCounter = badPoisonCounter.coerceAtLeast(1) + 1)
	} else {
		this
	}

/**
 * 处理成员离开上场席位时应清除的运行态。
 *
 * 现代规则下，替换会清除能力阶级和连续保护计数，但不会清除 HP、PP、主要异常状态、特性或携带道具。
 * 剧毒状态会保留，但剧毒递增计数回到 1；睡眠状态和剩余阻止行动次数在现代规则下随成员保留。
 * 畏缩、混乱、回复封锁、挑衅、定身法、无理取闹、束缚、命中锁定和技能造成的临时体重减轻属于在场状态，离场时会被清除。
 * 后续接入锁招、寄生等离场即消失的状态时，也应在这里统一清理。
 */
fun BattleParticipant.leaveBattlefield(): BattleParticipant =
	copy(
		statStages = emptyMap(),
		weightReduction = 0,
		protectionChain = 0,
		badPoisonCounter = if (majorStatus == BattleMajorStatus.BAD_POISON) 1 else 0,
		chargingSkillId = null,
		chargingTargetActorId = null,
		chargingTurnsRemaining = 0,
		rechargeTurnsRemaining = 0,
		flinched = false,
		confusionTurnsRemaining = 0,
		healBlockTurnsRemaining = 0,
		tauntTurnsRemaining = 0,
		disabledSkillId = null,
		disabledSkillTurnsRemaining = 0,
		tormented = false,
		boundByActorId = null,
		bindingTurnsRemaining = 0,
		lastSuccessfulSkillId = null,
		accuracyLockTargetActorId = null,
		accuracyLockTurnsRemaining = 0,
		lockedMoveSkillId = null,
		lockedMoveTargetActorId = null,
		lockedMoveTurnsRemaining = 0,
		lockedMoveConfusesOnEnd = false,
		choiceLockedSkillId = null,
		substituteHp = 0,
	)
