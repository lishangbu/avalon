package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.damage.BattleDamageCalculator
import io.github.lishangbu.battleengine.damage.BattleDamageRequest
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageModifiers
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.random.BattleRandom
import kotlin.math.floor

/**
 * 现代回合制战斗引擎的第一阶段核心状态机。
 *
 * 该类不依赖 Spring、Jimmer 或数据库。调用方传入已经冻结的初始状态、规则快照、行动列表和随机源，
 * 引擎返回新的不可变战斗状态和事件流。第一阶段实现单打的最小闭环：启动、回合开始、技能行动排序、
 * 替换、PP 消耗、命中/闪避判定、击中要害、基础伤害、倒下检测和胜负判定。
 *
 * 当前不负责的边界包括：道具使用、状态持续效果细分、双打范围技能、连续保护成功率和复杂技能脚本。
 * 这些能力会通过后续规则处理器接入，但仍共享这里的事件流和确定性随机源。
 */
class BattleEngine(
	private val damageCalculator: BattleDamageCalculator = BattleDamageCalculator(),
	private val statStageModifiers: BattleStatStageModifiers = BattleStatStageModifiers(),
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

	/**
	 * 结算一个完整回合。
	 *
	 * @param state 当前战斗状态，不能已经结束。
	 * @param actions 本回合行动。第一阶段要求每个可行动上场成员最多提交一个 `UseSkill`。
	 * @param random 所有命中、击中要害、伤害浮动和同速排序都从这里消费随机数。
	 * @return 结算后的新状态。若战斗结束，事件流最后会包含 `BattleEnded`；否则包含 `TurnEnded`。
	 */
	fun resolveTurn(state: BattleState, actions: List<BattleAction>, random: BattleRandom): BattleState {
		require(state.result == null) { "battle already ended" }
		require(actions.map { it.actorId }.toSet().size == actions.size) {
			"each actor can submit at most one action per turn"
		}
		val nextTurnNumber = state.turnNumber + 1
		val started = state
			.copy(turnNumber = nextTurnNumber)
			.appendEvent(BattleEvent.TurnStarted(nextTurnNumber))
		val afterSwitches = resolveSwitches(started, actions.filterIsInstance<BattleAction.SwitchParticipant>(), random)
		if (afterSwitches.result != null) {
			return afterSwitches
		}
		val orderedActions = orderSkillActions(afterSwitches, actions.filterIsInstance<BattleAction.UseSkill>(), random)
		val resolved = orderedActions.fold(TurnContext(afterSwitches)) { current, plan ->
			if (current.state.result != null) current else executeUseSkill(current, plan, random)
		}.state
		val afterEndTurnEffects = resolved.result?.let { resolved } ?: applyEndTurnEffects(resolved)
		return afterEndTurnEffects.result?.let { afterEndTurnEffects }
			?: afterEndTurnEffects.appendEvent(BattleEvent.TurnEnded(nextTurnNumber))
	}

	/**
	 * 按优先度、速度和同速随机数排序行动。
	 *
	 * 第一阶段只支持技能行动，所以优先度来自技能槽。速度相同的行动会消费随机数作为排序键；
	 * 这不是最终双打同速规则的完整实现，但已经保证同一随机脚本下的 replay 稳定。
	 */
	private fun orderSkillActions(state: BattleState, actions: List<BattleAction.UseSkill>, random: BattleRandom): List<ActionPlan> {
		val plans = actions.map { action ->
			val actor = requireNotNull(state.participant(action.actorId)) { "actor not found: ${action.actorId}" }
			val skill = requireNotNull(actor.skillSlot(action.skillId)) { "skill not found: ${action.skillId}" }
			ActionPlan(action, actor, skill)
		}
		return plans
			.groupBy { it.skill.priority to effectiveSpeed(it.actor) }
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
	 * 结算本回合所有替换行动。
	 *
	 * 替换阶段先于技能阶段。第一版按离场成员的有效速度排序，速度相同时消费随机数打破平手；这让未来接入
	 * 入场特性时能得到可复盘的确定顺序。若离场成员已经倒下，事件标记为 `forced=true`。
	 */
	private fun resolveSwitches(
		state: BattleState,
		actions: List<BattleAction.SwitchParticipant>,
		random: BattleRandom,
	): BattleState {
		val ordered = actions
			.map { action ->
				val actor = requireNotNull(state.participant(action.actorId)) { "switch actor not found: ${action.actorId}" }
				SwitchPlan(action, actor)
			}
			.groupBy { effectiveSpeed(it.actor) }
			.toSortedMap(compareByDescending { it })
			.values
			.flatMap { sameSpeedPlans ->
				if (sameSpeedPlans.size == 1) {
					sameSpeedPlans
				} else {
					sameSpeedPlans.sortedBy { random.nextInt(1_000_000, "switch speed tie for ${it.actor.actorId}") }
				}
			}
		return ordered.fold(state) { current, plan ->
			val actor = current.participant(plan.action.actorId) ?: return@fold current
			val side = current.sideOf(actor.actorId) ?: return@fold current
			require(side.isActive(actor.actorId)) { "switch actor must be active: ${actor.actorId}" }
			val switched = current.switchActive(actor.actorId, plan.action.targetActorId)
			switched.appendEvent(
				BattleEvent.ParticipantSwitched(
					turnNumber = current.turnNumber,
					sideId = side.sideId,
					previousActorId = actor.actorId,
					nextActorId = plan.action.targetActorId,
					forced = !actor.canBattle(),
				),
			)
		}
	}

	/**
	 * 执行一次使用技能行动。
	 *
	 * 行动者若已经倒下会被跳过；目标若已经倒下或不存在也会被跳过，后续双打目标重定向会替换这里的简单规则。
	 */
	private fun executeUseSkill(context: TurnContext, plan: ActionPlan, random: BattleRandom): TurnContext {
		val state = context.state
		val action = plan.action as BattleAction.UseSkill
		val actor = state.participant(action.actorId) ?: return context
		val target = state.activeTargetFor(action.targetActorId) ?: return context
		if (!state.isActive(actor.actorId) || !actor.canBattle() || !target.canBattle()) {
			return context
		}
		val skill = actor.skillSlot(action.skillId) ?: return context
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

		if (skill.protectsUser) {
			return context.copy(
				state = usedState.appendEvent(
					BattleEvent.ProtectionStarted(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						skillId = skill.skillId,
					),
				),
				protectedActorIds = context.protectedActorIds + actor.actorId,
			)
		}

		if (target.actorId in context.protectedActorIds && skill.affectedByProtect) {
			return context.copy(
				state = usedState.appendEvent(
					BattleEvent.SkillBlockedByProtection(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						targetActorId = target.actorId,
						skillId = skill.skillId,
					),
				),
			)
		}

		val accuracyCheck = accuracyCheck(actor, target, skill, random)
		if (!accuracyCheck.hit) {
			return context.copy(
				state = usedState.appendEvent(
					BattleEvent.SkillMissed(
						turnNumber = state.turnNumber,
						actorId = actor.actorId,
						targetActorId = target.actorId,
						skillId = skill.skillId,
						accuracyRoll = accuracyCheck.roll ?: 0,
					),
				),
			)
		}
		if (skill.damageClass == BattleDamageClass.STATUS) {
			return context.copy(state = applySkillEffects(usedState, actor.actorId, target.actorId, skill, random))
		}

		val criticalHitCheck = criticalHitCheck(skill, random)
		val randomPercent = 85 + random.nextInt(16, "damage random for ${skill.skillId}")
		val damage = damageCalculator.calculate(
			BattleDamageRequest(
				attacker = actorAfterPp,
				defender = target,
				skill = skill,
				rules = state.rules,
				environment = state.environment,
				randomPercent = randomPercent,
				criticalHit = criticalHitCheck.hit,
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
					criticalHit = criticalHitCheck.hit,
				),
			)
		val afterContactAbilities = applyContactAbilityEffects(
			state = damagedState,
			actorId = actor.actorId,
			targetActorId = damagedTarget.actorId,
			skill = skill,
			random = random,
		)
		val afterRecoil = applyPostDamageItemEffects(
			state = afterContactAbilities,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = damage.amount,
		)
		val targetAfterPostDamage = afterRecoil.participant(damagedTarget.actorId) ?: damagedTarget
		val actorAfterPostDamage = afterRecoil.participant(actor.actorId) ?: actorAfterPp
		val afterTargetFaint = afterRecoil.handleFaintAndResult(targetAfterPostDamage)
		if (afterTargetFaint.result != null) {
			return context.copy(state = afterTargetFaint)
		}
		val afterDamage = afterTargetFaint.handleFaintAndResult(actorAfterPostDamage)
		return context.copy(state = if (afterDamage.result != null) {
			afterDamage
		} else {
			applySkillEffects(afterDamage, actor.actorId, damagedTarget.actorId, skill, random)
		})
	}

	/**
	 * 处理命中判定。
	 *
	 * 空命中表示必中；否则先应用攻击方命中阶级和目标闪避阶级，再消费一个 1 到 100 的随机掷点。
	 * 若修正后命中率已经达到或超过 100，则直接命中且不消费随机数。天气必中、无防守和蓄力中目标等
	 * 例外规则会在这里之前或这里内部追加结构化 modifier。
	 */
	private fun accuracyCheck(
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): AccuracyCheck {
		val accuracy = skill.accuracy ?: return AccuracyCheck(hit = true, roll = null)
		val modifiedAccuracy = floor(
			accuracy *
				statStageModifiers.accuracyMultiplier(actor.statStage(BattleStat.ACCURACY)) /
				statStageModifiers.accuracyMultiplier(target.statStage(BattleStat.EVASION)),
		).toInt().coerceAtLeast(1)
		if (modifiedAccuracy >= 100) {
			return AccuracyCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(100, "accuracy for ${skill.skillId}") + 1
		return AccuracyCheck(hit = roll <= modifiedAccuracy, roll = roll)
	}

	/**
	 * 结算击中要害概率。
	 *
	 * 现代规则下，普通等级概率为 1/24，+1 为 1/8，+2 为 1/2，+3 及以上视为必定击中要害。
	 * 必定要害不消费随机数；其它等级消费 `[0, denominator)`，掷到 0 表示成功。
	 */
	private fun criticalHitCheck(skill: BattleSkillSlot, random: BattleRandom): CriticalHitCheck {
		val denominator = when (skill.criticalHitStage.coerceAtMost(3)) {
			0 -> 24
			1 -> 8
			2 -> 2
			else -> 1
		}
		if (denominator == 1) {
			return CriticalHitCheck(hit = true, roll = null)
		}
		val roll = random.nextInt(denominator, "critical hit for ${skill.skillId}")
		return CriticalHitCheck(hit = roll == 0, roll = roll)
	}

	/**
	 * 应用技能命中后的结构化附加效果。
	 *
	 * 第一批只处理主要异常状态和能力阶级变化。效果按技能槽中的顺序结算；概率小于 100 的效果会消费随机数。
	 * 若目标已经倒下、已有主要异常状态或阶级变化被上下限夹住，则保持状态不变并跳过事件。
	 */
	private fun applySkillEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		val afterStatuses = skill.statusApplications.fold(state) { current, application ->
			if (!chanceSucceeds(application.chancePercent, random, "status chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, application.target) ?: return@fold current
				if (!recipient.canBattle() || recipient.majorStatus != null) {
					current
				} else {
					current
						.replaceParticipant(recipient.applyMajorStatus(application.status))
						.appendEvent(
							BattleEvent.StatusApplied(
								turnNumber = current.turnNumber,
								actorId = actorId,
								targetActorId = recipient.actorId,
								status = application.status,
							),
						)
				}
			}
		}
		return skill.statStageEffects.fold(afterStatuses) { current, effect ->
			if (!chanceSucceeds(effect.chancePercent, random, "stat stage chance for ${skill.skillId}")) {
				current
			} else {
				val recipient = current.effectRecipient(actorId, targetActorId, effect.target) ?: return@fold current
				val beforeStage = recipient.statStage(effect.stat)
				val updated = recipient.changeStatStage(effect.stat, effect.stageDelta)
				val afterStage = updated.statStage(effect.stat)
				if (beforeStage == afterStage) {
					current
				} else {
					current
						.replaceParticipant(updated)
						.appendEvent(
							BattleEvent.StatStageChanged(
								turnNumber = current.turnNumber,
								actorId = actorId,
								targetActorId = recipient.actorId,
								stat = effect.stat,
								delta = afterStage - beforeStage,
								currentStage = afterStage,
							),
						)
				}
			}
		}
	}

	/**
	 * 处理目标方“受到接触技能后影响攻击方”的特性效果。
	 *
	 * 第一批只实现概率附加主要异常状态。该 hook 在伤害事件之后、反伤和倒下判定之前执行，
	 * 可以覆盖接触后攻击方被麻痹等常见场景。
	 */
	private fun applyContactAbilityEffects(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		random: BattleRandom,
	): BattleState {
		if (!skill.makesContact) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		return target.abilityEffects
			.filterIsInstance<BattleAbilityEffect.ContactStatusOnAttacker>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (!actor.canBattle() || actor.majorStatus != null) {
					current
				} else if (!chanceSucceeds(effect.chancePercent, random, "contact status for $targetActorId")) {
					current
				} else {
					current
						.replaceParticipant(actor.applyMajorStatus(effect.status))
						.appendEvent(
							BattleEvent.StatusApplied(
								turnNumber = current.turnNumber,
								actorId = targetActorId,
								targetActorId = actor.actorId,
								status = effect.status,
							),
						)
				}
			}
	}

	/**
	 * 处理造成伤害后的道具反伤。
	 *
	 * 伤害增幅本身由伤害计算器读取道具效果完成；这里根据最终伤害扣除攻击方 HP 并产生反伤事件。
	 */
	private fun applyPostDamageItemEffects(
		state: BattleState,
		actorId: String,
		skill: BattleSkillSlot,
		damageAmount: Int,
	): BattleState {
		if (damageAmount <= 0 || skill.damageClass == BattleDamageClass.STATUS) {
			return state
		}
		return state.participant(actorId)
			?.itemEffects
			.orEmpty()
			.filterIsInstance<BattleItemEffect.DamageBoostWithRecoil>()
			.fold(state) { current, effect ->
				val actor = current.participant(actorId) ?: return@fold current
				if (!actor.canBattle()) {
					current
				} else {
					val recoil = (damageAmount / effect.recoilDenominator).coerceAtLeast(1)
					current
						.replaceParticipant(actor.receiveDamage(recoil))
						.appendEvent(
							BattleEvent.RecoilDamageApplied(
								turnNumber = current.turnNumber,
								actorId = actor.actorId,
								amount = recoil,
							),
						)
				}
			}
	}

	/**
	 * 结算回合末主要异常状态伤害。
	 *
	 * 当前实现覆盖灼伤、中毒和剧毒的固定扣血入口。剧毒的逐回合递增计数尚未建模，因此暂按普通中毒比例处理；
	 * 后续会把持续状态计数加入成员运行态，并用公开 fixture 验证递增伤害。
	 */
	private fun applyEndTurnEffects(state: BattleState): BattleState {
		val afterResidual = state.sides
			.flatMap { it.participants }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle()) {
					current
				} else {
					val residualDamage = residualDamage(latest) ?: return@fold current
					val damaged = latest.receiveDamage(residualDamage)
					current
						.replaceParticipant(damaged)
						.appendEvent(
							BattleEvent.ResidualDamageApplied(
								turnNumber = current.turnNumber,
								actorId = latest.actorId,
								status = requireNotNull(latest.majorStatus),
								amount = residualDamage,
							),
						)
						.handleFaintAndResult(damaged)
				}
			}
		return if (afterResidual.result != null) {
			afterResidual
		} else {
			applyEndTurnHealing(applyEndTurnTerrainEffects(afterResidual))
		}
	}

	/**
	 * 处理回合末场地回复。
	 *
	 * 第一批只实现青草场地的固定比例回复，并暂时认为所有当前上场且仍可战斗的成员都满足受场地影响条件。
	 * 飞行、漂浮、携带道具免疫地面场地等例外会在成员运行态具备“是否接地”后接入。
	 */
	private fun applyEndTurnTerrainEffects(state: BattleState): BattleState {
		if (state.environment.terrain != BattleTerrain.GRASSY) {
			return state
		}
		return state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.currentHp == latest.maxHp) {
					current
				} else {
					val healAmount = (latest.maxHp / current.rules.grassyTerrainHealDenominator).coerceAtLeast(1)
						.coerceAtMost(latest.maxHp - latest.currentHp)
					current
						.replaceParticipant(latest.heal(healAmount))
						.appendEvent(
							BattleEvent.TerrainHealingApplied(
								turnNumber = current.turnNumber,
								actorId = latest.actorId,
								terrain = BattleTerrain.GRASSY,
								amount = healAmount,
							),
						)
				}
			}
	}

	/**
	 * 处理回合末携带道具回复。
	 *
	 * 第一批只实现固定最大 HP 比例回复，不处理道具消耗、回复封锁或复杂场地顺序。
	 */
	private fun applyEndTurnHealing(state: BattleState): BattleState =
		state.sides
			.flatMap { it.participants }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || latest.currentHp == latest.maxHp) {
					current
				} else {
					latest.itemEffects
						.filterIsInstance<BattleItemEffect.HeldEndTurnHeal>()
						.fold(current) { healingState, effect ->
							val currentParticipant = healingState.participant(latest.actorId) ?: return@fold healingState
							val healAmount = (currentParticipant.maxHp / effect.healDenominator).coerceAtLeast(1)
								.coerceAtMost(currentParticipant.maxHp - currentParticipant.currentHp)
							if (healAmount <= 0) {
								healingState
							} else {
								healingState
									.replaceParticipant(currentParticipant.heal(healAmount))
									.appendEvent(
										BattleEvent.HealingApplied(
											turnNumber = healingState.turnNumber,
											actorId = currentParticipant.actorId,
											amount = healAmount,
										),
									)
							}
						}
				}
			}

	/**
	 * 计算主要异常状态在回合末造成的固定伤害。
	 */
	private fun residualDamage(participant: BattleParticipant): Int? =
		when (participant.majorStatus) {
			BattleMajorStatus.BURN -> (participant.maxHp / 16).coerceAtLeast(1)
			BattleMajorStatus.POISON,
			BattleMajorStatus.BAD_POISON -> (participant.maxHp / 8).coerceAtLeast(1)
			else -> null
		}

	/**
	 * 计算行动排序使用的有效速度。
	 *
	 * 速度先应用能力阶级，再应用麻痹减半。天气、道具、特性和顺风等速度修正会在后续 modifier 管线中加入。
	 */
	private fun effectiveSpeed(participant: BattleParticipant): Int {
		val staged = statStageModifiers.modifiedBattleStat(
			participant.speed,
			participant.statStage(BattleStat.SPEED),
		)
		return if (participant.majorStatus == BattleMajorStatus.PARALYSIS) {
			(staged / 2).coerceAtLeast(1)
		} else {
			staged
		}
	}

	/**
	 * 根据效果目标枚举找到实际承受效果的成员。
	 */
	private fun BattleState.effectRecipient(actorId: String, targetActorId: String, target: BattleEffectTarget): BattleParticipant? =
		when (target) {
			BattleEffectTarget.USER -> participant(actorId)
			BattleEffectTarget.TARGET -> participant(targetActorId)
		}

	/**
	 * 结算百分比概率。
	 *
	 * 100% 不消费随机数，0% 永远失败；中间概率消费 1..100 掷点。
	 */
	private fun chanceSucceeds(chancePercent: Int, random: BattleRandom, reason: String): Boolean =
		when (chancePercent) {
			100 -> true
			0 -> false
			else -> random.nextInt(100, reason) + 1 <= chancePercent
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

	private data class SwitchPlan(
		val action: BattleAction.SwitchParticipant,
		val actor: BattleParticipant,
	)

	/**
	 * 单个回合技能阶段的临时上下文。
	 *
	 * `state` 是不断推进的不可变战斗状态；`protectedActorIds` 保存本回合已经成功建立保护屏障的成员。
	 * 这类回合内临时标记不进入 `BattleState`，避免被误认为跨回合持久状态，也方便后续扩展击掌奇袭、
	 * 守住连续成功率、先制阻挡等同样只在当前回合有效的规则。
	 */
	private data class TurnContext(
		val state: BattleState,
		val protectedActorIds: Set<String> = emptySet(),
	)

	private data class AccuracyCheck(
		val hit: Boolean,
		val roll: Int?,
	)

	private data class CriticalHitCheck(
		val hit: Boolean,
		val roll: Int?,
	)
}
