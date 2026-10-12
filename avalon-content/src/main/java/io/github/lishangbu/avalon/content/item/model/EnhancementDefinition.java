package io.github.lishangbu.avalon.content.item.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * EnhancementCatalog 使用的类型化配置，声明playerLevel、locations、maximumLevel、successPercent等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param playerLevel 角色等级
 * @param locations 允许的地点代码
 * @param maximumLevel 等级上界
 * @param fees 费用档位声明
 * @param stones 强化石声明列表
 */
public record EnhancementDefinition(int playerLevel, List<String> locations, int maximumLevel,
		List<Integer> successPercent, int statPercentPerLevel, List<EnhancementFee> fees,
		Map<String, BigDecimal> qualityMultipliers, List<EnhancementStone> stones) {
	public EnhancementDefinition {
		locations = List.copyOf(locations);
		successPercent = List.copyOf(successPercent);
		fees = List.copyOf(fees);
		qualityMultipliers = Map.copyOf(qualityMultipliers);
		stones = List.copyOf(stones);
	}
}
