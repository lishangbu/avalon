package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * EquipmentCatalog 使用的类型化配置，声明shops、offers等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param shops 商店地点声明列表
 * @param offers 可用供应项
 */
public record EquipmentDefinition(List<String> shops, List<EquipmentOffer> offers) {
	public EquipmentDefinition {
		shops = List.copyOf(shops);
		offers = List.copyOf(offers);
	}
}
