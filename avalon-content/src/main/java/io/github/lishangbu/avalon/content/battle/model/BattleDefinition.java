package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * BattleCatalog 使用的类型化配置，声明growth、skills、encounters、products等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param growth 成长声明
 * @param skills 技能声明列表
 * @param encounters 遭遇声明列表
 * @param products 产物声明列表
 */
public record BattleDefinition(Map<String, BattleGrowth> growth, List<BattleSkill> skills,
		List<BattleEncounter> encounters, List<BattleProduct> products, Map<String, Map<String, Double>> elements,
		Map<String, Integer> supplyDrops, Map<String, Integer> bookDrops) {
	public BattleDefinition {
		growth = Map.copyOf(growth);
		skills = List.copyOf(skills);
		encounters = List.copyOf(encounters);
		products = List.copyOf(products);
		var copiedElements = new LinkedHashMap<String, Map<String, Double>>();
		elements.forEach((element, multipliers) -> copiedElements.put(element, Map.copyOf(multipliers)));
		elements = Map.copyOf(copiedElements);
		supplyDrops = Map.copyOf(supplyDrops);
		bookDrops = Map.copyOf(bookDrops);
	}
}
