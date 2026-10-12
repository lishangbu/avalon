package io.github.lishangbu.avalon.inventory.application.model;

import io.github.lishangbu.avalon.inventory.domain.EnhancementRulesQuote;
import java.util.Map;

/**
 * EnhancementPreviewService 使用的命令或结果数据，包含before、cost、mode、materials等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param before 操作前预览
 * @param cost 执行所需成本
 * @param materials 材料需求或材料投影
 * @param resultBound 结果资产的绑定状态
 */
public record EnhancementPreview(InventoryItemView before, EnhancementRulesQuote cost, CultivationMaterialMode mode,
		CultivationMaterialPlan materials, boolean resultBound, Map<String, Integer> successStats,
		Map<String, Integer> failureStats) {
	public EnhancementPreview {
		successStats = Map.copyOf(successStats);
		failureStats = Map.copyOf(failureStats);
	}
}
