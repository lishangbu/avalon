package io.github.lishangbu.avalon.content.creature.model;

import java.util.List;

/**
 * EvolutionResearchCatalog 使用的类型化配置，声明token、offers等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param token 令牌代码或令牌数据
 * @param offers 可用供应项
 */
public record EvolutionResearchDefinition(String token, List<EvolutionResearchOfferConfig> offers) {
	public EvolutionResearchDefinition {
		offers = List.copyOf(offers);
	}
}
