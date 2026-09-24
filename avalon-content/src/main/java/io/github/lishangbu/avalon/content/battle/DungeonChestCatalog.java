package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.function.ToIntFunction;

/** 发起者宝箱独立于每日参与奖励，权重和来源回收价在启动时检查。 */
@Component
public class DungeonChestCatalog {

	private final Definition definition;

	public DungeonChestCatalog(ItemCatalog items, DungeonCatalog dungeons) {
		definition = ContentLoader.read("/content/dungeon-chests.json", Definition.class);
		validate(definition, items, dungeons.definition().id());
	}

	public static void validate(Definition value, ItemCatalog items, String dungeon) {
		if (!dungeon.equals(value.dungeon()) || value.name() == null || value.name().isBlank())
			throw new IllegalArgumentException("宝箱副本引用或名称无效");
		weights(value.equipment(), Equipment::weight);
		weights(value.extraCounts(), Count::weight);
		weights(value.extras(), Extra::weight);
		var codes = new HashSet<String>();
		for (var gear : value.equipment())
			if (!codes.add(gear.code()) || !items.hasEquipment(gear.code()) || gear.sellPrice() < 0)
				throw new IllegalArgumentException("宝箱装备引用、回收价或重复项无效");
		var counts = new HashSet<Integer>();
		for (var count : value.extraCounts())
			if (!counts.add(count.amount()) || count.amount() < 1 || count.amount() > 2)
				throw new IllegalArgumentException("宝箱额外奖励项数无效");
		codes.clear();
		for (var extra : value.extras())
			if (!codes.add(extra.code()) || extra.amount() < 1 || (!"GOLD".equals(extra.code())
					&& (!items.hasItem(extra.code()) || items.item(extra.code()).task())))
				throw new IllegalArgumentException("宝箱额外奖励引用或数量无效");
	}

	private static <T> void weights(List<T> entries, ToIntFunction<T> weight) {
		if (entries.isEmpty() || entries.stream().anyMatch(entry -> weight.applyAsInt(entry) <= 0)
				|| entries.stream().mapToLong(weight::applyAsInt).sum() != 100)
			throw new IllegalArgumentException("宝箱池权重必须为正且总和为100");
	}

	public Definition definition() {
		return definition;
	}

	public record Equipment(String code, int weight, int sellPrice) {
	}

	public record Count(int amount, int weight) {
	}

	public record Extra(String code, int amount, int weight, boolean bound) {
	}

	public record Definition(String dungeon, String name, List<Equipment> equipment, List<Count> extraCounts,
			List<Extra> extras) {
		public Definition {
			equipment = equipment == null ? List.of() : List.copyOf(equipment);
			extraCounts = extraCounts == null ? List.of() : List.copyOf(extraCounts);
			extras = extras == null ? List.of() : List.copyOf(extras);
		}
	}

}
