package io.github.lishangbu.avalon.content.battle.model;

import java.util.Map;

/**
 * CultivationRewardCatalog
 * 使用的类型化配置，声明researchToken、researchQuantity、commissionStones、dungeonStones等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param researchToken 研究令牌物品代码
 * @param stoneI 第一档强化石引用
 * @param stoneII 第二档强化石引用
 * @param routes 演化路线声明列表
 */
public record CultivationRewardDefinition(String researchToken, int researchQuantity, int commissionStones,
		int dungeonStones, String stoneI, String stoneII, int commonPercent, int specialPercent, int bookPercent,
		Map<String, CultivationRewardPool> routes) {
	public CultivationRewardDefinition {
		routes = Map.copyOf(routes);
	}
}
