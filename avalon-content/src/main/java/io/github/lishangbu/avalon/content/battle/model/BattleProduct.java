package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;

/**
 * BattleCatalog 使用的类型化配置，声明code、price、requires、locations等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param code 内容目录中的稳定代码
 * @param price 价格
 * @param requires 开放前置引用
 * @param locations 允许的地点代码
 */
public record BattleProduct(String code, int price, String requires, List<String> locations) {
	public BattleProduct {
		locations = locations == null ? List.of() : List.copyOf(locations);
	}
}
