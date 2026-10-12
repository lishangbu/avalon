package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;
import java.util.Map;

/**
 * CollectionCatalog 使用的类型化配置，声明species、milestoneSpecies、milestones、speciesRewards等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param species 幻兽物种代码
 */
public record CollectionDefinition(CollectionReward species, List<String> milestoneSpecies,
		List<CollectionMilestone> milestones, Map<String, CollectionReward> speciesRewards) {
	public CollectionDefinition {
		milestoneSpecies = List.copyOf(milestoneSpecies);
		milestones = List.copyOf(milestones);
		speciesRewards = speciesRewards == null ? Map.of() : Map.copyOf(speciesRewards);
	}
}
