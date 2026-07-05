package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleVolatileStatus

/*
 * `BattleParticipant` 的主要异常状态与临时状态运行态操作。
 *
 * 这一组函数只表达状态本身如何写入、递减或清除；它不负责状态是否免疫、是否被替身阻挡、随机持续时间如何产生，
 * 也不负责追加状态应用或解除事件。这样状态免疫顺序留在 resolver，成员快照只保存已经确认生效的运行态。
 */
/**
 * 附加主要异常状态。
 *
 * 主要异常状态不会覆盖已有主要异常状态；调用方会在进入这里前记录对应阻止事件。睡眠状态使用
 * `sleepTurnsRemaining` 表达还会阻止行动几次，而不是保存历史版本的原始睡眠计数。
 */
fun BattleParticipant.applyMajorStatus(status: BattleMajorStatus, sleepTurnsRemaining: Int = 0): BattleParticipant {
	require(status == BattleMajorStatus.SLEEP || sleepTurnsRemaining == 0) {
		"sleepTurnsRemaining can only be set for sleep"
	}
	require(status != BattleMajorStatus.SLEEP || sleepTurnsRemaining > 0) {
		"sleepTurnsRemaining must be positive for sleep"
	}
	return if (majorStatus == null) {
		copy(
			majorStatus = status,
			badPoisonCounter = if (status == BattleMajorStatus.BAD_POISON) 1 else 0,
			sleepTurnsRemaining = if (status == BattleMajorStatus.SLEEP) sleepTurnsRemaining else 0,
		)
	} else {
		this
	}
}

/**
 * 消耗一次睡眠阻止行动次数。
 *
 * 返回值只修改成员运行态，不产生事件；调用方需要根据 `sleepTurnsRemaining` 的前后变化追加
 * “睡眠阻止行动”或“状态解除”等可观察事实。
 */
fun BattleParticipant.consumeSleepBlockedTurn(): BattleParticipant =
	if (majorStatus == BattleMajorStatus.SLEEP && sleepTurnsRemaining > 1) {
		copy(sleepTurnsRemaining = sleepTurnsRemaining - 1)
	} else if (majorStatus == BattleMajorStatus.SLEEP) {
		copy(majorStatus = null, sleepTurnsRemaining = 0)
	} else {
		this
	}

/**
 * 清除主要异常状态以及该状态绑定的运行态计数。
 */
fun BattleParticipant.clearMajorStatus(): BattleParticipant =
	if (majorStatus == null) {
		this
	} else {
		copy(majorStatus = null, badPoisonCounter = 0, sleepTurnsRemaining = 0)
	}

/**
 * 附加临时状态。
 *
 * 畏缩只持续到本回合行动前或回合末，因此不需要额外计数。混乱使用公开实现中的内部计数：
 * 成员行动前先递减一次，递减后为 0 则解除并照常行动，否则再进行混乱自伤判定。回复封锁、挑衅和定身法保存
 * 回合末递减计数，分别用于阻止回复类技能、变化类技能和被指定禁用的技能。无理取闹没有回合倒计时，
 * 它持续到成员离场，期间阻止连续两次真正使用同一个技能。束缚记录来源成员和剩余回合，用于阻止主动替换
 * 并在回合末造成固定比例伤害。
 */
fun BattleParticipant.applyVolatileStatus(
	status: BattleVolatileStatus,
	confusionTurnsRemaining: Int = 0,
	healBlockTurnsRemaining: Int = 0,
	tauntTurnsRemaining: Int = 0,
	disabledSkillId: Long? = null,
	disabledSkillTurnsRemaining: Int = 0,
	boundByActorId: String? = null,
	bindingTurnsRemaining: Int = 0,
): BattleParticipant {
	require(status == BattleVolatileStatus.CONFUSION || confusionTurnsRemaining == 0) {
		"confusionTurnsRemaining can only be set for confusion"
	}
	require(status != BattleVolatileStatus.CONFUSION || confusionTurnsRemaining > 0) {
		"confusionTurnsRemaining must be positive for confusion"
	}
	require(status == BattleVolatileStatus.HEAL_BLOCK || healBlockTurnsRemaining == 0) {
		"healBlockTurnsRemaining can only be set for heal block"
	}
	require(status != BattleVolatileStatus.HEAL_BLOCK || healBlockTurnsRemaining > 0) {
		"healBlockTurnsRemaining must be positive for heal block"
	}
	require(status == BattleVolatileStatus.TAUNT || tauntTurnsRemaining == 0) {
		"tauntTurnsRemaining can only be set for taunt"
	}
	require(status != BattleVolatileStatus.TAUNT || tauntTurnsRemaining > 0) {
		"tauntTurnsRemaining must be positive for taunt"
	}
	require(status == BattleVolatileStatus.DISABLE || disabledSkillId == null) {
		"disabledSkillId can only be set for disable"
	}
	require(status == BattleVolatileStatus.DISABLE || disabledSkillTurnsRemaining == 0) {
		"disabledSkillTurnsRemaining can only be set for disable"
	}
	require(status != BattleVolatileStatus.DISABLE || disabledSkillId != null) {
		"disabledSkillId must be present for disable"
	}
	require(status != BattleVolatileStatus.DISABLE || disabledSkillTurnsRemaining > 0) {
		"disabledSkillTurnsRemaining must be positive for disable"
	}
	require(status == BattleVolatileStatus.BINDING || boundByActorId == null) {
		"boundByActorId can only be set for binding"
	}
	require(status == BattleVolatileStatus.BINDING || bindingTurnsRemaining == 0) {
		"bindingTurnsRemaining can only be set for binding"
	}
	require(status != BattleVolatileStatus.BINDING || boundByActorId != null) {
		"boundByActorId must be present for binding"
	}
	require(status != BattleVolatileStatus.BINDING || bindingTurnsRemaining > 0) {
		"bindingTurnsRemaining must be positive for binding"
	}
	return when (status) {
		BattleVolatileStatus.FLINCH -> copy(flinched = true)
		BattleVolatileStatus.CONFUSION -> if (this.confusionTurnsRemaining > 0) {
			this
		} else {
			copy(confusionTurnsRemaining = confusionTurnsRemaining)
		}
		BattleVolatileStatus.HEAL_BLOCK -> if (this.healBlockTurnsRemaining > 0) {
			this
		} else {
			copy(healBlockTurnsRemaining = healBlockTurnsRemaining)
		}
		BattleVolatileStatus.TAUNT -> if (this.tauntTurnsRemaining > 0) {
			this
		} else {
			copy(tauntTurnsRemaining = tauntTurnsRemaining)
		}
		BattleVolatileStatus.DISABLE -> if (this.disabledSkillTurnsRemaining > 0) {
			this
		} else {
			copy(
				disabledSkillId = disabledSkillId,
				disabledSkillTurnsRemaining = disabledSkillTurnsRemaining,
			)
		}
		BattleVolatileStatus.TORMENT -> if (tormented) this else copy(tormented = true)
		BattleVolatileStatus.BINDING -> if (this.bindingTurnsRemaining > 0) {
			this
		} else {
			copy(
				boundByActorId = boundByActorId,
				bindingTurnsRemaining = bindingTurnsRemaining,
			)
		}
	}
}

/**
 * 消耗一次畏缩阻止行动。
 *
 * 畏缩阻止本次行动后立即消失；如果成员本回合没有行动，回合末也会被静默清理。
 */
fun BattleParticipant.consumeFlinch(): BattleParticipant =
	if (flinched) copy(flinched = false) else this

/**
 * 混乱行动前递减一次内部计数。
 */
fun BattleParticipant.decrementConfusionBeforeMove(): BattleParticipant =
	if (confusionTurnsRemaining > 0) {
		copy(confusionTurnsRemaining = confusionTurnsRemaining - 1)
	} else {
		this
	}

/**
 * 回合末推进回复封锁剩余回合。
 *
 * 回复封锁不像畏缩那样回合末静默清理，也不像混乱那样行动前递减；它按完整回合持续时间递减，
 * 递减到 0 时调用方会追加临时状态解除事件。
 */
fun BattleParticipant.decrementHealBlockEndTurn(): BattleParticipant =
	if (healBlockTurnsRemaining > 0) copy(healBlockTurnsRemaining = healBlockTurnsRemaining - 1) else this

/**
 * 回合末推进挑衅剩余回合。
 *
 * 挑衅在现代规则下按固定持续回合处理，效果期间只限制变化技能的使用；持续回合归零时调用方追加
 * 临时状态解除事件，方便 replay 明确看到挑衅自然结束。
 */
fun BattleParticipant.decrementTauntEndTurn(): BattleParticipant =
	if (tauntTurnsRemaining > 0) copy(tauntTurnsRemaining = tauntTurnsRemaining - 1) else this

/**
 * 回合末推进定身法剩余回合。
 *
 * 定身法保存的是“被禁用技能 + 剩余完整回合数”。每个回合末递减一次；归零时同时清除技能 ID，避免后续
 * 行动选择校验把一个已经结束的禁用状态误判为仍然生效。
 */
fun BattleParticipant.decrementDisableEndTurn(): BattleParticipant =
	when {
		disabledSkillTurnsRemaining > 1 -> copy(disabledSkillTurnsRemaining = disabledSkillTurnsRemaining - 1)
		disabledSkillTurnsRemaining == 1 -> clearDisable()
		else -> this
	}

/**
 * 清除定身法运行态。
 */
fun BattleParticipant.clearDisable(): BattleParticipant =
	if (disabledSkillTurnsRemaining == 0 && disabledSkillId == null) {
		this
	} else {
		copy(disabledSkillId = null, disabledSkillTurnsRemaining = 0)
	}

/**
 * 回合末推进束缚剩余回合。
 *
 * 束缚每个回合末先造成固定比例伤害，再递减持续回合。归零时清除来源成员 ID，避免之后主动替换校验继续误判
 * 成员仍被同一个来源困住。
 */
fun BattleParticipant.decrementBindingEndTurn(): BattleParticipant =
	when {
		bindingTurnsRemaining > 1 -> copy(bindingTurnsRemaining = bindingTurnsRemaining - 1)
		bindingTurnsRemaining == 1 -> clearBinding()
		else -> this
	}

/**
 * 清除束缚运行态。
 */
fun BattleParticipant.clearBinding(): BattleParticipant =
	if (bindingTurnsRemaining == 0 && boundByActorId == null) {
		this
	} else {
		copy(boundByActorId = null, bindingTurnsRemaining = 0)
	}

/**
 * 清除指定临时状态。
 */
fun BattleParticipant.clearVolatileStatus(status: BattleVolatileStatus): BattleParticipant =
	when (status) {
		BattleVolatileStatus.FLINCH -> if (flinched) copy(flinched = false) else this
		BattleVolatileStatus.CONFUSION -> if (confusionTurnsRemaining > 0) copy(confusionTurnsRemaining = 0) else this
		BattleVolatileStatus.HEAL_BLOCK -> if (healBlockTurnsRemaining > 0) copy(healBlockTurnsRemaining = 0) else this
		BattleVolatileStatus.TAUNT -> if (tauntTurnsRemaining > 0) copy(tauntTurnsRemaining = 0) else this
		BattleVolatileStatus.DISABLE -> clearDisable()
		BattleVolatileStatus.TORMENT -> if (tormented) copy(tormented = false) else this
		BattleVolatileStatus.BINDING -> clearBinding()
	}

/**
 * 清理回合结束时不会跨回合保留的临时状态。
 */
fun BattleParticipant.clearEndTurnVolatileStatuses(): BattleParticipant =
	copy(
		flinched = false,
		fatalDamageEndureSkillId = null,
	)
