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
 * `receivedDamage` 表示命中后读取使用者本回合承受的最后一段合格直接伤害，再按配置倍数返还给伤害来源。
 * `oneHitKnockOut` 表示命中后直接造成目标当前 HP 等量伤害，并使用一击必杀专用等级与命中率规则。
 * `targetScope` 表示技能在站位中的目标范围，供双打范围技能计算实际目标和 0.75 伤害修正。
 * `USER_SIDE_ACTIVE` 这类己方范围会在使用者所在侧当前上场成员中解析目标，并把使用者排在第一位，便于技能宣告
 * 事件保持稳定。
 * `minHits`/`maxHits` 表示一次技能使用中的连续命中段数；单段技能二者都为 1，多段技能会在命中后决定段数。
 * `criticalHitStage` 表示进入现代击中要害概率表前的技能侧基础等级，0 为普通技能，3 及以上视为必定要害。
 * `criticalHitStageBoost` 表示变化技能命中成功后给使用者写入的在场期间要害等级加成；聚气使用 +2，加成会和
 * 后续技能自身 [criticalHitStage] 相加。它保存在成员运行态上，离场时清除。
 * `protectsUser` 表示该技能在本回合为使用者建立保护屏障；`enduresFatalDamage` 表示该技能在本回合让使用者
 * 承受致命技能伤害时至少保留 1 HP；`affectedByProtect` 表示该技能命中目标时会被目标的保护屏障阻挡。
 * 三者拆开建模，是为了把守住、挺住、佯攻、范围技能和穿透保护的特殊技能保持为明确的规则标签。
 * `thawsUserBeforeMove` 表示该技能允许冰冻中的使用者发动，并在行动前解除自身冰冻。
 * `soundBased` 表示声音类技能，现代规则中这类技能可以穿过替身影响目标。
 * `bypassesSubstitute` 表示技能可以穿过目标替身影响目标。声音类技能天然走 [soundBased]，本字段用于清除浓雾
 * 这类非声音但明确不被替身拦下的变化技能，避免用技能名称分支污染通用替身判断。
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
 * `defendingStatOverride` 表示普通伤害公式防守侧使用的能力项覆盖。绝大多数物理技能读取防御、特殊技能读取特防；
 * 精神冲击这类特殊技能例外地仍读取目标防御。这里选择只覆盖防守能力项，而不是新增一个完整“公式配置对象”，
 * 是因为现代资料里这组规则只改变防守项选择，攻击项、威力、属性一致加成、属性克制、天气、道具和要害流程都不变。
 * `leavesTargetAtOneHp` 表示该技能造成目标本体伤害时不能把目标 HP 降到 0。它不是满 HP 保命效果，也不是伤害公式
 * 修正：公式仍照常算出原始伤害，真正写入目标本体 HP 前再夹到“当前 HP - 1”。这样点到为止这类技能不会触发满
 * HP 保命特性/道具，也不会让吸取、反伤和造成伤害后回复读取错误的过量伤害。替身承受伤害时不读取这个字段，
 * 因为替身可以被这类技能打破。
 * `breaksTargetSideDamageReductions` 表示该技能命中且目标没有属性免疫时，会在普通伤害计算前清除目标所在一侧
 * 的物理/特殊/全伤害屏障。把它放在技能槽而不是屏障模型中，是因为屏障本身只描述防守状态，真正决定“本次攻击
 * 会不会打碎屏障”的是技能资料。该字段只控制屏障移除时机，不改变命中、保护、属性免疫、伤害公式或附加效果。
 * `typelessDamage` 表示该技能虽然仍有一个资料层基础属性 ID，但本次普通伤害按“无属性伤害”处理：不获得属性一致
 * 加成、不读取属性克制倍率，也不触发指定属性吸收、指定属性增伤或指定属性减伤道具。现代挣扎就是这种规则形态；
 * 把它做成显式布尔值，是为了避免用一个不存在的属性编号伪装无属性，导致未来道具或特性误把它当成普通属性技能。
 * `elementOverridesByWeather` 表示指定天气下技能本次结算使用的属性覆盖，例如气象球在晴天变为火属性。
 * `elementOverridesByTerrain` 表示指定场地下技能本次结算使用的属性覆盖，例如场地脉冲在电气场地变为电属性。
 * `ignoresUserBurnAttackReduction` 表示该物理技能在使用者灼伤时仍使用正常攻击值，例如硬撑。
 * `lockMoveTurnsMin`/`lockMoveTurnsMax` 表示使用后会锁定连续使用的总回合数，包含当前首次使用回合；
 * `confusesUserAfterLock` 表示锁定结束后使用者会进入混乱。
 * `forceTargetSwitch` 表示技能成功命中并完成伤害/附加效果后，会强制目标所属方随机换入一个可战斗后备成员。
 * `locksAccuracyOnTarget` 表示变化技能命中后让使用者到下回合结束前锁定该目标，下一次对该目标的命中判定必中。
 * `targetLastSkillPpReduction` 表示技能成功后扣减目标最近一次成功使用技能的剩余 PP，例如怨恨固定扣 4 点；
 * 若目标没有可扣减的最近技能或该技能 PP 已为 0，会写入技能失败事件而不修改状态。
 * `plantsLeechSeed` 表示技能成功命中后会在目标身上写入寄生种子；该效果按使用者所在站位保存来源，而不是保存
 * 使用者 actorId，以支持原使用者换下后同位置成员继续获得回复的现代双打规则。
 * `clearsUserSideHazardsAndTraps` 表示技能成功连接后会清理使用者身上的束缚/寄生种子，并清除使用者一侧全部
 * 入场陷阱。它和 `sideEntryHazardApplications` 分开，是因为后者是“向目标侧放置陷阱”，而这里是“从己方侧
 * 移除既有陷阱和自身限制”，两者的作用侧和事件语义完全相反。
 * `clearsFieldHazardsAndSubstitutes` 表示变化技能成功后清除双方一侧的全部入场陷阱，并直接移除当前场上所有
 * 替身。该效果不要求场上实际存在陷阱或替身；因此它只负责状态清理，不能让没有可清目标的场景变成技能失败。
 * `clearsTargetSideBarriersAndFieldHazards` 表示变化技能成功后清除目标侧伤害屏障与非伤害型防护、双方入场陷阱，
 * 并清除当前场地。它不移除替身；替身穿透由 [bypassesSubstitute] 单独表达，方便其它非清场技能复用。
 * `groundedTerrainPriorityBoosts` 表示使用者接地且指定场地存在时，技能行动优先度获得的额外提升。
 * `statStageOperations` 表示技能命中后执行的能力阶级清除、复制、交换或取反等结构化操作。
 * `sideConditionApplications` 表示技能命中后建立的一侧防守屏障效果，例如物理屏障或特殊屏障。
 * `sideSpeedModifierApplications` 表示技能命中后建立的一侧速度结算效果，例如顺风。
 * `sideEntryHazardApplications` 表示技能命中后建立在一侧、等待后续成员换入时触发的入场陷阱效果。
 * `curesUserSideMajorStatuses` 表示技能成功后清除使用者所在侧全部成员的主要异常状态；治愈铃声使用该字段。
 * 它只处理主要异常槽位，不清除混乱、寄生、束缚等临时状态。
 * `curesUserMajorStatus` 表示技能成功后只清除使用者自身的主要异常状态；勇气填充使用该字段。它和
 * `curesUserSideMajorStatuses` 分开，是为了避免把“自我净化”误扩展成队伍净化。
 * `curesUserSideActiveMajorStatuses` 表示技能成功后只清除使用者同侧当前上场成员的主要异常状态；丛林治疗和
 * 新月祈祷使用该字段。它刻意不复用整队清除字段，避免把“当前在场的自己和同伴”错误扩展到后备成员。
 * `fieldSpeedOrderApplications` 表示技能命中后建立的全场速度顺序效果，例如戏法空间。
 * `chargesBeforeUse` 表示技能首次使用时先进入蓄力，下一次技能行动才真正释放效果；
 * `chargeSkippedByWeathers` 表示指定天气下该蓄力等待可以省略，技能会在宣告回合直接进入命中和伤害流程。
 * `hpEffects` 表示技能成功后直接改变 HP 的效果，例如吸取回复、反作用伤害或自我回复。
 * `restoresUserBySleeping` 表示技能成功后让使用者进入固定 2 次行动阻止的睡眠并回满 HP；睡觉使用该字段。
 * 这条规则会覆盖已有主要异常，但仍会被满 HP、回复封锁、睡眠免疫、场地免疫和已有睡眠阻止。
 * `postDamageStatusCures` 表示技能造成实际伤害后治愈目标指定主要异常的效果。
 * `removesUserElementAfterDamage` 表示技能成功造成伤害后移除使用者当前与技能基础属性相同的属性。
 * `weightEffects` 表示技能成功后对成员当前体重产生的临时修正，例如速度成功提升后降低自身有效体重。
 * `rechargesAfterUse` 表示技能成功造成实际伤害后，使用者下一次技能行动前必须休整一次。
 * `environmentEffects` 表示技能成功后直接改写全场环境的效果，例如设置天气。
 *
 * 普通伤害公式只处理带威力的物理/特殊技能；其它技能效果通过显式规则对象进入对应 resolver。
 * 当前显式支持主要异常状态、临时状态、能力阶级变化、HP 变化、环境变化和场上效果，避免用弱类型脚本描述核心规则。
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
	val receivedDamage: BattleReceivedDamage? = null,
	val oneHitKnockOut: BattleOneHitKnockOut? = null,
	val accuracy: Int?,
	val targetScope: BattleSkillTargetScope = BattleSkillTargetScope.SELECTED_TARGET,
	val minHits: Int = 1,
	val maxHits: Int = 1,
	val makesContact: Boolean = false,
	val criticalHitStage: Int = 0,
	val criticalHitStageBoost: Int = 0,
	val affectedByProtect: Boolean = true,
	val protectsUser: Boolean = false,
	val enduresFatalDamage: Boolean = false,
	val thawsUserBeforeMove: Boolean = false,
	val soundBased: Boolean = false,
	val bypassesSubstitute: Boolean = false,
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
	val defendingStatOverride: BattleStat? = null,
	val leavesTargetAtOneHp: Boolean = false,
	val breaksTargetSideDamageReductions: Boolean = false,
	val typelessDamage: Boolean = false,
	val elementOverridesByWeather: Map<BattleWeather, Long> = emptyMap(),
	val elementOverridesByTerrain: Map<BattleTerrain, Long> = emptyMap(),
	val ignoresUserBurnAttackReduction: Boolean = false,
	val lockMoveTurnsMin: Int = 1,
	val lockMoveTurnsMax: Int = 1,
	val confusesUserAfterLock: Boolean = false,
	val forceTargetSwitch: Boolean = false,
	val locksAccuracyOnTarget: Boolean = false,
	val targetLastSkillPpReduction: Int = 0,
	val plantsLeechSeed: Boolean = false,
	val clearsUserSideHazardsAndTraps: Boolean = false,
	val clearsFieldHazardsAndSubstitutes: Boolean = false,
	val clearsTargetSideBarriersAndFieldHazards: Boolean = false,
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
	val sideProtectionApplications: List<BattleSideProtectionApplication> = emptyList(),
	val curesUserSideMajorStatuses: Boolean = false,
	val curesUserMajorStatus: Boolean = false,
	val curesUserSideActiveMajorStatuses: Boolean = false,
	val fieldSpeedOrderApplications: List<BattleFieldSpeedOrderApplication> = emptyList(),
	val hpEffects: List<BattleSkillHpEffect> = emptyList(),
	val restoresUserBySleeping: Boolean = false,
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
		require(receivedDamage == null || damageClass != BattleDamageClass.STATUS) {
			"received damage rule requires a damaging skill"
		}
		require(oneHitKnockOut == null || damageClass != BattleDamageClass.STATUS) {
			"one-hit knock out damage requires a damaging skill"
		}
		val directDamageRuleCount =
			listOf(fixedDamage, proportionalDamage, hpDerivedDamage, receivedDamage, oneHitKnockOut).count { it != null }
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
		require(
			defendingStatOverride == null ||
				defendingStatOverride == BattleStat.DEFENSE ||
				defendingStatOverride == BattleStat.SPECIAL_DEFENSE,
		) {
			"defending stat override must target defense or special defense"
		}
		require(defendingStatOverride == null || damageClass != BattleDamageClass.STATUS) {
			"defending stat override requires a damaging skill"
		}
		require(!leavesTargetAtOneHp || damageClass != BattleDamageClass.STATUS) {
			"target one hp floor requires a damaging skill"
		}
		require(!breaksTargetSideDamageReductions || damageClass != BattleDamageClass.STATUS) {
			"screen breaking requires a damaging skill"
		}
		require(!typelessDamage || damageClass != BattleDamageClass.STATUS) {
			"typeless damage requires a damaging skill"
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
		require(!locksAccuracyOnTarget || damageClass == BattleDamageClass.STATUS) {
			"accuracy lock effect requires a status skill"
		}
		require(targetLastSkillPpReduction >= 0) { "targetLastSkillPpReduction must not be negative" }
		require(targetLastSkillPpReduction == 0 || damageClass == BattleDamageClass.STATUS) {
			"target last skill PP reduction requires a status skill"
		}
		require(criticalHitStage >= 0) { "criticalHitStage must not be negative" }
		require(criticalHitStageBoost >= 0) { "criticalHitStageBoost must not be negative" }
		require(criticalHitStageBoost == 0 || damageClass == BattleDamageClass.STATUS) {
			"critical hit stage boost requires a status skill"
		}
		require(!curesUserSideMajorStatuses || damageClass == BattleDamageClass.STATUS) {
			"user side status cure requires a status skill"
		}
		require(!curesUserMajorStatus || damageClass == BattleDamageClass.STATUS) {
			"user status cure requires a status skill"
		}
		require(!curesUserSideActiveMajorStatuses || damageClass == BattleDamageClass.STATUS) {
			"user side active status cure requires a status skill"
		}
		require(!restoresUserBySleeping || damageClass == BattleDamageClass.STATUS) {
			"rest healing requires a status skill"
		}
		require(
			hpEffects.none {
				it is BattleSkillHpEffect.MaximizeUserAttackWithHalfMaxHpCost ||
					it is BattleSkillHpEffect.AverageUserAndTargetCurrentHp
			} || damageClass == BattleDamageClass.STATUS,
		) {
			"direct status HP effects require a status skill"
		}
		require(!protectsUser || damageClass == BattleDamageClass.STATUS) { "protect skill must be a status skill" }
		require(!enduresFatalDamage || damageClass == BattleDamageClass.STATUS) {
			"fatal damage endure requires a status skill"
		}
		require(!(protectsUser && enduresFatalDamage)) {
			"protect barrier and fatal damage endure must be configured as separate skill effects"
		}
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
	 * 返回扣减指定 PP 后的技能槽。
	 *
	 * 怨恨这类效果扣的是目标“最近成功使用技能”的剩余 PP，而不是当前正在宣告的技能 PP。扣减量大于剩余 PP 时
	 * 会夹到 0，调用方负责在剩余 PP 已经为 0 时把技能判为失败，避免事件里出现扣减 0 点的伪成功。
	 */
	fun reducePp(amount: Int): BattleSkillSlot {
		require(amount > 0) { "PP reduction amount must be positive" }
		return copy(remainingPp = (remainingPp - amount).coerceAtLeast(0))
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
