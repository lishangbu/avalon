package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestCount;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestEquipment;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestExtra;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 胜利时一次性抽取。开箱不调用此规则；重复额外奖励合并数量，不重抽。 */
@Component
@RequiredArgsConstructor
public class DungeonChestRules {

	private final DungeonChestCatalog catalog;

	public DungeonChestRulesLoot roll(RandomGenerator random) {
		return roll(catalog.definition().dungeon(), random);
	}

	public DungeonChestRulesLoot roll(String dungeon, RandomGenerator random) {
		var definition = catalog.definition(dungeon);
		var gear = pick(definition.equipment(), DungeonChestEquipment::weight, random);
		int count = pick(definition.extraCounts(), DungeonChestCount::weight, random).amount();
		var extras = new LinkedHashMap<String, DungeonChestRulesExtra>();
		int gold = 0;
		for (int index = 0; index < count; index++) {
			var selected = pick(definition.extras(), DungeonChestExtra::weight, random);
			if (selected.code().equals("GOLD"))
				gold = Math.addExact(gold, selected.amount());
			else
				extras.merge(selected.code(),
						new DungeonChestRulesExtra(selected.code(), selected.amount(), selected.bound()),
						(first, second) -> new DungeonChestRulesExtra(first.code(),
								Math.addExact(first.amount(), second.amount()), first.bound()));
		}
		return new DungeonChestRulesLoot(gear.code(), gear.sellPrice(), gold, List.copyOf(extras.values()));
	}

	private static <T> T pick(List<T> pool, ToIntFunction<T> weight, RandomGenerator random) {
		int value = random.nextInt(100);
		for (var entry : pool) {
			value -= weight.applyAsInt(entry);
			if (value < 0)
				return entry;
		}
		throw new IllegalStateException("宝箱权重目录损坏");
	}

}
