package io.github.lishangbu.battleengine.model

/**
 * 战斗成员在本场战斗中可使用的一格技能。
 *
 * 该对象是战斗初始状态的一部分，已经合并了基础技能资料和规则快照所需的最小执行字段：
 * 属性、伤害类别、威力、命中、击中要害等级、保护交互、优先度和 PP。它不保存技能文本说明，
 * 也不保存数据库实体引用对象。
 *
 * `power` 和 `accuracy` 允许为空，用于表达变化技能、必中技能或由特殊策略决定数值的技能。
 * `targetScope` 表示技能在站位中的目标范围，供双打范围技能计算实际目标和 0.75 伤害修正。
 * `minHits`/`maxHits` 表示一次技能使用中的连续命中段数；单段技能二者都为 1，多段技能会在命中后决定段数。
 * `criticalHitStage` 表示进入现代击中要害概率表前的技能侧基础等级，0 为普通技能，3 及以上视为必定要害。
 * `protectsUser` 表示该技能在本回合为使用者建立保护屏障；`affectedByProtect` 表示该技能命中目标时会被
 * 目标的保护屏障阻挡。两者拆开建模，是为了后续支持佯攻、Z 类强化效果、范围技能和穿透保护的特殊技能。
 * `thawsUserBeforeMove` 表示该技能允许冰冻中的使用者发动，并在行动前解除自身冰冻。
 * `soundBased` 表示声音类技能，现代规则中这类技能可以穿过替身影响目标。
 * `powderBased` 表示粉末/孢子类技能，草属性目标会天然免疫这类技能。
 * `weakenedByGrassyTerrain` 表示该技能属于会被青草场地削弱的地面震动类技能。
 * `accuracyOverridesByWeather` 表示指定天气下的命中覆盖值，值为 null 表示该天气下必中；
 * `powerMultipliersByWeather` 表示指定天气下参与普通伤害公式前的威力倍率。
 * `lockMoveTurnsMin`/`lockMoveTurnsMax` 表示使用后会锁定连续使用的总回合数，包含当前首次使用回合；
 * `confusesUserAfterLock` 表示锁定结束后使用者会进入混乱。
 * `sideConditionApplications` 表示技能命中后建立的一侧防守屏障效果，例如物理屏障或特殊屏障。
 * `sideSpeedModifierApplications` 表示技能命中后建立的一侧速度结算效果，例如顺风。
 * `sideEntryHazardApplications` 表示技能命中后建立在一侧、等待后续成员换入时触发的入场陷阱效果。
 * `fieldSpeedOrderApplications` 表示技能命中后建立的全场速度顺序效果，例如戏法空间。
 * `chargesBeforeUse` 表示技能首次使用时先进入蓄力，下一次技能行动才真正释放效果；
 * `chargeSkippedByWeathers` 表示指定天气下该蓄力等待可以省略，技能会在宣告回合直接进入命中和伤害流程。
 * `hpEffects` 表示技能成功后直接改变 HP 的效果，例如吸取回复、反作用伤害或自我回复。
 * `rechargesAfterUse` 表示技能成功造成实际伤害后，使用者下一次技能行动前必须休整一次。
 * `environmentEffects` 表示技能成功后直接改写全场环境的效果，例如设置天气。
 *
 * 第一阶段普通伤害公式只处理带威力的物理/特殊技能；特殊技能效果会继续通过显式规则对象扩展。
 * 当前显式支持主要异常状态、临时状态、能力阶级变化、HP 变化和一侧场上效果，避免用弱类型脚本描述核心规则。
 */
data class BattleSkillSlot(
	val skillId: Long,
	val name: String,
	val elementId: Long,
	val damageClass: BattleDamageClass,
	val power: Int?,
	val accuracy: Int?,
	val targetScope: BattleSkillTargetScope = BattleSkillTargetScope.SELECTED_TARGET,
	val minHits: Int = 1,
	val maxHits: Int = 1,
	val makesContact: Boolean = false,
	val criticalHitStage: Int = 0,
	val affectedByProtect: Boolean = true,
	val protectsUser: Boolean = false,
	val thawsUserBeforeMove: Boolean = false,
	val soundBased: Boolean = false,
	val powderBased: Boolean = false,
	val weakenedByGrassyTerrain: Boolean = false,
	val chargesBeforeUse: Boolean = false,
	val chargeSkippedByWeathers: Set<BattleWeather> = emptySet(),
	val rechargesAfterUse: Boolean = false,
	val accuracyOverridesByWeather: Map<BattleWeather, Int?> = emptyMap(),
	val powerMultipliersByWeather: Map<BattleWeather, Double> = emptyMap(),
	val lockMoveTurnsMin: Int = 1,
	val lockMoveTurnsMax: Int = 1,
	val confusesUserAfterLock: Boolean = false,
	val priority: Int = 0,
	val remainingPp: Int,
	val maxPp: Int,
	val statusApplications: List<BattleStatusApplication> = emptyList(),
	val volatileStatusApplications: List<BattleVolatileStatusApplication> = emptyList(),
	val statStageEffects: List<BattleStatStageEffect> = emptyList(),
	val sideConditionApplications: List<BattleSideConditionApplication> = emptyList(),
	val sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication> = emptyList(),
	val sideEntryHazardApplications: List<BattleSideEntryHazardApplication> = emptyList(),
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication> = emptyList(),
	val hpEffects: List<BattleSkillHpEffect> = emptyList(),
	val environmentEffects: List<BattleSkillEnvironmentEffect> = emptyList(),
) {
	init {
		require(skillId > 0) { "skillId must be positive" }
		require(name.isNotBlank()) { "skill name must not be blank" }
		require(elementId > 0) { "elementId must be positive" }
		require(power == null || power > 0) { "power must be positive when present" }
		require(accuracy == null || accuracy in 1..100) { "accuracy must be between 1 and 100 when present" }
		require(minHits > 0) { "minHits must be positive" }
		require(maxHits >= minHits) { "maxHits must be greater than or equal to minHits" }
		require(damageClass != BattleDamageClass.STATUS || (minHits == 1 && maxHits == 1)) {
			"status skill cannot use multi-hit settings"
		}
		require(accuracyOverridesByWeather.keys.none { it == BattleWeather.NONE }) {
			"accuracyOverridesByWeather cannot target NONE"
		}
		require(accuracyOverridesByWeather.values.all { it == null || it in 1..100 }) {
			"weather accuracy override must be null or between 1 and 100"
		}
		require(powerMultipliersByWeather.keys.none { it == BattleWeather.NONE }) {
			"powerMultipliersByWeather cannot target NONE"
		}
		require(powerMultipliersByWeather.values.all { it > 0.0 }) {
			"weather power multiplier must be positive"
		}
		require(BattleWeather.NONE !in chargeSkippedByWeathers) {
			"chargeSkippedByWeathers cannot target NONE"
		}
		require(chargeSkippedByWeathers.isEmpty() || chargesBeforeUse) {
			"chargeSkippedByWeathers requires chargesBeforeUse"
		}
		require(lockMoveTurnsMin > 0) { "lockMoveTurnsMin must be positive" }
		require(lockMoveTurnsMax >= lockMoveTurnsMin) { "lockMoveTurnsMax must be greater than or equal to lockMoveTurnsMin" }
		require(!confusesUserAfterLock || lockMoveTurnsMax > 1) {
			"confusesUserAfterLock requires a locking move"
		}
		require(criticalHitStage >= 0) { "criticalHitStage must not be negative" }
		require(!protectsUser || damageClass == BattleDamageClass.STATUS) { "protect skill must be a status skill" }
		require(remainingPp in 0..maxPp) { "remainingPp must be between 0 and maxPp" }
		require(maxPp >= 0) { "maxPp must not be negative" }
	}

	/**
	 * 返回消耗 1 点 PP 后的技能槽。
	 *
	 * 调用方必须先检查 `remainingPp`，这里仍保留 require 作为状态机不变量保护。
	 */
	fun consumePp(): BattleSkillSlot {
		require(remainingPp > 0) { "skill has no remaining PP" }
		return copy(remainingPp = remainingPp - 1)
	}
}
