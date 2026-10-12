package io.github.lishangbu.avalon.content.life.model;

import java.util.List;
import java.util.Map;

/**
 * LifeProductionCatalog 使用的类型化配置，声明id、skill、level、product等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param skill 技能代码
 * @param level 等级
 * @param product 产物代码
 * @param quantity 物品或资源数量
 * @param points 规则使用的点数
 * @param ingredients 原料声明
 * @param locations 允许的地点代码
 */
public record LifeProductionRecipe(String id, String skill, int level, String product, int quantity, int points,
		Map<String, Integer> ingredients, List<String> locations) {
	public LifeProductionRecipe {
		ingredients = Map.copyOf(ingredients);
		locations = List.copyOf(locations);
	}
}
