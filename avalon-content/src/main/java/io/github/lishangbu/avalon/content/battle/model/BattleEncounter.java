package io.github.lishangbu.avalon.content.battle.model;

import java.util.Map;

/**
 * BattleCatalog 使用的类型化配置，声明id、name、location、requires等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param location 地点代码
 * @param requires 开放前置引用
 * @param captureRequires 捕捉所需前置
 * @param species 幻兽物种代码
 * @param boss 首领声明
 * @param count 业务计数
 */
public record BattleEncounter(String id, String name, String location, String requires, String captureRequires,
		boolean tutorial, Map<String, Integer> species, BattleBoss boss, String wild, int count) {
	public BattleEncounter {
		species = species == null ? Map.of() : Map.copyOf(species);
	}
}
