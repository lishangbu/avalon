package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * MarketCatalog 使用的类型化配置，声明playerLevel、locations等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param playerLevel 角色等级
 * @param locations 允许的地点代码
 */
public record MarketDefinition(int playerLevel, List<String> locations) {
	public MarketDefinition {
		locations = List.copyOf(locations);
	}
}
