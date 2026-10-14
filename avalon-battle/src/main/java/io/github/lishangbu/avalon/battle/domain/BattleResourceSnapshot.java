package io.github.lishangbu.avalon.battle.domain;

import java.util.*;

/** 执行前由宿主收集的只读资源输入；回合内的规则不能重新查询真实库存或捕获容量。 */
public record BattleResourceSnapshot(Map<String, Map<String, Long>> items, Set<String> capturable) {
	/** 深复制嵌套库存与可捕获目标，隔离宿主可变数据。 */
	public BattleResourceSnapshot {
		var copy = new HashMap<String, Map<String, Long>>();
		items.forEach((actor, values) -> copy.put(actor, Map.copyOf(values)));
		items = Map.copyOf(copy);
		capturable = Set.copyOf(capturable);
	}

	/** 在角色/副本锁内收集所有可能用到的物品；这里只读，不扣除资产。 */
	public static BattleResourceSnapshot read(BattleState state, List<BattleStateOrder> orders,
			BattleEngineResources source) {
		var items = new HashMap<String, Map<String, Long>>();
		var codes = new HashSet<String>();
		if (orders != null)
			orders.forEach(order -> {
				if (order != null && order.action() != null)
					codes.add(order.action());
			});
		for (var actor : state.getUnits())
			if (!actor.isEnemy() && actor.isHero()) {
				var counts = new HashMap<String, Long>();
				for (String code : codes)
					counts.put(code, (long) source.count(actor, code));
				items.put(actor.getId(), counts);
			}
		var targets = new HashSet<String>();
		for (var target : state.getUnits())
			if (target.isEnemy() && source.room(target))
				targets.add(target.getId());
		return new BattleResourceSnapshot(items, targets);
	}
}
