package io.github.lishangbu.avalon.inventory.domain;

import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.random.RandomGenerator;

/** 胜利时一次性抽取。开箱不调用此规则；重复额外奖励合并数量，不重抽。 */
@Component
@RequiredArgsConstructor
public class DungeonChestRules {

	private final DungeonChestCatalog catalog;

	public Loot roll(RandomGenerator random) {
		var definition = catalog.definition();
		var gear = pick(definition.equipment(), DungeonChestCatalog.Equipment::weight, random);
		int count = pick(definition.extraCounts(), DungeonChestCatalog.Count::weight, random).amount();
		var extras = new LinkedHashMap<String, Extra>();
		int gold = 0;
		for (int index = 0; index < count; index++) {
			var selected = pick(definition.extras(), DungeonChestCatalog.Extra::weight, random);
			if (selected.code().equals("GOLD"))
				gold = Math.addExact(gold, selected.amount());
			else
				extras.merge(selected.code(), new Extra(selected.code(), selected.amount(), selected.bound()),
						(first, second) -> new Extra(first.code(), Math.addExact(first.amount(), second.amount()),
								first.bound()));
		}
		return new Loot(gear.code(), gear.sellPrice(), gold, List.copyOf(extras.values()));
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

	public record Extra(String code, int amount, boolean bound) {
	}

	public record Loot(String equipment, int sellPrice, int gold, List<Extra> extras) {
		public Loot {
			extras = List.copyOf(extras);
		}
	}

}
