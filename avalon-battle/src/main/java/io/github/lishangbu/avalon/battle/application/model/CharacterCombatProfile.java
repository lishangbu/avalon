package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;

import java.util.List;

/**
 * 角色页的只读战斗档案。属性为正式等级与当前装备的常驻值，不包含战局内临时效果； 技能仅包含角色已掌握的职业技能，不暴露持久化实体或客户端可修改的战斗单位。
 */
public record CharacterCombatProfile(Stats stats, List<Skill> skills) {

	public CharacterCombatProfile {
		skills = List.copyOf(skills);
	}

	/** 冷却按完整后续回合计数，和战斗配置保持同一语义。 */
	public record Skill(String name, String element, int mana, int cooldown) {
	}
}
