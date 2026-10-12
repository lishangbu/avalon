package io.github.lishangbu.avalon.content.creature.model;

import java.util.Map;

/**
 * EvolutionCatalog 使用的类型化配置，声明source、target、name、level等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param target 目标编号或目标声明
 * @param name 展示名称
 * @param level 等级
 * @param materials 材料需求或材料投影
 * @param skill 技能代码
 * @param description 玩家可读说明
 */
public record EvolutionRoute(String source, String target, String name, int level, Map<String, Integer> materials,
		String skill, String description) {
	public EvolutionRoute(String source, String target, String name, int level, Map<String, Integer> materials,
			String skill) {
		this(source, target, name, level, materials, skill, null);
	}

	public EvolutionRoute {
		materials = Map.copyOf(materials);
	}

}
