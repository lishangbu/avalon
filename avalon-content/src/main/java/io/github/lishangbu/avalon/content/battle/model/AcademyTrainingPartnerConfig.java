package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/** 固定借用数据不读取真实装备和已学技能，不在真实幻兽表创建实体。 */
public record AcademyTrainingPartnerConfig(String id, String species, String name, String element, List<Integer> stats,
		List<String> skills) {
	public AcademyTrainingPartnerConfig {
		stats = List.copyOf(stats);
		skills = List.copyOf(skills);
	}
}
