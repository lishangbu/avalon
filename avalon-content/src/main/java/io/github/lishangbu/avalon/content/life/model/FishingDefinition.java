package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * FishingCatalog 使用的类型化配置，声明bait、fish等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param bait 鱼饵声明
 * @param fish 鱼类声明或代码
 */
public record FishingDefinition(String bait, List<FishingFish> fish) {
	public FishingDefinition {
		fish = List.copyOf(fish);
	}
}
