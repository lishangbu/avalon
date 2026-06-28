package io.github.lishangbu.battleengine.model

/**
 * 一名参与战斗的成员快照。
 *
 * 成员保存战斗结算需要的当前运行态：HP、等级、五项战斗能力、属性集合、技能槽、特性/道具身份、
 * 是否接地、连续保护计数、剧毒计数、睡眠剩余阻止行动次数、锁招运行态，以及畏缩/混乱等临时状态。
 * 它不直接包含种类、训练者、背包或数据库实体；这些资料应在进入引擎前转换成稳定数值。
 *
 * 第一阶段状态不变量：
 * - `currentHp` 必须位于 `0..maxHp`。
 * - 可行动成员必须拥有至少一个技能槽。
 * - 攻击、防御、特攻、特防、速度必须为正数，避免公式除零或负速度排序。
 */
data class BattleParticipant(
	val actorId: String,
	val creatureId: Long,
	val level: Int,
	val maxHp: Int,
	val currentHp: Int,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val elementIds: Set<Long>,
	val skillSlots: List<BattleSkillSlot>,
	val abilityId: Long? = null,
	val itemId: Long? = null,
	val grounded: Boolean = true,
	val majorStatus: BattleMajorStatus? = null,
	val statStages: Map<BattleStat, Int> = emptyMap(),
	val protectionChain: Int = 0,
	val badPoisonCounter: Int = 0,
	val sleepTurnsRemaining: Int = 0,
	val flinched: Boolean = false,
	val confusionTurnsRemaining: Int = 0,
	val lockedMoveSkillId: Long? = null,
	val lockedMoveTargetActorId: String? = null,
	val lockedMoveTurnsRemaining: Int = 0,
	val lockedMoveConfusesOnEnd: Boolean = false,
	val abilityEffects: List<BattleAbilityEffect> = emptyList(),
	val itemEffects: List<BattleItemEffect> = emptyList(),
) {
	init {
		require(actorId.isNotBlank()) { "actorId must not be blank" }
		require(creatureId > 0) { "creatureId must be positive" }
		require(level in 1..100) { "level must be between 1 and 100" }
		require(maxHp > 0) { "maxHp must be positive" }
		require(currentHp in 0..maxHp) { "currentHp must be between 0 and maxHp" }
		require(attack > 0) { "attack must be positive" }
		require(defense > 0) { "defense must be positive" }
		require(specialAttack > 0) { "specialAttack must be positive" }
		require(specialDefense > 0) { "specialDefense must be positive" }
		require(speed > 0) { "speed must be positive" }
		require(elementIds.all { it > 0 }) { "elementIds must contain only positive ids" }
		require(skillSlots.isNotEmpty()) { "skillSlots must not be empty" }
		require(skillSlots.map { it.skillId }.toSet().size == skillSlots.size) { "skillSlots must not contain duplicate skill ids" }
		require(abilityId == null || abilityId > 0) { "abilityId must be positive when present" }
		require(itemId == null || itemId > 0) { "itemId must be positive when present" }
		require(statStages.values.all { it in -6..6 }) { "stat stage values must be between -6 and 6" }
		require(protectionChain >= 0) { "protectionChain must not be negative" }
		require(badPoisonCounter >= 0) { "badPoisonCounter must not be negative" }
		require(sleepTurnsRemaining >= 0) { "sleepTurnsRemaining must not be negative" }
		require(majorStatus == BattleMajorStatus.SLEEP || sleepTurnsRemaining == 0) {
			"sleepTurnsRemaining must be zero unless participant is asleep"
		}
		require(majorStatus != BattleMajorStatus.SLEEP || sleepTurnsRemaining > 0) {
			"sleepTurnsRemaining must be positive when participant is asleep"
		}
		require(confusionTurnsRemaining >= 0) { "confusionTurnsRemaining must not be negative" }
		require(lockedMoveSkillId == null || lockedMoveSkillId > 0) { "lockedMoveSkillId must be positive when present" }
		require(lockedMoveTargetActorId == null || lockedMoveTargetActorId.isNotBlank()) {
			"lockedMoveTargetActorId must not be blank when present"
		}
		require(lockedMoveTurnsRemaining >= 0) { "lockedMoveTurnsRemaining must not be negative" }
		require(lockedMoveTurnsRemaining == 0 || lockedMoveSkillId != null) {
			"lockedMoveSkillId must be present while locked move remains"
		}
		require(lockedMoveTurnsRemaining == 0 || lockedMoveTargetActorId != null) {
			"lockedMoveTargetActorId must be present while locked move remains"
		}
		require(lockedMoveTurnsRemaining > 0 || !lockedMoveConfusesOnEnd) {
			"lockedMoveConfusesOnEnd must be false when no locked move remains"
		}
	}

	/**
	 * 判断成员是否仍可继续战斗。
	 */
	fun canBattle(): Boolean = currentHp > 0

	/**
	 * 按伤害值扣除 HP，并把结果夹取到 0。
	 */
	fun receiveDamage(amount: Int): BattleParticipant {
		require(amount >= 0) { "damage amount must not be negative" }
		return copy(currentHp = (currentHp - amount).coerceAtLeast(0))
	}

	/**
	 * 回复 HP，并把结果夹取到最大 HP。
	 */
	fun heal(amount: Int): BattleParticipant {
		require(amount >= 0) { "heal amount must not be negative" }
		return copy(currentHp = (currentHp + amount).coerceAtMost(maxHp))
	}

	/**
	 * 替换一个技能槽，通常用于 PP 消耗。
	 */
	fun replaceSkillSlot(slot: BattleSkillSlot): BattleParticipant =
		copy(skillSlots = skillSlots.map { current -> if (current.skillId == slot.skillId) slot else current })

	/**
	 * 查找本成员可使用的技能槽。
	 */
	fun skillSlot(skillId: Long): BattleSkillSlot? =
		skillSlots.firstOrNull { it.skillId == skillId }

	/**
	 * 附加主要异常状态。
	 *
	 * 第一批不允许覆盖已有主要异常状态；调用方应在事件层决定是否记录失败原因。睡眠状态使用
	 * `sleepTurnsRemaining` 表达还会阻止行动几次，而不是保存历史版本的原始睡眠计数。
	 */
	fun applyMajorStatus(status: BattleMajorStatus, sleepTurnsRemaining: Int = 0): BattleParticipant {
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
	fun consumeSleepBlockedTurn(): BattleParticipant =
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
	fun clearMajorStatus(): BattleParticipant =
		if (majorStatus == null) {
			this
		} else {
			copy(majorStatus = null, badPoisonCounter = 0, sleepTurnsRemaining = 0)
		}

	/**
	 * 附加临时状态。
	 *
	 * 畏缩只持续到本回合行动前或回合末，因此不需要额外计数。混乱使用公开实现中的内部计数：
	 * 成员行动前先递减一次，递减后为 0 则解除并照常行动，否则再进行混乱自伤判定。
	 */
	fun applyVolatileStatus(status: BattleVolatileStatus, confusionTurnsRemaining: Int = 0): BattleParticipant {
		require(status == BattleVolatileStatus.CONFUSION || confusionTurnsRemaining == 0) {
			"confusionTurnsRemaining can only be set for confusion"
		}
		require(status != BattleVolatileStatus.CONFUSION || confusionTurnsRemaining > 0) {
			"confusionTurnsRemaining must be positive for confusion"
		}
		return when (status) {
			BattleVolatileStatus.FLINCH -> copy(flinched = true)
			BattleVolatileStatus.CONFUSION -> if (this.confusionTurnsRemaining > 0) {
				this
			} else {
				copy(confusionTurnsRemaining = confusionTurnsRemaining)
			}
		}
	}

	/**
	 * 消耗一次畏缩阻止行动。
	 *
	 * 畏缩阻止本次行动后立即消失；如果成员本回合没有行动，回合末也会被静默清理。
	 */
	fun consumeFlinch(): BattleParticipant =
		if (flinched) copy(flinched = false) else this

	/**
	 * 混乱行动前递减一次内部计数。
	 */
	fun decrementConfusionBeforeMove(): BattleParticipant =
		if (confusionTurnsRemaining > 0) {
			copy(confusionTurnsRemaining = confusionTurnsRemaining - 1)
		} else {
			this
		}

	/**
	 * 清除指定临时状态。
	 */
	fun clearVolatileStatus(status: BattleVolatileStatus): BattleParticipant =
		when (status) {
			BattleVolatileStatus.FLINCH -> if (flinched) copy(flinched = false) else this
			BattleVolatileStatus.CONFUSION -> if (confusionTurnsRemaining > 0) copy(confusionTurnsRemaining = 0) else this
		}

	/**
	 * 开始锁定连续使用某个技能。
	 *
	 * `turnsRemainingAfterCurrent` 只记录未来还会被强制行动几次；首次使用的当前回合不保存在计数中。
	 */
	fun startLockedMove(
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
	fun consumeLockedMoveTurn(): BattleParticipant =
		when {
			lockedMoveTurnsRemaining > 1 -> copy(lockedMoveTurnsRemaining = lockedMoveTurnsRemaining - 1)
			lockedMoveTurnsRemaining == 1 -> clearLockedMove()
			else -> this
		}

	/**
	 * 清除锁招运行态。
	 */
	fun clearLockedMove(): BattleParticipant =
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

	/**
	 * 清理回合结束时不会跨回合保留的临时状态。
	 */
	fun clearEndTurnVolatileStatuses(): BattleParticipant =
		if (flinched) copy(flinched = false) else this

	/**
	 * 改变一个能力阶级，并夹取到现代规则允许的 -6..6。
	 */
	fun changeStatStage(stat: BattleStat, delta: Int): BattleParticipant {
		require(delta != 0) { "delta must not be zero" }
		val next = ((statStages[stat] ?: 0) + delta).coerceIn(-6, 6)
		return copy(statStages = statStages + (stat to next))
	}

	/**
	 * 读取指定能力项当前阶级。
	 */
	fun statStage(stat: BattleStat): Int =
		statStages[stat] ?: 0

	/**
	 * 标记本成员本回合成功使用保护类行动。
	 *
	 * `protectionChain` 表示连续成功保护次数，用于下一次保护类行动计算递减成功率。
	 */
	fun markProtectionSuccess(): BattleParticipant =
		copy(protectionChain = protectionChain + 1)

	/**
	 * 清空连续保护计数。
	 *
	 * 只要本成员在一个回合内没有成功建立保护屏障，回合末就应调用该函数，确保下一次保护重新从必定成功开始。
	 */
	fun resetProtectionChain(): BattleParticipant =
		if (protectionChain == 0) this else copy(protectionChain = 0)

	/**
	 * 推进剧毒计数。
	 *
	 * 剧毒伤害在每次回合末结算后递增一次。若当前成员不是剧毒状态，则保持原样，避免普通中毒误用计数。
	 */
	fun advanceBadPoisonCounter(): BattleParticipant =
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
	 * 畏缩和混乱属于临时状态，离场时会被清除。后续接入锁招、束缚、寄生等离场即消失的状态时，
	 * 也应在这里统一清理。
	 */
	fun leaveBattlefield(): BattleParticipant =
		copy(
			statStages = emptyMap(),
			protectionChain = 0,
			badPoisonCounter = if (majorStatus == BattleMajorStatus.BAD_POISON) 1 else 0,
			flinched = false,
			confusionTurnsRemaining = 0,
			lockedMoveSkillId = null,
			lockedMoveTargetActorId = null,
			lockedMoveTurnsRemaining = 0,
			lockedMoveConfusesOnEnd = false,
		)
}
