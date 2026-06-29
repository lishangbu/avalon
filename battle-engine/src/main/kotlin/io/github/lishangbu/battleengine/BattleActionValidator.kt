package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleState

/**
 * 回合行动提交前的结构化校验器。
 *
 * [BattleEngine.resolveTurn] 假定调用方传入的是已经被规则层接受的行动，因此遇到明显非法输入时会使用
 * `require` 保护状态机不变量。真实对战服务和管理端调试工具需要更友好的前置校验：一次性返回所有问题，
 * 并用稳定 code 让前端或接口层映射成提示文案。
 *
 * 该校验器只依赖当前 [BattleState] 和本回合提交的 [BattleAction]，不访问数据库，也不读取资料表文本。
 * 它检查的是“能否把这批行动交给引擎结算”的通用条件：战斗是否已结束、同一成员是否重复提交、行动者是否
 * 存在并在场、技能是否可用、PP 是否足够、讲究类锁招、回复封锁、挑衅、定身法和无理取闹是否限制选择、
 * 蓄力/休整是否禁止主动替换，以及替换目标是否属于同一方且可以上场。
 *
 * 复杂规则仍由引擎结算阶段处理。例如命中、保护、属性免疫、精神场地阻挡、睡眠/麻痹/畏缩等会产生事件的
 * 运行时效果，不应在这里提前当成非法输入；否则 replay 会失去这些可观察事实。
 */
class BattleActionValidator {
	/**
	 * 返回本回合行动列表中的全部提交问题。
	 */
	fun validate(state: BattleState, actions: List<BattleAction>): List<BattleActionViolation> {
		val duplicatedActorIds = actions
			.groupingBy { it.actorId }
			.eachCount()
			.filterValues { it > 1 }
			.keys
		return actions.flatMap { action -> validateAction(state, action, duplicatedActorIds) }
	}

	/**
	 * 若存在提交问题则抛出异常。
	 *
	 * 该方法适合内部测试或命令式调用；需要字段级展示时，应使用 [validate] 获取结构化列表。
	 */
	fun requireValid(state: BattleState, actions: List<BattleAction>) {
		val violations = validate(state, actions)
		require(violations.isEmpty()) {
			violations.joinToString(separator = "; ") { it.message }
		}
	}

	private fun validateAction(
		state: BattleState,
		action: BattleAction,
		duplicatedActorIds: Set<String>,
	): List<BattleActionViolation> {
		val violations = mutableListOf<BattleActionViolation>()
		if (state.result != null) {
			violations += violation("battle-ended", action.actorId, message = "战斗已经结束，不能继续提交行动")
		}
		if (action.actorId in duplicatedActorIds) {
			violations += violation("duplicate-action", action.actorId, message = "同一成员在同一回合只能提交一个行动")
		}

		val actor = state.participant(action.actorId)
		if (actor == null) {
			violations += violation("actor-not-found", action.actorId, message = "行动成员不存在: ${action.actorId}")
			return violations
		}
		if (!state.isActive(actor.actorId)) {
			violations += violation("actor-not-active", actor.actorId, message = "行动成员当前不在场: ${actor.actorId}")
			return violations
		}
		if (!actor.canBattle()) {
			if (action is BattleAction.SwitchParticipant) {
				return violations + validateSwitch(state, action, actor)
			}
			return violations + violation("actor-fainted", actor.actorId, message = "行动成员已经无法战斗: ${actor.actorId}")
		}

		when (action) {
			is BattleAction.UseSkill -> violations += validateUseSkill(state, action, actor)
			is BattleAction.SwitchParticipant -> violations += validateSwitch(state, action, actor)
		}
		return violations
	}

	private fun validateUseSkill(
		state: BattleState,
		action: BattleAction.UseSkill,
		actor: BattleParticipant,
	): List<BattleActionViolation> {
		val violations = mutableListOf<BattleActionViolation>()
		val skill = actor.skillSlot(action.skillId)
		if (skill == null) {
			violations += violation(
				code = "skill-not-found",
				actorId = actor.actorId,
				resourceId = action.skillId,
				message = "成员未携带技能: ${action.skillId}",
			)
		} else {
			if (skill.remainingPp <= 0) {
				violations += violation(
					code = "skill-no-pp",
					actorId = actor.actorId,
					resourceId = skill.skillId,
					message = "技能 PP 已耗尽: ${skill.skillId}",
				)
			}
			if (actor.choiceLockedToAnotherSkill(skill.skillId)) {
				violations += violation(
					code = "choice-locked",
					actorId = actor.actorId,
					resourceId = actor.choiceLockedSkillId,
					message = "成员受讲究类道具限制，只能选择技能: ${actor.choiceLockedSkillId}",
				)
			}
			if (actor.healBlockTurnsRemaining > 0 && healBlockPreventsSkill(skill.hpEffects)) {
				violations += violation(
					code = "heal-blocked",
					actorId = actor.actorId,
					resourceId = skill.skillId,
					message = "成员处于回复封锁状态，不能选择回复类技能: ${skill.skillId}",
				)
			}
			if (actor.tauntTurnsRemaining > 0 && tauntPreventsSkill(skill.damageClass)) {
				violations += violation(
					code = "taunted",
					actorId = actor.actorId,
					resourceId = skill.skillId,
					message = "成员处于挑衅状态，不能选择变化技能: ${skill.skillId}",
				)
			}
			if (actor.disabledSkillTurnsRemaining > 0 && actor.disabledSkillId == skill.skillId) {
				violations += violation(
					code = "disabled-skill",
					actorId = actor.actorId,
					resourceId = skill.skillId,
					message = "技能处于定身法状态，暂时不能选择: ${skill.skillId}",
				)
			}
			if (actor.tormented && actor.lastSuccessfulSkillId == skill.skillId) {
				violations += violation(
					code = "tormented-repeat",
					actorId = actor.actorId,
					resourceId = skill.skillId,
					message = "成员处于无理取闹状态，不能连续选择同一个技能: ${skill.skillId}",
				)
			}
		}

		val target = state.participant(action.targetActorId)
		if (target == null) {
			violations += violation(
				code = "target-not-found",
				actorId = actor.actorId,
				targetActorId = action.targetActorId,
				message = "目标成员不存在: ${action.targetActorId}",
			)
		} else if (!state.isActive(target.actorId)) {
			violations += violation(
				code = "target-not-active",
				actorId = actor.actorId,
				targetActorId = target.actorId,
				message = "目标成员当前不在场: ${target.actorId}",
			)
		} else if (!target.canBattle()) {
			violations += violation(
				code = "target-fainted",
				actorId = actor.actorId,
				targetActorId = target.actorId,
				message = "目标成员已经无法战斗: ${target.actorId}",
			)
		}
		return violations
	}

	/**
	 * 判断回复封锁是否禁止提交该技能。
	 *
	 * 这里只看技能自身携带的 HP 回复效果，不读取技能名称或本地化文本；资料层负责把吸取回复和自我回复技能
	 * 转换成 [BattleSkillHpEffect]。建立替身和反作用伤害不属于回复类技能，仍允许提交。
	 */
	private fun healBlockPreventsSkill(hpEffects: List<BattleSkillHpEffect>): Boolean =
		hpEffects.any { effect ->
			effect is BattleSkillHpEffect.SelfHealMaxHpFraction ||
				effect is BattleSkillHpEffect.SelfHealMaxHpByWeather ||
				effect is BattleSkillHpEffect.DrainDamage
		}

	/**
	 * 判断挑衅是否禁止提交该技能。
	 *
	 * 挑衅只限制变化分类技能；攻击分类技能即便最终没有造成伤害，也应交给引擎继续结算。
	 */
	private fun tauntPreventsSkill(damageClass: BattleDamageClass): Boolean =
		damageClass == BattleDamageClass.STATUS

	private fun validateSwitch(
		state: BattleState,
		action: BattleAction.SwitchParticipant,
		actor: BattleParticipant,
	): List<BattleActionViolation> {
		val side = state.sideOf(actor.actorId) ?: return listOf(
			violation("actor-not-found", actor.actorId, message = "行动成员不存在: ${actor.actorId}"),
		)
		val violations = mutableListOf<BattleActionViolation>()
		if (actor.lockedMoveTurnsRemaining > 0) {
			violations += violation(
				code = "locked-move-prevents-switch",
				actorId = actor.actorId,
				resourceId = actor.lockedMoveSkillId,
				message = "锁招期间不能主动替换: ${actor.lockedMoveSkillId}",
			)
		}
		if (actor.rechargeTurnsRemaining > 0) {
			violations += violation(
				code = "recharge-prevents-switch",
				actorId = actor.actorId,
				message = "成员正在休整，不能主动替换",
			)
		}
		if (actor.chargingTurnsRemaining > 0) {
			violations += violation(
				code = "charging-prevents-switch",
				actorId = actor.actorId,
				resourceId = actor.chargingSkillId,
				message = "成员正在蓄力，不能主动替换: ${actor.chargingSkillId}",
			)
		}
		val targetSide = state.sideOf(action.targetActorId)
		val target = targetSide?.participant(action.targetActorId)
		when {
			target == null -> violations += violation(
				code = "switch-target-not-found",
				actorId = actor.actorId,
				targetActorId = action.targetActorId,
				message = "替换目标不存在: ${action.targetActorId}",
			)
			targetSide.sideId != side.sideId -> violations += violation(
				code = "switch-target-opponent",
				actorId = actor.actorId,
				targetActorId = target.actorId,
				message = "替换目标必须属于同一方: ${target.actorId}",
			)
			targetSide.isActive(target.actorId) -> violations += violation(
				code = "switch-target-active",
				actorId = actor.actorId,
				targetActorId = target.actorId,
				message = "替换目标已经在场: ${target.actorId}",
			)
			!target.canBattle() -> violations += violation(
				code = "switch-target-fainted",
				actorId = actor.actorId,
				targetActorId = target.actorId,
				message = "替换目标已经无法战斗: ${target.actorId}",
			)
		}
		return violations
	}

	private fun violation(
		code: String,
		actorId: String,
		targetActorId: String? = null,
		resourceId: Long? = null,
		message: String,
	): BattleActionViolation =
		BattleActionViolation(
			code = code,
			actorId = actorId,
			targetActorId = targetActorId,
			resourceId = resourceId,
			message = message,
		)
}

/**
 * 一条回合行动提交违规。
 *
 * `code` 是稳定机器码；`actorId` 定位提交行动的成员；`targetActorId` 在目标或替换目标相关问题中提供；
 * `resourceId` 用于技能、锁定技能等数值资料；`message` 是面向管理端或调试报告的简体中文说明。
 */
data class BattleActionViolation(
	val code: String,
	val actorId: String,
	val targetActorId: String? = null,
	val resourceId: Long? = null,
	val message: String,
)
