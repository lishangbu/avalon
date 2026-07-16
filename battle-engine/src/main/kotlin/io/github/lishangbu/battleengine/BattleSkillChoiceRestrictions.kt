package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/**
 * 技能选择限制的共享谓词。
 *
 * 这些谓词同时被“提交前校验”和“行动前结算”使用：提交前校验要给接口或管理端返回稳定错误码，行动前结算要在
 * replay 中追加真实的阻止事件。两边必须对“回复封锁是否限制该技能”和“挑衅是否限制该技能”保持同一口径。
 *
 * 这里只根据结构化技能槽判断，不读取技能名称、本地化文本或数据库资料。资料导入层负责把吸取回复、自我回复、
 * 天气/场地变量回复、目标回复、按目标攻击实数回复、治愈目标异常后回复等规则映射为 [BattleSkillHpEffect]，
 * 引擎只消费已经冻结的运行态。
 */
internal fun healBlockPreventsSkill(skill: BattleSkillSlot): Boolean =
	skill.hpEffects.any { effect ->
		effect is BattleSkillHpEffect.SelfHealMaxHpFraction ||
			effect is BattleSkillHpEffect.SelfHealMaxHpByWeather ||
			effect is BattleSkillHpEffect.SelfHealByTargetCurrentAttack ||
			effect is BattleSkillHpEffect.SelfHealAfterTargetMajorStatusCure ||
			effect is BattleSkillHpEffect.TargetHealMaxHpFraction ||
			effect is BattleSkillHpEffect.TargetHealMaxHpByTerrain ||
			effect is BattleSkillHpEffect.DrainDamage
	}

/**
 * 判断挑衅是否禁止选择该技能。
 *
 * 挑衅只限制变化分类技能；物理和特殊分类技能即便最终没有造成伤害，也应交给引擎继续结算命中、保护、免疫或技能
 * 自身规则，而不是在提交或行动前阶段提前当作非法。
 */
internal fun tauntPreventsSkill(skill: BattleSkillSlot): Boolean =
	skill.damageClass == BattleDamageClass.STATUS

/** 判断当前携带道具是否禁止选择给定技能。 */
internal fun BattleParticipant.heldItemPreventsSkill(skill: BattleSkillSlot): Boolean =
	itemEffects.any { it is BattleItemEffect.StatusSkillRestriction } && skill.damageClass == BattleDamageClass.STATUS

/**
 * 判断成员当前是否没有任何可由玩家正常提交的技能。
 *
 * 现代规则中，所有技能都因为 PP、讲究锁定、回复封锁、挑衅、定身法或无理取闹等选择限制而不可选时，成员不会
 * 因为接口提交了某个原技能而直接报错；正式回合会自动改用内置的“挣扎”行动。把这个谓词放在共享选择限制文件中，
 * 可以让提交校验和行动规划使用完全一致的口径，避免管理端认为可提交、引擎却仍尝试消费原技能 PP。
 */
internal fun BattleParticipant.mustUseStruggle(): Boolean =
	skillSlots.none { skill -> canSubmitSkill(skill) }

/**
 * 判断单个技能槽当前是否能作为普通提交技能使用。
 *
 * 这里只覆盖已经建模为“选择阶段限制”的条件；睡眠、麻痹、混乱、保护、命中、属性免疫等会产生回放事件的运行时
 * 条件仍留在回合结算阶段处理，不参与“是否强制挣扎”的选择集合。
 */
private fun BattleParticipant.canSubmitSkill(skill: BattleSkillSlot): Boolean =
	skill.remainingPp > 0 &&
		!choiceLockedToAnotherSkill(skill.skillId) &&
		!(healBlockTurnsRemaining > 0 && healBlockPreventsSkill(skill)) &&
		!(tauntTurnsRemaining > 0 && tauntPreventsSkill(skill)) &&
		!heldItemPreventsSkill(skill) &&
		!(disabledSkillTurnsRemaining > 0 && disabledSkillId == skill.skillId) &&
		!(tormented && lastSuccessfulSkillId == skill.skillId)
