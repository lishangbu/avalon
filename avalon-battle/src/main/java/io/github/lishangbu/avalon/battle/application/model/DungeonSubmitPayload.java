package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 多人提交的幂等载荷，固定成员本轮提交的全部参数。 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param requestId 请求UUID，重试必须保持不变
 * @param revision 提交窗口的状态修订号
 * @param round 回合编号
 * @param orders 已声明的行动指令
 * @param replacements 换位或替换声明
 */
public record DungeonSubmitPayload(UUID requestId, int revision, int round, List<BattleStateOrder> orders,
		Map<String, Integer> replacements) {
}
