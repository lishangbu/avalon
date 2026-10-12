package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * ItemCatalog 使用的类型化配置，声明items等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param items 物品声明列表
 */
public record ItemDefinition(List<ItemItem> items) {
	public ItemDefinition {
		items = List.copyOf(items);
	}
}
