package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * LifeProductionCatalog 使用的类型化配置，声明item、price、locations等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param item 物品代码或物品数据
 * @param price 价格
 * @param locations 允许的地点代码
 */
public record LifeProductionOffer(String item, int price, List<String> locations) {
	public LifeProductionOffer {
		locations = List.copyOf(locations);
	}
}
