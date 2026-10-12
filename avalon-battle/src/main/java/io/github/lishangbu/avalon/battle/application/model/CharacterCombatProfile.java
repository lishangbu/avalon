package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import java.util.List;

/**
 * 角色页的只读战斗档案。属性为正式等级与当前装备的常驻值，不包含战局内临时效果； 技能仅包含角色已掌握的职业技能，不暴露持久化实体或客户端可修改的战斗单位。
 */
public record CharacterCombatProfile(BattleStateStats stats, List<CharacterCombatProfileSkill> skills) {

	public CharacterCombatProfile {
		skills = List.copyOf(skills);
	}

}
