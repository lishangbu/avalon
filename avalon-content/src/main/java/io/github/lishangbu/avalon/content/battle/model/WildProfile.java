package io.github.lishangbu.avalon.content.battle.model;

import java.util.Map;

/**
 * WildCatalog 使用的类型化配置，声明species、minPlayerLevel、elite、healthMultiplier等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param species 幻兽物种代码
 * @param minPlayerLevel 角色等级下界
 */
public record WildProfile(Map<String, WildSpawn> species, int minPlayerLevel, boolean elite, int healthMultiplier,
		double attackMultiplier, int experienceMultiplier, int goldMultiplier, Map<String, Integer> bookDrops,
		Map<String, Integer> supplyDrops) {
	public WildProfile {
		species = Map.copyOf(species);
		bookDrops = bookDrops == null ? Map.of() : Map.copyOf(bookDrops);
		supplyDrops = supplyDrops == null ? Map.of() : Map.copyOf(supplyDrops);
	}
}
