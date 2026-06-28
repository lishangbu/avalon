package io.github.lishangbu.battleengine.model

/**
 * 一名参与战斗的成员快照。
 *
 * 成员保存战斗结算需要的当前运行态：HP、等级、五项战斗能力、属性集合、技能槽、连续保护计数和剧毒计数。
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
	val majorStatus: BattleMajorStatus? = null,
	val statStages: Map<BattleStat, Int> = emptyMap(),
	val protectionChain: Int = 0,
	val badPoisonCounter: Int = 0,
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
		require(statStages.values.all { it in -6..6 }) { "stat stage values must be between -6 and 6" }
		require(protectionChain >= 0) { "protectionChain must not be negative" }
		require(badPoisonCounter >= 0) { "badPoisonCounter must not be negative" }
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
	 * 第一批不允许覆盖已有主要异常状态；调用方应在事件层决定是否记录失败原因。
	 */
	fun applyMajorStatus(status: BattleMajorStatus): BattleParticipant =
		if (majorStatus == null) {
			copy(
				majorStatus = status,
				badPoisonCounter = if (status == BattleMajorStatus.BAD_POISON) 1 else 0,
			)
		} else {
			this
		}

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
	 * 剧毒状态会保留，但剧毒递增计数回到 1。后续接入更细的易失状态系统时，混乱、畏缩、锁招等离场即消失
	 * 的状态也应在这里统一清理。
	 */
	fun leaveBattlefield(): BattleParticipant =
		copy(
			statStages = emptyMap(),
			protectionChain = 0,
			badPoisonCounter = if (majorStatus == BattleMajorStatus.BAD_POISON) 1 else 0,
		)
}
