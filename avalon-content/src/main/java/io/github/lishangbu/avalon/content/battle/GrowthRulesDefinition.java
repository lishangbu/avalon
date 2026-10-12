package io.github.lishangbu.avalon.content.battle;

import java.util.List;

/**
 * GrowthRules 使用的类型化配置，声明thresholds等数据。 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 */
public record GrowthRulesDefinition(List<Long> thresholds) {
	public GrowthRulesDefinition {
		thresholds = List.copyOf(thresholds);
	}
}
