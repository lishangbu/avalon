package io.github.lishangbu.battleengine.model

/**
 * 现代主系列规则中单个成员最多可携带的技能数量。
 */
const val MAX_BATTLE_SKILL_SLOTS = 4

/**
 * 一名参与战斗的成员快照。
 *
 * 成员保存战斗结算需要的当前运行态：HP、等级、五项战斗能力、属性集合、技能槽、特性/道具身份、
 * 是否接地、连续保护计数、剧毒计数、睡眠剩余阻止行动次数、技能蓄力计数、技能休整计数、技能锁招运行态、
 * 讲究类道具锁定技能，以及畏缩/混乱等临时状态。
 * 它不直接包含种类、训练者、背包或数据库实体；这些资料应在进入引擎前转换成稳定数值。
 *
 * 第一阶段状态不变量：
 * - `currentHp` 必须位于 `0..maxHp`。
 * - 可行动成员必须拥有 1 到 4 个技能槽，且同一成员内技能不能重复。
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
	val chargingSkillId: Long? = null,
	val chargingTargetActorId: String? = null,
	val chargingTurnsRemaining: Int = 0,
	val rechargeTurnsRemaining: Int = 0,
	val flinched: Boolean = false,
	val confusionTurnsRemaining: Int = 0,
	val lockedMoveSkillId: Long? = null,
	val lockedMoveTargetActorId: String? = null,
	val lockedMoveTurnsRemaining: Int = 0,
	val lockedMoveConfusesOnEnd: Boolean = false,
	val abilityEffects: List<BattleAbilityEffect> = emptyList(),
	val itemEffects: List<BattleItemEffect> = emptyList(),
	val choiceLockedSkillId: Long? = null,
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
		require(skillSlots.size <= MAX_BATTLE_SKILL_SLOTS) {
			"skillSlots must contain at most $MAX_BATTLE_SKILL_SLOTS skills"
		}
		require(skillSlots.map { it.skillId }.toSet().size == skillSlots.size) { "skillSlots must not contain duplicate skill ids" }
		require(abilityId == null || abilityId > 0) { "abilityId must be positive when present" }
		require(itemId == null || itemId > 0) { "itemId must be positive when present" }
		require(statStages.values.all { it in -6..6 }) { "stat stage values must be between -6 and 6" }
		require(protectionChain >= 0) { "protectionChain must not be negative" }
		require(badPoisonCounter >= 0) { "badPoisonCounter must not be negative" }
		require(sleepTurnsRemaining >= 0) { "sleepTurnsRemaining must not be negative" }
		require(chargingSkillId == null || chargingSkillId > 0) { "chargingSkillId must be positive when present" }
		require(chargingTargetActorId == null || chargingTargetActorId.isNotBlank()) {
			"chargingTargetActorId must not be blank when present"
		}
		require(chargingTurnsRemaining >= 0) { "chargingTurnsRemaining must not be negative" }
		require(rechargeTurnsRemaining >= 0) { "rechargeTurnsRemaining must not be negative" }
		require(majorStatus == BattleMajorStatus.SLEEP || sleepTurnsRemaining == 0) {
			"sleepTurnsRemaining must be zero unless participant is asleep"
		}
		require(majorStatus != BattleMajorStatus.SLEEP || sleepTurnsRemaining > 0) {
			"sleepTurnsRemaining must be positive when participant is asleep"
		}
		require(chargingTurnsRemaining == 0 || chargingSkillId != null) {
			"chargingSkillId must be present while charging turns remain"
		}
		require(chargingTurnsRemaining == 0 || chargingTargetActorId != null) {
			"chargingTargetActorId must be present while charging turns remain"
		}
		require(chargingTurnsRemaining > 0 || (chargingSkillId == null && chargingTargetActorId == null)) {
			"charging skill state must be cleared when no charging turns remain"
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
		require(chargingTurnsRemaining == 0 || lockedMoveTurnsRemaining == 0) {
			"participant cannot be charging and locked into a move at the same time"
		}
		require(choiceLockedSkillId == null || choiceLockedSkillId > 0) {
			"choiceLockedSkillId must be positive when present"
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
	 * 消费当前携带道具。
	 *
	 * 第一批道具生命周期只需要表达“一次性触发后不再拥有该道具效果”。道具 ID 和所有道具效果一起清空，
	 * 因为成员快照只允许一个携带道具；后续如果要支持道具被替换、回收或禁用而不移除，会增加更细的状态字段。
	 */
	fun consumeHeldItem(): BattleParticipant =
		copy(itemId = null, itemEffects = emptyList(), choiceLockedSkillId = null)

	/**
	 * 按讲究类道具规则记录首次成功宣告的技能。
	 *
	 * 只有携带对应道具效果且尚未锁定时才会写入。已经锁定的成员保持原技能，避免一次异常行动覆盖既有选择。
	 */
	fun lockChoiceSkillIfNeeded(skillId: Long): BattleParticipant {
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
	fun choiceLockedToAnotherSkill(skillId: Long): Boolean {
		require(skillId > 0) { "skillId must be positive" }
		return choiceLockedSkillId != null && choiceLockedSkillId != skillId
	}

	/**
	 * 查找本成员可使用的技能槽。
	 */
	fun skillSlot(skillId: Long): BattleSkillSlot? =
		skillSlots.firstOrNull { it.skillId == skillId }

	/**
	 * 附加主要异常状态。
	 *
	 * 主要异常状态不会覆盖已有主要异常状态；调用方会在进入这里前记录对应阻止事件。睡眠状态使用
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
	 * 标记成员进入技能休整状态。
	 *
	 * 现代规则中，部分强力技能在成功造成伤害后会让使用者下一次技能行动前休整一次。该计数只保存“未来还会
	 * 阻止几次技能行动”，当前成功使用技能的回合不计入。若成员已经处于休整状态，调用方不应重复叠加。
	 */
	fun startRecharge(turnsRemainingAfterCurrent: Int = 1): BattleParticipant {
		require(turnsRemainingAfterCurrent > 0) { "turnsRemainingAfterCurrent must be positive" }
		return copy(rechargeTurnsRemaining = turnsRemainingAfterCurrent)
	}

	/**
	 * 消耗一次技能休整阻止行动。
	 */
	fun consumeRechargeTurn(): BattleParticipant =
		when {
			rechargeTurnsRemaining > 1 -> copy(rechargeTurnsRemaining = rechargeTurnsRemaining - 1)
			rechargeTurnsRemaining == 1 -> copy(rechargeTurnsRemaining = 0)
			else -> this
		}

	/**
	 * 标记成员进入技能蓄力状态。
	 *
	 * 该状态表示本次技能已经支付 PP 并完成宣告，但真正效果要在未来技能行动中释放。`turnsRemainingBeforeUse`
	 * 保存未来还要等待几次技能行动；现代常见蓄力技能为 1。目标保存为首次选择的目标槽位，以便下一回合复用
	 * 现有目标重定向规则。
	 */
	fun startChargingSkill(
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
	fun consumeChargingTurn(): BattleParticipant =
		when {
			chargingTurnsRemaining > 1 -> copy(chargingTurnsRemaining = chargingTurnsRemaining - 1)
			chargingTurnsRemaining == 1 -> clearChargingSkill()
			else -> this
		}

	/**
	 * 清除技能蓄力运行态。
	 */
	fun clearChargingSkill(): BattleParticipant =
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
			chargingSkillId = null,
			chargingTargetActorId = null,
			chargingTurnsRemaining = 0,
			rechargeTurnsRemaining = 0,
			flinched = false,
			confusionTurnsRemaining = 0,
			lockedMoveSkillId = null,
			lockedMoveTargetActorId = null,
			lockedMoveTurnsRemaining = 0,
			lockedMoveConfusesOnEnd = false,
			choiceLockedSkillId = null,
		)
}
