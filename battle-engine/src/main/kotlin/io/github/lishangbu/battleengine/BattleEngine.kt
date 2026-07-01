package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 现代回合制战斗引擎的第一阶段核心状态机。
 *
 * 该类不依赖 Spring、Jimmer 或数据库。调用方传入已经冻结的初始状态、规则快照、行动列表和随机源，
 * 引擎返回新的不可变战斗状态和事件流。第一阶段实现基础战斗闭环：启动、回合开始、技能行动排序、
 * 替换、PP 消耗、命中/闪避判定、击中要害、基础伤害、保护、状态、天气、场地、双打范围目标修正、
 * 倒下检测和胜负判定。
 *
 * 当前不负责的边界包括：主动使用道具、状态持续效果细分、复杂技能脚本和完整官方竞技裁定。
 * 这些能力会通过后续规则处理器接入，但仍共享这里的事件流和确定性随机源。
 *
 * 本类继续采用“显式阶段状态机”，而不是完整事件驱动调度器。原因是战斗规则最敏感的是阶段顺序：替换必须先于
 * 技能行动，行动前状态必须先于 PP 消耗和命中判定，伤害后道具、倒下检查、回合末伤害、天气/场地持续时间也都
 * 有严格先后。这里的 [BattleEvent] 是已经发生的事实记录，用于 replay、测试断言和调试；它不是用来再次分发
 * 规则 hook 的事件总线。拆出的 resolver 只能封装某个阶段内部的细节，不能重新决定跨阶段顺序。
 */
class BattleEngine(
	private val damageCalculator: BattleDamageCalculator = BattleDamageCalculator(),
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
) {
	private val actionOrdering = BattleActionOrdering(statStageModifiers)
	private val actionPlanner = BattleTurnActionPlanner(actionOrdering)
	private val skillTargeting = BattleSkillTargeting()
	private val hitResolution = BattleHitResolution(statStageModifiers)
	private val directDamage = BattleDirectDamage()
	private val damageDefenseEffects = BattleDamageDefenseEffects()
	private val targetDefenseEffects = BattleTargetDefenseEffects()
	private val skillHpEffects = BattleSkillHpEffects()
	private val statStageEffects = BattleStatStageEffects(
		substituteBlocksOpponentEffect = { state, actorId, targetActorId, skill ->
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
		},
	)
	private val environmentEffects = BattleEnvironmentEffects()
	private val fieldEffects = BattleFieldEffects()
	private val chargeMoves = BattleChargeMoves()
	/**
	 * 行动前状态阻止 resolver。
	 *
	 * 该阶段只会推进 [BattleState]，不会修改 `TurnContext` 中的保护集合或其它回合内编排字段。因此它从主类抽出
	 * 后，执行技能流程只需要把返回的状态重新放回当前上下文。混乱自伤后可能触发低体力回复道具，所以这里把
	 * 统一伤害后结算器里的低体力道具处理函数作为回调传入，避免出现“普通伤害一套道具顺序，混乱自伤另一套
	 * 道具顺序”。
	 */
	private val beforeMoveEffects = BattleBeforeMoveEffects(
		statStageModifiers = statStageModifiers,
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
	)

	private val switchInAbilityEffects = BattleSwitchInAbilityEffects(actionOrdering, environmentEffects)
	/**
	 * 回合末临时状态推进器。
	 *
	 * 主状态机仍负责决定回合末阶段顺序：先结算伤害/回复，再清理不会跨回合保留的状态，再推进持续时间，
	 * 然后推进天气、场地、一侧状态和回合上限。这个对象只处理成员运行态上那些纯机械的临时状态字段，避免
	 * [BattleEngine.resolveTurn] 被大量 `flatMap activeParticipants + decrement` 细节淹没。
	 */
	private val endTurnVolatileStatuses = BattleEndTurnVolatileStatuses()
	/**
	 * 回合末环境与持续伤害结算器。
	 *
	 * 主类只保留“回合末阶段发生在技能阶段之后、持续时间推进之前”这个编排事实；具体异常伤害、束缚伤害、
	 * 天气伤害、天气/场地/道具回复和回合上限收口都委托给该对象。低体力回复道具仍回调统一伤害后结算器，
	 * 保证所有伤害入口共享同一套道具消费与回复封锁判断。
	 */
	private val endTurnEffects = BattleEndTurnEffects(
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
	)
	/**
	 * 状态附加与状态免疫结算器。
	 *
	 * 主要异常和临时状态的写入、阻止原因、状态治愈道具都放在这里；替身阻挡和无视目标特性的共享判断由目标
	 * 防守 resolver 提供，状态规则本身只关心“能不能写入这个状态”。
	 */
	private val statusEffects = BattleStatusEffects(
		substituteBlocksOpponentEffect = { state, actorId, targetActorId, skill ->
			targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)
		},
		skillIgnoresTargetAbilityEffects = { state, actorId, targetActorId ->
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
		},
	)
	/**
	 * 锁招类技能的持续回合和结束结算器。
	 *
	 * 主状态机仍决定“成功后推进锁招”或“失败后中断锁招”的调用时机；该对象只维护锁招字段、锁招事件和结束后
	 * 可能发生的疲劳混乱。
	 */
	private val lockedMoves = BattleLockedMoveEffects(statusEffects)

	/**
	 * 单目标技能结算前的场地、属性、特性阻止与吸收规则。
	 *
	 * 主流程仍负责这些阻止点的先后顺序，以及被阻止后是否需要中断锁招；该对象只封装每个阻止点自身的判断和
	 * 吸收状态改写。
	 */
	private val skillBlockEffects = BattleSkillBlockEffects(
		skillIgnoresTargetAbilityEffects = { state, actor, target ->
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actor, target)
		},
	)
	private val preHitTargetGate = BattlePreHitTargetGate(
		skillBlockEffects = skillBlockEffects,
		targetDefenseEffects = targetDefenseEffects,
		hitResolution = hitResolution,
	)

	/**
	 * 伤害后的接触特性与携带道具结算器。
	 *
	 * 该对象不计算伤害，只处理 HP 变化之后的附加效果。低体力回复道具也从这里提供给行动前、入场陷阱和回合末
	 * resolver，保证所有伤害入口共享同一套触发线、回复封锁和道具消费规则。
	 */
	private val postDamageEffects = BattlePostDamageEffects(
		statusEffects = statusEffects,
		skillIgnoresTargetAbilityEffects = { state, actorId, targetActorId ->
			targetDefenseEffects.skillIgnoresTargetAbilityEffects(state, actorId, targetActorId)
		},
	)
	private val damageApplicationEffects = BattleDamageApplicationEffects(
		damageDefenseEffects = damageDefenseEffects,
		skillHpEffects = skillHpEffects,
		postDamageEffects = postDamageEffects,
	)
	private val damageHitResolution = BattleDamageHitResolution(
		damageCalculator = damageCalculator,
		hitResolution = hitResolution,
		targetDefenseEffects = targetDefenseEffects,
		damageDefenseEffects = damageDefenseEffects,
		skillHpEffects = skillHpEffects,
		damageApplicationEffects = damageApplicationEffects,
	)

	/**
	 * 入场陷阱结算从主状态机拆出，但仍复用主状态机里的主要异常阻止判断和低体力道具回复判断。
	 *
	 * 这样做的边界比较刻意：入场陷阱本身是一个独立阶段，适合从 4000 多行的主类里移走；但“毒菱是否被属性、
	 * 场地、特性或道具阻止”和“入场伤害后是否触发低体力回复道具”并不是入场陷阱专属规则。如果在新 resolver
	 * 里复制这些判断，后续修正状态免疫或道具回复顺序时就会出现两套实现。这里用闭包把共享判断接进 resolver，
	 * 保持行为只有一份，同时让换入阶段的主流程更短。
	 */
	private val entryHazardEffects = BattleEntryHazardEffects(
		majorStatusBlockReason = { state, actorId, recipient, status ->
			statusEffects.blockedMajorStatusReason(state, actorId, recipient, status)
		},
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
	)
	private val switchResolution = BattleSwitchResolution(
		actionOrdering = actionOrdering,
		endTurnEffects = endTurnEffects,
		entryHazardEffects = entryHazardEffects,
		switchInAbilityEffects = switchInAbilityEffects,
	)
	private val forcedSwitchEffects = BattleForcedSwitchEffects(
		targetDefenseEffects = targetDefenseEffects,
		endTurnEffects = endTurnEffects,
		entryHazardEffects = entryHazardEffects,
		switchInAbilityEffects = switchInAbilityEffects,
	)
	private val skillAdditionalEffects = BattleSkillAdditionalEffects(
		statusEffects = statusEffects,
		statStageEffects = statStageEffects,
		fieldEffects = fieldEffects,
		targetDefenseEffects = targetDefenseEffects,
		forcedSwitchEffects = forcedSwitchEffects,
	)
	private val skillTargetResolution = BattleSkillTargetResolution(
		preHitTargetGate = preHitTargetGate,
		skillBlockEffects = skillBlockEffects,
		lockedMoves = lockedMoves,
		skillAdditionalEffects = skillAdditionalEffects,
		skillHpEffects = skillHpEffects,
		environmentEffects = environmentEffects,
		directDamage = directDamage,
		damageHitResolution = damageHitResolution,
		postDamageEffects = postDamageEffects,
	)
	private val skillUseResolution = BattleSkillUseResolution(
		beforeMoveEffects = beforeMoveEffects,
		chargeMoves = chargeMoves,
		lockedMoves = lockedMoves,
		skillTargeting = skillTargeting,
		resolveTarget = skillTargetResolution::resolve,
	)
	private val turnResolution = BattleTurnResolution(
		switchResolution = switchResolution,
		actionPlanner = actionPlanner,
		skillUseResolution = skillUseResolution,
		endTurnVolatileStatuses = endTurnVolatileStatuses,
		endTurnEffects = endTurnEffects,
		environmentEffects = environmentEffects,
	)

	/**
	 * 启动一场战斗并产出初始事件。
	 *
	 * @param initialState 已冻结的战斗初始快照。
	 * @return turnNumber 为 0 的战斗状态，事件流包含 `BattleStarted`。
	 */
	fun start(initialState: BattleInitialState): BattleState {
		val started = BattleState(
			format = initialState.format,
			rules = initialState.rules,
			environment = initialState.environment,
			sides = initialState.sides,
			turnNumber = 0,
			events = listOf(
				BattleEvent.BattleStarted(
					turnNumber = 0,
					formatCode = initialState.format.code,
					sideIds = initialState.sides.map { it.sideId },
				),
			),
		)
		return switchInAbilityEffects.applyInitial(started)
	}

	/**
	 * 结算一个完整回合。
	 *
	 * @param state 当前战斗状态，不能已经结束。
	 * @param actions 本回合行动。第一阶段要求每个可行动上场成员最多提交一个 `UseSkill`。
	 * @param random 所有命中、击中要害、伤害浮动和同速排序都从这里消费随机数。
	 * @return 结算后的新状态。若战斗结束，事件流最后会包含 `BattleEnded`；否则包含 `TurnEnded`。
	 */
	fun resolveTurn(state: BattleState, actions: List<BattleAction>, random: BattleRandom): BattleState =
		turnResolution.resolve(state, actions, random)

}
