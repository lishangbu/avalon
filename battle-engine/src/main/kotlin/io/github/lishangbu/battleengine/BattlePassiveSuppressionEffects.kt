package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleState

/** 返回成员当前特性的完整规则快照，不区分是否正被全场效果压制。 */
internal fun BattleParticipant.allAbilityEffects(): List<BattleAbilityEffect> =
	abilityEffects.ifEmpty { suppressedAbilityEffects }

/** 返回成员当前携带道具的完整规则快照，不区分是否正被笨拙压制。 */
internal fun BattleParticipant.allItemEffects() = itemEffects.ifEmpty { suppressedItemEffects }

/**
 * 同步化学变化气体和笨拙产生的被动压制状态。
 *
 * 原始规则效果被移动到独立暂存字段，因此现有结算器继续只读取 `abilityEffects` 和 `itemEffects` 即可获得统一的
 * “当前可执行效果”口径；压制来源离场后再原样恢复，不会丢失从资料快照装配出的结构化规则。
 */
internal fun BattleState.synchronizePassiveSuppressions(): BattleState {
	val activeParticipants = sides.flatMap { it.activeParticipants() }.filter { it.canBattle() }
	val fieldSuppressionActive = activeParticipants.any { participant ->
		participant.allAbilityEffects().any { it is BattleAbilityEffect.FieldAbilitySuppression }
	}
	val synchronized = copy(
		sides = sides.map { side ->
			side.copy(
				participants = side.participants.map { participant ->
					val active = participant.actorId in side.activeActorIds && participant.canBattle()
					val allAbilityEffects = participant.allAbilityEffects()
					val suppressionImmune = allAbilityEffects.any { it.isFieldSuppressionImmune() }
					val suppressAbility = fieldSuppressionActive && active && !suppressionImmune
					val abilitySynchronized = when {
						suppressAbility && participant.abilityEffects.isNotEmpty() -> participant.copy(
							abilityEffects = emptyList(),
							suppressedAbilityEffects = participant.abilityEffects,
						)
						!suppressAbility && participant.suppressedAbilityEffects.isNotEmpty() -> participant.copy(
							abilityEffects = participant.suppressedAbilityEffects,
							suppressedAbilityEffects = emptyList(),
						)
						else -> participant
					}
					abilitySynchronized.synchronizeHeldItemSuppression()
				},
			)
		},
	)
	return synchronized.synchronizeWeatherForms().synchronizeTerrainElementIdentities()
}

/**
 * 标识现代规则中不能被化学变化气体关闭的身份与形态维持效果。
 *
 * 这些效果决定成员当前物种、形态或睡眠身份；若暂时移除，可能制造资料快照无法表达的非法中间态。全场压制
 * 本身也必须保留，否则两个气体来源会互相关闭并产生振荡。
 */
private fun BattleAbilityEffect.isFieldSuppressionImmune(): Boolean = when (this) {
	is BattleAbilityEffect.FieldAbilitySuppression,
	is BattleAbilityEffect.AlwaysTreatedAsleep,
	is BattleAbilityEffect.StanceChange,
	is BattleAbilityEffect.SwitchInFormChange,
	is BattleAbilityEffect.SwitchOutFormChange,
	is BattleAbilityEffect.DamageAbsorbingFormChange,
	is BattleAbilityEffect.PostSkillHpFormChange,
	is BattleAbilityEffect.ReceivedDamageFormRetaliation,
	is BattleAbilityEffect.EndTurnFormToggle,
	is BattleAbilityEffect.EndTurnHpFormChange,
	is BattleAbilityEffect.HeldItemElementIdentity,
	-> true
	else -> false
}

/** 根据当前有效特性在可执行字段和暂存字段之间移动道具效果。 */
internal fun BattleParticipant.synchronizeHeldItemSuppression(): BattleParticipant {
	val suppressItem = abilityEffects.any { it is BattleAbilityEffect.HeldItemEffectSuppression }
	return when {
		suppressItem && itemEffects.isNotEmpty() -> copy(
			itemEffects = emptyList(),
			suppressedItemEffects = itemEffects,
			choiceLockedSkillId = null,
		)
		!suppressItem && suppressedItemEffects.isNotEmpty() -> copy(
			itemEffects = suppressedItemEffects,
			suppressedItemEffects = emptyList(),
		)
		else -> this
	}
}
