package io.github.lishangbu.avalon.inventory.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 背包净空间计算。库存来源记录不等于背包格子：同名同绑定物品合并计算，每九十九个占一格。 装备逐件占格，任务栏独立不占背包；金币由角色领域管理，不传入本规则。
 * 合成调用者传入扣除材料并加入成品后的投影，不能只检查操作前空格。
 */
public final class BagCapacityRules {

	public static final int CAPACITY = 60;

	public static final int STACK_LIMIT = 99;

	private BagCapacityRules() {
	}

	public static int slots(List<BagCapacityRulesEntry> entries) {
		long equipment = 0;
		Map<Key, Long> stacks = new HashMap<>();
		for (var entry : entries) {
			if (entry.quantity() < 0)
				throw new IllegalArgumentException("库存数量不能为负");
			if (entry.task())
				continue;
			if (entry.equipment())
				equipment = Math.addExact(equipment, entry.quantity());
			else
				stacks.merge(new Key(entry.code(), entry.bound()), (long) entry.quantity(), Math::addExact);
		}
		long used = equipment;
		for (long quantity : stacks.values())
			used = Math.addExact(used, (quantity + STACK_LIMIT - 1) / STACK_LIMIT);
		return Math.toIntExact(used);
	}

	/** 二分计算本次最多可装入数量；保留原库存，适用于大批量奖励而不逐个循环。 */
	public static int fits(List<BagCapacityRulesEntry> current, BagCapacityRulesEntry addition) {
		if (addition.quantity() < 0)
			throw new IllegalArgumentException("奖励数量不能为负");
		if (addition.task())
			return addition.quantity();
		int low = 0;
		int high = addition.quantity();
		var projected = new ArrayList<>(current);
		projected.add(addition);
		while (low < high) {
			int middle = low + (int) (((long) high - low + 1) / 2);
			projected.set(projected.size() - 1,
					new BagCapacityRulesEntry(addition.code(), addition.bound(), middle, addition.equipment(), false));
			if (slots(projected) <= CAPACITY)
				low = middle;
			else
				high = middle - 1;
		}
		return low;
	}

	private record Key(String code, boolean bound) {
	}

}
