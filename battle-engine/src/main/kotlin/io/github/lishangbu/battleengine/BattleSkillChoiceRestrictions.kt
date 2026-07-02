package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleSkillHpEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot

/**
 * 技能选择限制的共享谓词。
 *
 * 这些谓词同时被“提交前校验”和“行动前结算”使用：提交前校验要给接口或管理端返回稳定错误码，行动前结算要在
 * replay 中追加真实的阻止事件。两边必须对“回复封锁是否限制该技能”和“挑衅是否限制该技能”保持同一口径。
 *
 * 这里只根据结构化技能槽判断，不读取技能名称、本地化文本或数据库资料。资料导入层负责把吸取回复、自我回复、
 * 天气/场地变量回复、目标回复等规则映射为 [BattleSkillHpEffect]，引擎只消费已经冻结的运行态。
 */
internal fun healBlockPreventsSkill(skill: BattleSkillSlot): Boolean =
	skill.hpEffects.any { effect ->
		effect is BattleSkillHpEffect.SelfHealMaxHpFraction ||
			effect is BattleSkillHpEffect.SelfHealMaxHpByWeather ||
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
