package io.github.lishangbu.avalon.world.domain;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 先规划完整一轮补给；任一种资源无法补足就不消耗任何药。 */
public final class GatheringSupplyRules {

	private GatheringSupplyRules() {
	}

	public static Plan plan(int hp, int mp, int maxHp, int maxMp, int healthCost, int manaCost, List<Supply> supplies) {
		int healthNeeded = Math.max(0, healthCost + 1 - hp), manaNeeded = Math.max(0, manaCost - mp);
		if (maxHp <= healthCost || maxMp < manaCost)
			return new Plan(false, Map.of(), 0, 0);
		var consumed = new TreeMap<Long, Integer>();
		int restoredHealth = 0, restoredMana = 0;
		for (var supply : supplies) {
			int needed = supply.health() > 0 ? healthNeeded : manaNeeded;
			int restores = supply.health() > 0 ? supply.health() : supply.mana();
			if (needed <= 0 || restores <= 0)
				continue;
			int count = Math.min(supply.quantity(), (needed + restores - 1) / restores);
			if (count <= 0)
				continue;
			consumed.put(supply.id(), count);
			restoredHealth += count * supply.health();
			restoredMana += count * supply.mana();
			healthNeeded = Math.max(0, healthCost + 1 - hp - restoredHealth);
			manaNeeded = Math.max(0, manaCost - mp - restoredMana);
		}
		return healthNeeded == 0 && manaNeeded == 0 ? new Plan(true, Map.copyOf(consumed), restoredHealth, restoredMana)
				: new Plan(false, Map.of(), 0, 0);
	}

	public record Supply(long id, int quantity, int health, int mana) {
	}

	public record Plan(boolean sufficient, Map<Long, Integer> consumed, int health, int mana) {
	}

}
