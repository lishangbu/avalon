package io.github.lishangbu.avalon.inventory.application.model;

import io.github.lishangbu.avalon.inventory.domain.CultivationRewardRulesReward;
import java.util.List;

/**
 * CultivationRewardService 使用的命令或结果数据，包含rewards等字段。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param rewards 奖励声明列表
 */
public record CultivationRewardResult(List<CultivationRewardRulesReward> rewards) {
	public CultivationRewardResult {
		rewards = List.copyOf(rewards);
	}
}
