package io.github.lishangbu.battleengine.model

/**
 * 现代主系列规则中单个成员最多可携带的技能数量。
 */
const val MAX_BATTLE_SKILL_SLOTS = 4

/**
 * 一名参与战斗的成员快照。
 *
 * 成员保存战斗结算需要的当前运行态：HP、等级、五项战斗能力、属性集合、技能槽、特性/道具身份、
 * 体重、临时体重减轻量、是否接地、本次上场后技能行动尝试次数、连续保护计数、本回合挺住来源技能、
 * 剧毒计数、睡眠剩余阻止行动次数、
 * 技能蓄力计数、技能休整计数、技能锁招运行态、命中锁定目标、讲究类道具锁定技能、替身剩余 HP，以及畏缩、
 * 混乱、回复封锁、挑衅、定身法、无理取闹、束缚、寄生种子等临时状态。
 * 它不直接包含种类、训练者、背包或数据库实体；这些资料应在进入引擎前转换成稳定数值。
 *
 * 成员快照状态不变量：
 * - `currentHp` 必须位于 `0..maxHp`。
 * - 可行动成员必须拥有 1 到 4 个技能槽，且同一成员内技能不能重复。
 * - 攻击、防御、特攻、特防、速度必须为正数，避免公式除零或负速度排序。
 * - 体重必须为正数，并且沿用资料表的整数刻度；体重相关动态威力会直接读取该值，不在公式层再做单位换算。
 * - 临时体重减轻量不能为负数；有效体重由基础体重减去该值后再叠加特性/道具倍率，且最低不会低于资料刻度 1。
 */
data class BattleParticipant(
	val actorId: String,
	val creatureId: Long,
	val canEvolve: Boolean = false,
	val level: Int,
	val maxHp: Int,
	val currentHp: Int,
	val attack: Int,
	val defense: Int,
	val specialAttack: Int,
	val specialDefense: Int,
	val speed: Int,
	val weight: Int,
	val weightReduction: Int = 0,
	val elementIds: Set<Long>,
	val skillSlots: List<BattleSkillSlot>,
	val abilityId: Long? = null,
	val itemId: Long? = null,
	val itemLostSinceEntering: Boolean = false,
	val grounded: Boolean = true,
	val activeSkillActionCount: Int = 0,
	val majorStatus: BattleMajorStatus? = null,
	val natureDecreasedStat: BattleStat? = null,
	val nextSkillAccuracyMultiplier: Double = 1.0,
	val boosterEnergyStat: BattleStat? = null,
	val statStages: Map<BattleStat, Int> = emptyMap(),
	val criticalHitStageBonus: Int = 0,
	val protectionChain: Int = 0,
	val fatalDamageEndureSkillId: Long? = null,
	val badPoisonCounter: Int = 0,
	val sleepTurnsRemaining: Int = 0,
	val chargingSkillId: Long? = null,
	val chargingTargetActorId: String? = null,
	val chargingTurnsRemaining: Int = 0,
	val rechargeTurnsRemaining: Int = 0,
	val flinched: Boolean = false,
	val confusionTurnsRemaining: Int = 0,
	val healBlockTurnsRemaining: Int = 0,
	val tauntTurnsRemaining: Int = 0,
	val disabledSkillId: Long? = null,
	val disabledSkillTurnsRemaining: Int = 0,
	val tormented: Boolean = false,
	val infatuatedByActorId: String? = null,
	val boundByActorId: String? = null,
	val bindingTurnsRemaining: Int = 0,
	val leechSeedSourceSideId: String? = null,
	val leechSeedSourceActiveIndex: Int? = null,
	val lastSuccessfulSkillId: Long? = null,
	val consecutiveSuccessfulSkillUses: Int = 0,
	val accuracyLockTargetActorId: String? = null,
	val accuracyLockTurnsRemaining: Int = 0,
	val lockedMoveSkillId: Long? = null,
	val lockedMoveTargetActorId: String? = null,
	val lockedMoveTurnsRemaining: Int = 0,
	val lockedMoveConfusesOnEnd: Boolean = false,
	val abilityEffects: List<BattleAbilityEffect> = emptyList(),
	val itemEffects: List<BattleItemEffect> = emptyList(),
	val choiceLockedSkillId: Long? = null,
	val substituteHp: Int = 0,
	/** 进入战斗时冻结的原始属性，用于太晶化后的属性一致加成。 */
	val originalElementIds: Set<Long> = elementIds,
	val teraElementId: Long? = null,
	val terastallized: Boolean = false,
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
		require(weight > 0) { "weight must be positive" }
		require(weightReduction >= 0) { "weightReduction must not be negative" }
		require(elementIds.all { it > 0 }) { "elementIds must contain only positive ids" }
		require(originalElementIds.isNotEmpty() && originalElementIds.all { it > 0 }) { "originalElementIds must contain positive ids" }
		require(teraElementId == null || teraElementId > 0) { "teraElementId must be positive when present" }
		require(!terastallized || (teraElementId != null && elementIds == setOf(teraElementId))) {
			"terastallized participant must use only its tera element"
		}
		require(skillSlots.isNotEmpty()) { "skillSlots must not be empty" }
		require(skillSlots.size <= MAX_BATTLE_SKILL_SLOTS) {
			"skillSlots must contain at most $MAX_BATTLE_SKILL_SLOTS skills"
		}
		require(skillSlots.map { it.skillId }.toSet().size == skillSlots.size) { "skillSlots must not contain duplicate skill ids" }
		require(abilityId == null || abilityId > 0) { "abilityId must be positive when present" }
		require(itemId == null || itemId > 0) { "itemId must be positive when present" }
		require(activeSkillActionCount >= 0) { "activeSkillActionCount must not be negative" }
		require(nextSkillAccuracyMultiplier > 0.0) { "nextSkillAccuracyMultiplier must be positive" }
		require(statStages.values.all { it in -6..6 }) { "stat stage values must be between -6 and 6" }
		require(criticalHitStageBonus >= 0) { "criticalHitStageBonus must not be negative" }
		require(protectionChain >= 0) { "protectionChain must not be negative" }
		require(fatalDamageEndureSkillId == null || fatalDamageEndureSkillId > 0) {
			"fatalDamageEndureSkillId must be positive when present"
		}
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
		require(healBlockTurnsRemaining >= 0) { "healBlockTurnsRemaining must not be negative" }
		require(tauntTurnsRemaining >= 0) { "tauntTurnsRemaining must not be negative" }
		require(disabledSkillId == null || disabledSkillId > 0) { "disabledSkillId must be positive when present" }
		require(disabledSkillTurnsRemaining >= 0) { "disabledSkillTurnsRemaining must not be negative" }
		require(disabledSkillTurnsRemaining == 0 || disabledSkillId != null) {
			"disabledSkillId must be present while disable remains"
		}
		require(disabledSkillTurnsRemaining > 0 || disabledSkillId == null) {
			"disabledSkillId must be cleared when disable has no remaining turns"
		}
		require(lastSuccessfulSkillId == null || lastSuccessfulSkillId > 0) {
			"lastSuccessfulSkillId must be positive when present"
		}
		require(infatuatedByActorId == null || infatuatedByActorId.isNotBlank()) {
			"infatuatedByActorId must not be blank when present"
		}
		require(consecutiveSuccessfulSkillUses >= 0) { "consecutiveSuccessfulSkillUses must not be negative" }
		require(accuracyLockTargetActorId == null || accuracyLockTargetActorId.isNotBlank()) {
			"accuracyLockTargetActorId must not be blank when present"
		}
		require(accuracyLockTurnsRemaining >= 0) { "accuracyLockTurnsRemaining must not be negative" }
		require(accuracyLockTurnsRemaining == 0 || accuracyLockTargetActorId != null) {
			"accuracyLockTargetActorId must be present while accuracy lock remains"
		}
		require(accuracyLockTurnsRemaining > 0 || accuracyLockTargetActorId == null) {
			"accuracy lock target must be cleared when no accuracy lock remains"
		}
		require(boundByActorId == null || boundByActorId.isNotBlank()) {
			"boundByActorId must not be blank when present"
		}
		require(bindingTurnsRemaining >= 0) { "bindingTurnsRemaining must not be negative" }
		require(bindingTurnsRemaining == 0 || boundByActorId != null) {
			"boundByActorId must be present while binding remains"
		}
		require(bindingTurnsRemaining > 0 || boundByActorId == null) {
			"boundByActorId must be cleared when binding has no remaining turns"
		}
		require(leechSeedSourceSideId == null || leechSeedSourceSideId.isNotBlank()) {
			"leechSeedSourceSideId must not be blank when present"
		}
		require(leechSeedSourceActiveIndex == null || leechSeedSourceActiveIndex >= 0) {
			"leechSeedSourceActiveIndex must not be negative when present"
		}
		require((leechSeedSourceSideId == null) == (leechSeedSourceActiveIndex == null)) {
			"leech seed source side and active index must be present together"
		}
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
		require(substituteHp in 0 until maxHp) {
			"substituteHp must be non-negative and less than maxHp"
		}
	}

}
