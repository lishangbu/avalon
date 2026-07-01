package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 伤害类技能的成功收尾与失败中断结算器。
 *
 * 普通公式伤害、直接伤害和属性无效/直接伤害失败在“伤害怎么产生”上完全不同，但它们结束技能动作时共享一套
 * 顺序：失败或免疫要中断锁招；成功造成伤害后要按本次技能动作汇总实际 HP 损失，再结算命中后附加效果、造成
 * 伤害后回复道具和锁招推进。把这段从 [BattleSkillDamageResolution] 拆出后，伤害结算器只负责选择直接伤害或
 * 公式伤害路径，本类负责守住共同收尾顺序。
 *
 * 这里不计算伤害、不消费命中/要害/伤害浮动随机数，也不写入目标 HP。它只读取已经产生的事件和最新战斗快照，
 * 所以直接伤害、普通伤害、多段命中和替身伤害都可以复用同一个收口。
 */
internal class BattleDamageMoveFinishResolution(
	private val skillAdditionalEffects: BattleSkillAdditionalEffects,
	private val lockedMoves: BattleLockedMoveEffects,
	private val postDamageEffects: BattlePostDamageEffects,
) {
	/**
	 * 收拢一次技能成功造成伤害后的共同流程。
	 *
	 * 普通公式伤害和固定/比例/HP 派生直接伤害在写入 HP 之前差异很大：前者要消费要害和伤害浮动随机数，后者完全
	 * 跳过普通伤害公式。但只要已经产生实际伤害，两条路径后续顺序一致：
	 * - 如果写入伤害时已经判定胜负，只允许造成伤害后回复类道具读取本次实际伤害，不再追加命中后技能效果或锁招推进。
	 * - 如果战斗还在继续，以最新目标快照结算命中后附加效果，避免目标在前序伤害流程中被保命、解除状态或替换引用后
	 *   仍使用旧对象。
	 * - 再让攻击方造成伤害后回复道具读取完整实际伤害。
	 * - 最后推进锁招/连续招式状态，保证锁招目标记录看到的是附加效果后的最新目标。
	 */
	fun finishSuccessfulDamageMove(
		context: TurnContext,
		actor: BattleParticipant,
		target: BattleParticipant,
		skill: BattleSkillSlot,
		damageAmount: Int,
		random: BattleRandom,
	): TurnContext {
		if (context.state.result != null) {
			return context.copy(
				state = postDamageEffects.applyPostMoveDamageDealtHealingItem(
					state = context.state,
					actorId = actor.actorId,
					skill = skill,
					damageAmount = damageAmount,
				),
			)
		}
		val latestTarget = context.state.participant(target.actorId) ?: target
		val afterEffects = skillAdditionalEffects.apply(context.state, actor.actorId, latestTarget.actorId, skill, random)
		val afterPostMoveItemEffects = postDamageEffects.applyPostMoveDamageDealtHealingItem(
			state = afterEffects,
			actorId = actor.actorId,
			skill = skill,
			damageAmount = damageAmount,
		)
		return context.copy(
			state = lockedMoves.updateAfterSuccessfulUse(
				state = afterPostMoveItemEffects,
				actorId = actor.actorId,
				targetActorId = latestTarget.actorId,
				skill = skill,
				random = random,
			),
		)
	}

	/**
	 * 追加技能失败/免疫事件并按失败路径中断锁招。
	 *
	 * 属性完全无效、直接伤害规则自身失败、命中前 gate 中断等都共享同一个事实：本次技能动作没有进入成功收尾，
	 * 锁招或蓄力释放需要按中断分支清理。调用方负责构造具体事件；本函数只保证事件追加后立刻交给锁招清理。
	 */
	fun interruptSkillWithEvent(
		context: TurnContext,
		state: BattleState,
		actor: BattleParticipant,
		skill: BattleSkillSlot,
		random: BattleRandom,
		event: BattleEvent,
	): TurnContext =
		context.copy(
			state = lockedMoves.endAfterDisruption(
				state = state.appendEvent(event),
				actorId = actor.actorId,
				skill = skill,
				random = random,
			),
		)

	/**
	 * 汇总本次技能动作实际造成的 HP 损失。
	 *
	 * 调用方在多段命中或直接伤害写入前记录事件下标，本函数只扫描该下标之后由同一使用者、同一技能产生的普通
	 * 伤害和替身伤害事件。返回值是目标本体或替身实际扣掉的 HP，不包含命中免疫的 0 伤害、接触特性、反伤、
	 * 天气、异常状态或入场陷阱等非本次技能直接造成的伤害。该口径用于造成伤害后回复道具，避免多段技能按每段
	 * 分别回复。
	 */
	fun damageDealtByMove(
		state: BattleState,
		eventStartIndex: Int,
		actorId: String,
		skillId: Long,
	): Int =
		state.events.asSequence()
			.drop(eventStartIndex)
			.sumOf { event ->
				when (event) {
					is BattleEvent.DamageApplied ->
						if (event.actorId == actorId && event.skillId == skillId) event.amount else 0
					is BattleEvent.SubstituteDamageApplied ->
						if (event.actorId == actorId && event.skillId == skillId) event.amount else 0
					else -> 0
				}
			}
}
