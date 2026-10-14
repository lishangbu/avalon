package io.github.lishangbu.avalon.battle.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 完整回合的不可变外部输入；指令、补位及库存资格均固定，规则与重放使用同一份数据。 */
public record BattleRoundInput(List<BattleStateOrder> orders, boolean surrender, Map<String, Integer> replacements,
		BattleResourceSnapshot resources) {

	/** 复制集合，避免玩家请求对象或宿主库存被回合中的扩展规则修改。 */
	public BattleRoundInput {
		orders = orders == null ? null : List.copyOf(orders);
		replacements = Map.copyOf(replacements);
		Objects.requireNonNull(resources, "必须提供资源快照");
	}
}
