package io.github.lishangbu.avalon.battle.domain;

import java.util.List;
import java.util.Map;

/**
 * DungeonRoundState 使用的类型化规则数据，包含orders、replacements等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param orders 已声明的行动指令
 * @param replacements 换位或替换声明
 */
public record DungeonRoundStateSubmission(List<BattleStateOrder> orders, Map<String, Integer> replacements) {
	public DungeonRoundStateSubmission {
		orders = List.copyOf(orders);
		replacements = Map.copyOf(replacements);
	}
}
