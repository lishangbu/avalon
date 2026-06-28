package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 现代回合制战斗引擎的第一阶段核心状态机。
 *
 * 该类不依赖 Spring、Jimmer 或数据库。调用方传入已经冻结的初始状态、规则快照、行动列表和随机源，
 * 引擎返回新的不可变战斗状态和事件流。第一阶段实现单打的最小闭环：启动、回合开始、技能行动排序、
 * PP 消耗、命中判定、基础伤害、倒下检测和胜负判定。
 *
 * 当前不负责的边界包括：替换、道具使用、状态持续效果、天气地形、特性 hook、双打目标重定向、保护、
 * 击中要害和复杂技能脚本。这些能力会通过后续规则处理器接入，但仍共享这里的事件流和确定性随机源。
 */
class BattleEngine(
	private val damageCalculator: BattleDamageCalculator = BattleDamageCalculator(),
) {
	/**
	 * 启动一场战斗并产出初始事件。
	 *
	 * @param initialState 已冻结的战斗初始快照。
	 * @return turnNumber 为 0 的战斗状态，事件流包含 `BattleStarted`。
	 */
	fun start(initialState: BattleInitialState): BattleState =
		BattleState(
			format = initialState.format,
			rules = initialState.rules,
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

	/**
	 * 结算一个完整回合。
	 *
	 * @param state 当前战斗状态，不能已经结束。
	 * @param actions 本回合行动。第一阶段要求每个可行动上场成员最多提交一个 `UseSkill`。
	 * @param random 所有命中、伤害浮动和同速排序都从这里消费随机数。
	 * @return 结算后的新状态。若战斗结束，事件流最后会包含 `BattleEnded`；否则包含 `TurnEnded`。
	 */
	fun resolveTurn(state: BattleState, actions: List<BattleAction>, random: BattleRandom): BattleState {
		require(state.result == null) { "battle already ended" }
		val nextTurnNumber = state.turnNumber + 1
		val started = state
			.copy(turnNumber = nextTurnNumber)
			.appendEvent(BattleEvent.TurnStarted(nextTurnNumber))
		val orderedActions = orderActions(started, actions, random)
		val resolved = orderedActions.fold(started) { current, plan ->
			if (current.result != null) current else executeUseSkill(current, plan, random)
		}
		return resolved.result?.let { resolved } ?: resolved.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
	}

	/**
	 * 按优先度、速度和同速随机数排序行动。
	 *
	 * 第一阶段只支持技能行动，所以优先度来自技能槽。速度相同的行动会消费随机数作为排序键；
	 * 这不是最终双打同速规则的完整实现，但已经保证同一随机脚本下的 replay 稳定。
	 */
	private fun orderActions(state: BattleState, actions: List<BattleAction>, random: BattleRandom): List<ActionPlan> {
		val plans = actions.map { action ->
			when (action) {
				is BattleAction.UseSkill -> {
					val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
					val skill = requireNotNull(actor.skillSlot(action.skillId)) { "skill not found: ${action.skillId}" }
					ActionPlan(action, actor, skill)
				}
			}
		}
		return plans
			.groupBy { it.skill.priority to it.actor.speed }
			.toSortedMap(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
			.values
			.flatMap { sameOrderPlans ->
				if (sameOrderPlans.size == 1) {
					sameOrderPlans
				} else {
					sameOrderPlans.sortedBy { random.nextInt(1_000_000, "speed tie for ${it.actor.actorId}") }
				}
			}
	}

	/**
	 * 执行一次使用技能行动。
	 *
	 * 行动者若已经倒下会被跳过；目标若已经倒下或不存在也会被跳过，后续双打目标重定向会替换这里的简单规则。
	 */
	private fun executeUseSkill(state: BattleState, plan: ActionPlan, random: BattleRandom): BattleState {
		val action = plan.action as BattleAction.UseSkill
		val actor = state.participant(action.actorId) ?: return state
		val target = state.participant(action.targetActorId) ?: return state
		if (!actor.canBattle() || !target.canBattle()) {
			return state
		}
		val skill = actor.skillSlot(action.skillId) ?: return state
		require(skill.remainingPp > 0) { "skill has no remaining PP: ${skill.skillId}" }

		val actorAfterPp = actor.replaceSkillSlot(skill.consumePp())
		val usedState = state
			.replaceParticipant(actorAfterPp)
			.appendEvent(
				BattleEvent.SkillUsed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					skillName = skill.name,
				),
			)

		val accuracyCheck = accuracyCheck(skill, random)
		if (!accuracyCheck.hit) {
			return usedState.appendEvent(
				BattleEvent.SkillMissed(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					accuracyRoll = accuracyCheck.roll ?: 0,
				),
			)
		}
		if (skill.damageClass == BattleDamageClass.STATUS) {
			return usedState
		}

		val randomPercent = 85 + random.nextInt(16, "damage random for ${skill.skillId}")
		val damage = damageCalculator.calculate(
			BattleDamageRequest(
				attacker = actorAfterPp,
				defender = target,
				skill = skill,
				rules = state.rules,
				randomPercent = randomPercent,
			),
		)
		val damagedTarget = target.receiveDamage(damage.amount)
		val damagedState = usedState
			.replaceParticipant(damagedTarget)
			.appendEvent(
				BattleEvent.DamageApplied(
					turnNumber = state.turnNumber,
					actorId = actor.actorId,
					targetActorId = target.actorId,
					skillId = skill.skillId,
					amount = damage.amount,
					effectiveness = damage.effectiveness,
				),
			)
		return damagedState.handleFaintAndResult(damagedTarget)
	}

	/**
	 * 处理命中判定。
	 *
	 * 空命中表示必中；否则消费一个 1 到 100 的随机掷点，掷点小于等于命中值时命中。
	 * 未来命中/闪避阶级、天气必中、无防守等规则会在这里之前或这里内部追加 modifier。
	 */
	private fun accuracyCheck(skill: BattleSkillSlot, random: BattleRandom): AccuracyCheck {
		val accuracy = skill.accuracy ?: return AccuracyCheck(hit = true, roll = null)
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return AccuracyCheck(hit = roll <= accuracy, roll = roll)
	}

	/**
	 * 在伤害后追加倒下事件并判断胜负。
	 *
	 * 第一阶段只要某一方没有可战斗成员就立即结束战斗。后续替换请求、双打多成员同时倒下和裁定规则会扩展这里。
	 */
	private fun BattleState.handleFaintAndResult(target: BattleParticipant): BattleState {
		val withFaint = if (!target.canBattle()) {
			appendEvent(BattleEvent.ParticipantFainted(turnNumber, target.actorId))
		} else {
			this
		}
		val defeatedSides = withFaint.sides.filterNot { it.hasRemainingParticipant() }
		if (defeatedSides.isEmpty()) {
			return withFaint
		}
		val winner = withFaint.sides.first { it !in defeatedSides }
		val result = BattleResult(winningSideId = winner.sideId, reason = "all-opponents-fainted")
		return withFaint
			.copy(result = result)
			.appendEvent(
				BattleEvent.BattleEnded(
					turnNumber = turnNumber,
					winningSideId = result.winningSideId,
					reason = result.reason,
				),
			)
	}

	private data class ActionPlan(
		val action: BattleAction,
		val actor: BattleParticipant,
		val skill: BattleSkillSlot,
	)

	private data class AccuracyCheck(
		val hit: Boolean,
		val roll: Int?,
	)
}
