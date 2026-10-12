package io.github.lishangbu.avalon.content.life.model;

import java.util.List;

/**
 * GatheringCatalog 使用的类型化配置，声明nodes等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 */
public record GatheringDefinition(List<GatheringNode> nodes) {
	public GatheringDefinition {
		nodes = List.copyOf(nodes);
	}
}
