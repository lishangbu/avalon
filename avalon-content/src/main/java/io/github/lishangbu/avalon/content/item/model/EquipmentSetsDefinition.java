package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * EquipmentSetCatalog 使用的类型化配置，声明sets等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 */
public record EquipmentSetsDefinition(List<EquipmentSetDefinition> sets) {
	public EquipmentSetsDefinition {
		sets = List.copyOf(sets);
	}
}
