package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * 人物系数按生命、物攻、法攻、物防、法防、速度排序，魔力使用一次函数。 幻兽只保留技能及野外生成字段，属性由SpeciesStatsCatalog提供。
 */
public record BattleGrowth(List<Double> coefficients, int manaBase, int manaPerLevel, List<String> skills,
		double captureRate, int minLevel, int maxLevel, String book, String fragment) {
	public BattleGrowth {
		// null 表示未配置职业系数，必须保留以供幻兽配置校验。
		coefficients = coefficients == null ? null : List.copyOf(coefficients);
		skills = List.copyOf(skills);
	}
}
