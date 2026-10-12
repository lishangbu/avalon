package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * LifeProductionCatalog 使用的类型化配置，声明recipes、offers等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param recipes 配方声明列表
 * @param offers 可用供应项
 */
public record LifeProductionDefinition(List<LifeProductionRecipe> recipes, List<LifeProductionOffer> offers) {
	public LifeProductionDefinition {
		recipes = List.copyOf(recipes);
		offers = List.copyOf(offers);
	}
}
