package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers

/**
 * 战斗引擎内部 resolver 装配表。
 *
 * [BattleEngine] 是对外入口，只应该表达“启动战斗”和“结算一回合”这两个 API；如果把所有 resolver
 * 的构造也留在那里，门面会被依赖顺序、回调闭包和阶段注释淹没。本类把这些内部组件集中在同一个文件中，
 * 但不引入 Spring 容器、工厂接口或可配置插件点：现代规则阶段顺序仍由代码固定，测试也继续直接通过
 * [BattleEngine] 覆盖真实装配。
 *
 * 这里有几处看起来像循环依赖的回调，例如行动前混乱自伤、入场陷阱和回合末伤害都会复用
 * [BattlePostDamageEffects.applyLowHpHealingItem]。这些回调是刻意保留的：它们让所有伤害入口共享同一套
 * 低体力道具触发线、回复封锁和道具消费规则，避免每个阶段各自复制一份“差不多”的顺序。
 */
internal class BattleEngineComponents(
	damageCalculator: BattleDamageCalculator,
	statStageModifiers: BattleStatStageModifiers,
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
	 * 该阶段只会推进战斗状态，不会修改 `TurnContext` 中的保护集合或其它回合内编排字段。因此执行技能流程只需要
	 * 把返回的状态重新放回当前上下文。混乱自伤后可能触发低体力回复道具，所以这里把统一伤害后结算器里的
	 * 低体力道具处理函数作为回调传入，避免出现“普通伤害一套道具顺序，混乱自伤另一套道具顺序”。
	 */
	private val beforeMoveEffects = BattleBeforeMoveEffects(
		statStageModifiers = statStageModifiers,
		lowHpItemHealing = { state, actorId -> postDamageEffects.applyLowHpHealingItem(state, actorId) },
	)

	/**
	 * 入场特性结算器。
	 *
	 * 它暴露给 [BattleEngine.start]，因为开局状态需要立即触发双方首发成员的入场特性；回合内换入也复用同一个
	 * 对象，确保天气、场地和威吓类入场效果在开局与替换阶段保持完全一致。
	 */
	val switchInAbilityEffects = BattleSwitchInAbilityEffects(actionOrdering, environmentEffects)

	/**
	 * 回合末临时状态推进器。
	 *
	 * 主状态机仍负责决定回合末阶段顺序：先结算伤害/回复，再清理不会跨回合保留的状态，再推进持续时间，
	 * 然后推进天气、场地、一侧状态和回合上限。这个对象只处理成员运行态上那些纯机械的临时状态字段。
	 */
	private val endTurnVolatileStatuses = BattleEndTurnVolatileStatuses()

	/**
	 * 回合末环境与持续伤害结算器。
	 *
	 * 主状态机只保留“回合末阶段发生在技能阶段之后、持续时间推进之前”这个编排事实；具体异常伤害、束缚伤害、
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
	 * 这样做的边界比较刻意：入场陷阱本身是一个独立阶段，适合从主流程里移走；但“毒菱是否被属性、场地、
	 * 特性或道具阻止”和“入场伤害后是否触发低体力回复道具”并不是入场陷阱专属规则。如果在新 resolver 里复制
	 * 这些判断，后续修正状态免疫或道具回复顺序时就会出现两套实现。这里用闭包把共享判断接进 resolver，
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
	private val skillDamageResolution = BattleSkillDamageResolution(
		directDamage = directDamage,
		damageHitResolution = damageHitResolution,
		skillAdditionalEffects = skillAdditionalEffects,
		lockedMoves = lockedMoves,
		postDamageEffects = postDamageEffects,
	)
	private val skillTargetResolution = BattleSkillTargetResolution(
		preHitTargetGate = preHitTargetGate,
		skillBlockEffects = skillBlockEffects,
		lockedMoves = lockedMoves,
		skillAdditionalEffects = skillAdditionalEffects,
		skillHpEffects = skillHpEffects,
		environmentEffects = environmentEffects,
		skillDamageResolution = skillDamageResolution,
	)
	private val skillUseSetupResolution = BattleSkillUseSetupResolution(
		beforeMoveEffects = beforeMoveEffects,
		chargeMoves = chargeMoves,
		lockedMoves = lockedMoves,
		skillTargeting = skillTargeting,
	)
	private val skillUseResolution = BattleSkillUseResolution(
		useSetupResolution = skillUseSetupResolution,
		resolveTarget = skillTargetResolution::resolve,
	)

	/**
	 * 完整回合结算器。
	 *
	 * 这是内部装配向 [BattleEngine] 暴露的第二个入口：替换、技能行动、回合末结算、天气/场地推进和胜负检查都
	 * 仍按 [BattleTurnResolution] 的固定顺序执行。保持单一暴露点可以避免外部代码绕过回合状态机直接调用中间
	 * resolver。
	 */
	val turnResolution = BattleTurnResolution(
		switchResolution = switchResolution,
		actionPlanner = actionPlanner,
		skillUseResolution = skillUseResolution,
		endTurnVolatileStatuses = endTurnVolatileStatuses,
		endTurnEffects = endTurnEffects,
		environmentEffects = environmentEffects,
	)
}
