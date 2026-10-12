package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * CraftingCatalog 使用的类型化配置，声明recipes等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param recipes 配方声明列表
 */
public record CraftingDefinition(List<CraftingRecipe> recipes) {
	public CraftingDefinition {
		recipes = List.copyOf(recipes);
	}
}
