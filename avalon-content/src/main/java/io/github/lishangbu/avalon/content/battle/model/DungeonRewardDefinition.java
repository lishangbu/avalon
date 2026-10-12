package io.github.lishangbu.avalon.content.battle.model;

import java.util.List;
import java.util.Map;

/**
 * DungeonRewardCatalog
 * 使用的类型化配置，声明completionExperience、firstCompletionExperience、dailyGold、dailySupplies等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param completionExperience 完成经验奖励
 * @param firstCompletionExperience 首次完成经验奖励
 * @param dailyGold 每日金币规则
 * @param dailySupplies 每日补给规则
 */
public record DungeonRewardDefinition(int completionExperience, int firstCompletionExperience, int dailyGold,
		Map<String, Integer> dailySupplies, List<String> supplyStages, List<DungeonRewardDrop> supplyPool) {
	public DungeonRewardDefinition {
		dailySupplies = dailySupplies == null ? Map.of() : Map.copyOf(dailySupplies);
		supplyStages = supplyStages == null ? List.of() : List.copyOf(supplyStages);
		supplyPool = supplyPool == null ? List.of() : List.copyOf(supplyPool);
	}
}
