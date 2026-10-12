package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LearningCatalog 使用的类型化配置，声明pools等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param pools 奖池声明列表
 */
public record LearningDefinition(Map<String, List<String>> pools) {
	public LearningDefinition {
		pools = pools.entrySet()
			.stream()
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
	}
}
