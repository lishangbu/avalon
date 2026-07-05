package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 寄生种子的种下、持续伤害和站位回复规则。
 *
 * 寄生种子看起来像普通临时状态，但它和混乱、束缚都不一样：它没有固定持续回合；目标离场时解除；目标每回合
 * 受到最大 HP 1/8 的间接伤害；回复不是给原使用者 actorId，而是给原使用者当时所在的一侧上场席位。这个站位
 * 语义是现代双打正确性的关键：原使用者换下后，同一位置的新上场成员仍然接收回复；如果该位置没有可战斗成员，
 * 本回合不会扣目标 HP。
 *
 * 本类保持两条边界：
 * - 种下阶段只处理技能成功命中后的写入、草属性免疫、替身阻挡和重复失败，不参与命中/保护/属性伤害流程。
 * - 回合末阶段只处理寄生种子自己的扣血与回复，扣血后的低体力道具、倒下和胜负仍交给
 *   [BattleEndTurnDamageResultEffects]，避免每个残留伤害来源复制一份收口顺序。
 */
internal class BattleLeechSeedEffects(
	private val targetDefenseEffects: BattleTargetDefenseEffects,
	private val damageResultEffects: BattleEndTurnDamageResultEffects,
) {
	/**
	 * 在目标身上种下寄生种子。
	 *
	 * 函数只在 [BattleSkillSlot.plantsLeechSeed] 为 true 时工作。目标已经倒下时保持状态不变；目标已有寄生种子、
	 * 目标当前为草属性，或目标替身阻挡对手变化技能效果时，会追加 [BattleEvent.SkillFailed]，让 replay 明确看到
	 * 技能命中后效果没有写入。成功时保存使用者所在侧和上场索引，并追加 [BattleEvent.LeechSeedPlanted]。
	 */
	fun apply(
		state: BattleState,
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
	): BattleState {
		if (!skill.plantsLeechSeed) {
			return state
		}
		val actorSide = state.sideOf(actorId) ?: return state
		val sourceActiveIndex = actorSide.activeActorIds.indexOf(actorId)
		if (sourceActiveIndex < 0) {
			return state
		}
		val target = state.participant(targetActorId) ?: return state
		if (!target.canBattle()) {
			return state
		}
		if (target.isLeechSeeded()) {
			return state.appendLeechSeedFailure(actorId, targetActorId, skill, "target-already-leech-seeded")
		}
		if (target.hasElement(state.rules.elementId(GRASS_ELEMENT_CODE))) {
			return state.appendLeechSeedFailure(actorId, targetActorId, skill, "grass-target-immune-to-leech-seed")
		}
		if (targetDefenseEffects.substituteBlocksOpponentEffect(state, actorId, targetActorId, skill)) {
			return state.appendLeechSeedFailure(actorId, targetActorId, skill, "leech-seed-blocked-by-substitute")
		}
		return state
			.replaceParticipant(target.applyLeechSeed(actorSide.sideId, sourceActiveIndex))
			.appendEvent(
				BattleEvent.LeechSeedPlanted(
					turnNumber = state.turnNumber,
					actorId = actorId,
					targetActorId = targetActorId,
					sourceSideId = actorSide.sideId,
					sourceActiveIndex = sourceActiveIndex,
				),
			)
	}

	/**
	 * 处理回合末寄生种子扣血和站位回复。
	 *
	 * 只有当前上场、仍可战斗、已经被种下寄生种子的目标会参与结算。若来源站位当前没有可战斗成员，现代规则下
	 * 本回合不会抽取目标 HP；若目标拥有间接伤害免疫，扣血和回复都不会发生。实际扣血量按目标最大 HP 的 1/8
	 * 计算，最少 1 点；即使目标当前 HP 低于该值，回复方仍按规则伤害值尝试回复，再由缺失 HP 夹取实际回复量。
	 */
	fun applyEndTurnDrain(state: BattleState): BattleState =
		state.sides
			.flatMap { it.activeParticipants() }
			.fold(state) { current, participant ->
				val latest = current.participant(participant.actorId) ?: return@fold current
				if (!latest.canBattle() || !latest.isLeechSeeded()) {
					return@fold current
				}
				val sourceActor = current.leechSeedSourceActor(latest) ?: return@fold current
				if (!sourceActor.canBattle() || latest.hasIndirectDamageImmunity()) {
					return@fold current
				}
				val sourceSideId = requireNotNull(latest.leechSeedSourceSideId) { "leech seed source side must be present" }
				val sourceActiveIndex = requireNotNull(latest.leechSeedSourceActiveIndex) {
					"leech seed source active index must be present"
				}
				val damage = (latest.maxHp / LEECH_SEED_DAMAGE_DENOMINATOR).coerceAtLeast(1)
				val damaged = latest.receiveDamage(damage)
				damageResultEffects.apply(
					state = current,
					damaged = damaged,
					event = BattleEvent.LeechSeedDamageApplied(
						turnNumber = current.turnNumber,
						actorId = latest.actorId,
						sourceSideId = sourceSideId,
						sourceActiveIndex = sourceActiveIndex,
						amount = damage,
					),
					afterEvent = { afterDamage ->
						applyLeechSeedHealing(afterDamage, latest.actorId, sourceActor.actorId, damage)
					},
				)
			}

	/**
	 * 根据寄生种子扣血结果回复来源站位成员。
	 *
	 * 回复必须在扣血事件之后立即写入，这样 replay 顺序能表达“目标被抽取 -> 来源站位恢复”。函数会重新读取
	 * [afterDamage] 中的最新来源成员，避免来源和目标为同一 actor 或前序事件已经改变 HP 时使用过期快照。
	 */
	private fun applyLeechSeedHealing(
		afterDamage: BattleState,
		targetActorId: String,
		sourceActorId: String,
		damage: Int,
	): BattleState {
		val source = afterDamage.participant(sourceActorId) ?: return afterDamage
		if (!source.canReceiveHealing()) {
			return afterDamage
		}
		val healAmount = damage.coerceAtMost(source.maxHp - source.currentHp)
		return afterDamage
			.replaceParticipant(source.heal(healAmount))
			.appendEvent(
				BattleEvent.LeechSeedHealingApplied(
					turnNumber = afterDamage.turnNumber,
					actorId = source.actorId,
					sourceTargetActorId = targetActorId,
					amount = healAmount,
				),
			)
	}

	/**
	 * 解析寄生种子来源站位当前成员。
	 *
	 * 运行态保存的是来源侧和站位索引；这里按当前 `activeActorIds` 读取该位置的成员。若双打中该位置成员已经倒下且
	 * 尚未补位，调用方会把 null 视为“本回合不抽取目标 HP”，避免把回复错误给同侧其它站位。
	 */
	private fun BattleState.leechSeedSourceActor(target: BattleParticipant): BattleParticipant? {
		val sourceSideId = target.leechSeedSourceSideId ?: return null
		val sourceActiveIndex = target.leechSeedSourceActiveIndex ?: return null
		val sourceSide = sides.firstOrNull { it.sideId == sourceSideId } ?: return null
		val sourceActorId = sourceSide.activeActorIds.getOrNull(sourceActiveIndex) ?: return null
		return participant(sourceActorId)
	}

	private fun BattleState.appendLeechSeedFailure(
		actorId: String,
		targetActorId: String,
		skill: BattleSkillSlot,
		reason: String,
	): BattleState =
		appendEvent(
			BattleEvent.SkillFailed(
				turnNumber = turnNumber,
				actorId = actorId,
				targetActorId = targetActorId,
				skillId = skill.skillId,
				reason = reason,
			),
		)

	private companion object {
		/**
		 * 现代寄生种子每回合抽取目标最大 HP 的 1/8。
		 */
		private const val LEECH_SEED_DAMAGE_DENOMINATOR = 8

		/**
		 * 属性资料中的草属性稳定 code。
		 */
		private const val GRASS_ELEMENT_CODE = "grass"
	}
}
