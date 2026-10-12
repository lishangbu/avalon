package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * EquipmentCatalog 使用的类型化配置，声明code、requires、locations、price等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param code 内容目录中的稳定代码
 * @param requires 开放前置引用
 * @param locations 允许的地点代码
 * @param price 价格
 */
public record EquipmentOffer(String code, String requires, List<String> locations, int price) {
	public EquipmentOffer {
		locations = locations == null ? List.of() : List.copyOf(locations);
	}
}
