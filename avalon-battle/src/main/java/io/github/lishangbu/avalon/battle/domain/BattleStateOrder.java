package io.github.lishangbu.avalon.battle.domain;

import java.util.List;

/**
 * BattleState 使用的类型化规则数据，包含actor、action、target、targets等字段。
 * 保持原有数据语义和构造规则；拆为顶层类型后，调用方无需依赖规则实现的内部声明。
 *
 * @param actor 行动单位编号
 * @param action 行动代码
 * @param target 目标编号或目标声明
 * @param targets 目标编号列表
 */
public record BattleStateOrder(String actor, String action, String target, List<String> targets) {
	public BattleStateOrder(String actor, String action, String target) {
		this(actor, action, target, List.of());
	}
}
