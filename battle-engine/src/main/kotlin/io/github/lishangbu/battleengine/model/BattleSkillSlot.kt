package io.github.lishangbu.battleengine.model

/**
 * 战斗成员在本场战斗中可使用的一格技能。
 *
 * 该对象是战斗初始状态的一部分，已经合并了基础技能资料和规则快照所需的最小执行字段：
 * 属性、伤害类别、威力、命中、击中要害等级、保护交互、优先度和 PP。它不保存技能文本说明，
 * 也不保存数据库实体引用对象。
 *
 * `power` 和 `accuracy` 允许为空，用于表达变化技能、必中技能或由特殊策略决定数值的技能。
 * `fixedDamage` 表示命中后使用固定伤害口径，不进入普通物理/特殊伤害公式，也不消费要害或伤害随机数。
 * `proportionalDamage` 表示命中后按目标当前 HP 比例扣血，也不进入普通伤害公式。
 * `hpDerivedDamage` 表示命中后按战斗双方当前 HP 推导直接伤害或自我倒下代价。
 * `oneHitKnockOut` 表示命中后直接造成目标当前 HP 等量伤害，并使用一击必杀专用等级与命中率规则。
 * `targetScope` 表示技能在站位中的目标范围，供双打范围技能计算实际目标和 0.75 伤害修正。
 * `minHits`/`maxHits` 表示一次技能使用中的连续命中段数；单段技能二者都为 1，多段技能会在命中后决定段数。
 * `criticalHitStage` 表示进入现代击中要害概率表前的技能侧基础等级，0 为普通技能，3 及以上视为必定要害。
 * `protectsUser` 表示该技能在本回合为使用者建立保护屏障；`affectedByProtect` 表示该技能命中目标时会被
 * 目标的保护屏障阻挡。两者拆开建模，是为了后续支持佯攻、Z 类强化效果、范围技能和穿透保护的特殊技能。
 * `thawsUserBeforeMove` 表示该技能允许冰冻中的使用者发动，并在行动前解除自身冰冻。
 * `soundBased` 表示声音类技能，现代规则中这类技能可以穿过替身影响目标。
 * `powderBased` 表示粉末/孢子类技能，草属性目标会天然免疫这类技能。
 * `punchBased` 表示拳击类技能，攻击方拥有对应特性时会在伤害公式中获得威力倍率。
 * `slicingBased` 表示切割类技能，攻击方拥有对应特性时会在伤害公式中获得威力倍率。
 * `weakenedByGrassyTerrain` 表示该技能属于会被青草场地削弱的地面震动类技能。
 * `accuracyOverridesByWeather` 表示指定天气下的命中覆盖值，值为 null 表示该天气下必中；
 * `powerMultipliersByWeather` 表示指定天气下参与普通伤害公式前的威力倍率。
 * `groundedPowerMultipliersByTerrain` 表示使用者接地且指定场地存在时，参与普通伤害公式前的威力倍率；
 * 这个字段刻意把“接地”写进名称，因为现代场地规则通常只影响接触地面的成员，和只要求场地存在的技能规则不同。
 * `conditionalPowerMultipliers` 表示按使用者状态、目标状态、目标 HP 或使用者道具状态触发的公式前威力倍率。
 * `dynamicPower` 表示本次伤害公式使用的基础威力需要从战斗快照推导，例如读取能力阶级总和。
 * `elementOverridesByWeather` 表示指定天气下技能本次结算使用的属性覆盖，例如气象球在晴天变为火属性。
 * `elementOverridesByTerrain` 表示指定场地下技能本次结算使用的属性覆盖，例如场地脉冲在电气场地变为电属性。
 * `ignoresUserBurnAttackReduction` 表示该物理技能在使用者灼伤时仍使用正常攻击值，例如硬撑。
 * `lockMoveTurnsMin`/`lockMoveTurnsMax` 表示使用后会锁定连续使用的总回合数，包含当前首次使用回合；
 * `confusesUserAfterLock` 表示锁定结束后使用者会进入混乱。
 * `forceTargetSwitch` 表示技能成功命中并完成伤害/附加效果后，会强制目标所属方随机换入一个可战斗后备成员。
 * `groundedTerrainPriorityBoosts` 表示使用者接地且指定场地存在时，技能行动优先度获得的额外提升。
 * `statStageOperations` 表示技能命中后执行的能力阶级清除、复制、交换或取反等结构化操作。
 * `sideConditionApplications` 表示技能命中后建立的一侧防守屏障效果，例如物理屏障或特殊屏障。
 * `sideSpeedModifierApplications` 表示技能命中后建立的一侧速度结算效果，例如顺风。
 * `sideEntryHazardApplications` 表示技能命中后建立在一侧、等待后续成员换入时触发的入场陷阱效果。
 * `fieldSpeedOrderApplications` 表示技能命中后建立的全场速度顺序效果，例如戏法空间。
 * `chargesBeforeUse` 表示技能首次使用时先进入蓄力，下一次技能行动才真正释放效果；
 * `chargeSkippedByWeathers` 表示指定天气下该蓄力等待可以省略，技能会在宣告回合直接进入命中和伤害流程。
 * `hpEffects` 表示技能成功后直接改变 HP 的效果，例如吸取回复、反作用伤害或自我回复。
 * `postDamageStatusCures` 表示技能造成实际伤害后治愈目标指定主要异常的效果。
 * `removesUserElementAfterDamage` 表示技能成功造成伤害后移除使用者当前与技能基础属性相同的属性。
 * `weightEffects` 表示技能成功后对成员当前体重产生的临时修正，例如速度成功提升后降低自身有效体重。
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
	val fixedDamage: BattleFixedDamage? = null,
	val proportionalDamage: BattleProportionalDamage? = null,
	val hpDerivedDamage: BattleHpDerivedDamage? = null,
	val oneHitKnockOut: BattleOneHitKnockOut? = null,
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
	val punchBased: Boolean = false,
	val slicingBased: Boolean = false,
	val weakenedByGrassyTerrain: Boolean = false,
	val chargesBeforeUse: Boolean = false,
	val chargeSkippedByWeathers: Set<BattleWeather> = emptySet(),
	val rechargesAfterUse: Boolean = false,
	val accuracyOverridesByWeather: Map<BattleWeather, Int?> = emptyMap(),
	val powerMultipliersByWeather: Map<BattleWeather, Double> = emptyMap(),
	val groundedPowerMultipliersByTerrain: Map<BattleTerrain, Double> = emptyMap(),
	val conditionalPowerMultipliers: List<BattleSkillPowerMultiplier> = emptyList(),
	val dynamicPower: BattleSkillDynamicPower? = null,
	val elementOverridesByWeather: Map<BattleWeather, Long> = emptyMap(),
	val elementOverridesByTerrain: Map<BattleTerrain, Long> = emptyMap(),
	val ignoresUserBurnAttackReduction: Boolean = false,
	val lockMoveTurnsMin: Int = 1,
	val lockMoveTurnsMax: Int = 1,
	val confusesUserAfterLock: Boolean = false,
	val forceTargetSwitch: Boolean = false,
	val priority: Int = 0,
	val groundedTerrainPriorityBoosts: Map<BattleTerrain, Int> = emptyMap(),
	val remainingPp: Int,
	val maxPp: Int,
	val statusApplications: List<BattleStatusApplication> = emptyList(),
	val volatileStatusApplications: List<BattleVolatileStatusApplication> = emptyList(),
	val statStageEffects: List<BattleStatStageEffect> = emptyList(),
	val statStageOperations: List<BattleStatStageOperation> = emptyList(),
	val sideConditionApplications: List<BattleSideConditionApplication> = emptyList(),
	val sideSpeedModifierApplications: List<BattleSideSpeedModifierApplication> = emptyList(),
	val sideEntryHazardApplications: List<BattleSideEntryHazardApplication> = emptyList(),
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication> = emptyList(),
	val hpEffects: List<BattleSkillHpEffect> = emptyList(),
	val postDamageStatusCures: List<BattleSkillPostDamageStatusCure> = emptyList(),
	val removesUserElementAfterDamage: Boolean = false,
	val weightEffects: List<BattleSkillWeightEffect> = emptyList(),
	val environmentEffects: List<BattleSkillEnvironmentEffect> = emptyList(),
) {
	init {
		require(skillId > 0) { "skillId must be positive" }
		require(name.isNotBlank()) { "skill name must not be blank" }
		require(elementId > 0) { "elementId must be positive" }
		require(power == null || power > 0) { "power must be positive when present" }
		require(fixedDamage == null || damageClass != BattleDamageClass.STATUS) { "fixed damage requires a damaging skill" }
		require(proportionalDamage == null || damageClass != BattleDamageClass.STATUS) {
			"proportional damage requires a damaging skill"
		}
		require(hpDerivedDamage == null || damageClass != BattleDamageClass.STATUS) {
			"HP-derived damage requires a damaging skill"
		}
		require(oneHitKnockOut == null || damageClass != BattleDamageClass.STATUS) {
			"one-hit knock out damage requires a damaging skill"
		}
		val directDamageRuleCount = listOf(fixedDamage, proportionalDamage, hpDerivedDamage, oneHitKnockOut).count { it != null }
		require(directDamageRuleCount <= 1) {
			"direct damage rules cannot be used together"
		}
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
		require(groundedPowerMultipliersByTerrain.keys.none { it == BattleTerrain.NONE }) {
			"groundedPowerMultipliersByTerrain cannot target NONE"
		}
		require(groundedPowerMultipliersByTerrain.values.all { it > 0.0 }) {
			"grounded terrain power multiplier must be positive"
		}
		require(groundedTerrainPriorityBoosts.keys.none { it == BattleTerrain.NONE }) {
			"grounded terrain priority boosts cannot target NONE"
		}
		require(groundedTerrainPriorityBoosts.values.all { it > 0 }) {
			"grounded terrain priority boosts must be positive"
		}
		require(conditionalPowerMultipliers.isEmpty() || (damageClass != BattleDamageClass.STATUS && power != null)) {
			"conditional power multipliers require a damaging skill with base power"
		}
		require(dynamicPower == null || damageClass != BattleDamageClass.STATUS) {
			"dynamic power requires a damaging skill"
		}
		require(!removesUserElementAfterDamage || damageClass != BattleDamageClass.STATUS) {
			"user element removal requires a damaging skill"
		}
		require(elementOverridesByWeather.keys.none { it == BattleWeather.NONE }) {
			"elementOverridesByWeather cannot target NONE"
		}
		require(elementOverridesByWeather.values.all { it > 0 }) {
			"weather element override must be positive"
		}
		require(elementOverridesByTerrain.keys.none { it == BattleTerrain.NONE }) {
			"elementOverridesByTerrain cannot target NONE"
		}
		require(elementOverridesByTerrain.values.all { it > 0 }) {
			"terrain element override must be positive"
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
		require(!ignoresUserBurnAttackReduction || damageClass == BattleDamageClass.PHYSICAL) {
			"burn attack reduction bypass requires a physical skill"
		}
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

	/**
	 * 返回指定天气和场地下本次技能结算应使用的属性 ID。
	 *
	 * 大多数技能始终使用基础属性；天气球类技能会在非无天气下把技能属性改为天气对应属性，场地脉冲类技能会在
	 * 有效场地下把技能属性改为场地对应属性。调用方必须统一使用本函数读取属性，才能让属性一致加成、属性克制、
	 * 属性吸收、火属性解冻和指定属性道具在同一口径下工作。
	 *
	 * 场地覆盖优先于天气覆盖只是为了让显式配置保持确定性；正式资料中不会给同一个技能同时配置两种覆盖来源。
	 */
	fun effectiveElementId(weather: BattleWeather, terrain: BattleTerrain): Long =
		elementOverridesByTerrain[terrain] ?: elementOverridesByWeather[weather] ?: elementId
}
