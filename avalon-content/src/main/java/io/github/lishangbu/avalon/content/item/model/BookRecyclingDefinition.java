package io.github.lishangbu.avalon.content.item.model;

import java.util.List;

/**
 * BookRecyclingCatalog 使用的类型化配置，声明percent、locations等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param percent 百分比数值
 * @param locations 允许的地点代码
 */
public record BookRecyclingDefinition(int percent, List<String> locations) {
	public BookRecyclingDefinition {
		locations = List.copyOf(locations);
	}
}
