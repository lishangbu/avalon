package io.github.lishangbu.avalon.inventory.application.model;

import java.util.Map;

/**
 * CultivationMaterialService 使用的命令或结果数据，包含quantities、bindsTarget等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param bindsTarget 是否会使目标资产绑定
 */
public record CultivationMaterialPlan(Map<Long, Integer> quantities, boolean bindsTarget) {
	public CultivationMaterialPlan {
		quantities = Map.copyOf(quantities);
	}
}
