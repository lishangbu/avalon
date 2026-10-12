package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * DungeonCatalog 使用的类型化配置，声明id、name、wildSpecies、healthMultiplier等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param health 生命属性
 * @param attack 攻击属性或通道
 * @param defense 防御属性或通道
 * @param speed 速度属性
 * @param experience 经验数量
 * @param gold 金币数量
 * @param actions 允许的行动声明
 */
public record DungeonStage(String id, String name, List<String> wildSpecies, double healthMultiplier, int health,
		int attack, int defense, int speed, int guardHealth, int guardDefense, int experience, int gold,
		List<DungeonAction> actions) {
	public DungeonStage {
		wildSpecies = wildSpecies == null ? List.of() : List.copyOf(wildSpecies);
		actions = actions == null ? List.of() : List.copyOf(actions);
	}
}
