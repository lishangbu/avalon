package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;

/**
 * EvolutionCatalog 使用的类型化配置，声明playerLevel、routes、locations等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param playerLevel 角色等级
 * @param routes 演化路线声明列表
 * @param locations 允许的地点代码
 */
public record EvolutionDefinition(int playerLevel, List<EvolutionRoute> routes, List<String> locations) {
	public EvolutionDefinition {
		routes = List.copyOf(routes);
		locations = locations == null ? List.of() : List.copyOf(locations);
	}

	public EvolutionDefinition(int playerLevel, List<EvolutionRoute> routes) {
		this(playerLevel, routes, List.of());
	}
}
