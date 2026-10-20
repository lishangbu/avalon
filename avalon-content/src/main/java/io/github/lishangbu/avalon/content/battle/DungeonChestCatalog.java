package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestCount;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestDefinition;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestEquipment;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestExtra;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 发起者宝箱独立于每日参与奖励，权重和来源回收价在启动时检查。 */
@Component
public class DungeonChestCatalog {

	private final DungeonChestDefinition definition;

	private final Map<String, DungeonChestDefinition> definitions;

	@Autowired
	public DungeonChestCatalog(ItemCatalog items, DungeonCatalog dungeons) {
		this(items, dungeons, production(dungeons));
	}

	private static Map<String, DungeonChestDefinition> production(DungeonCatalog dungeons) {
		var values = new java.util.LinkedHashMap<String, DungeonChestDefinition>();
		for (var bundle : DungeonDocuments.current())
			if (dungeons.contains(bundle.dungeon().id()))
				values.put(bundle.dungeon().id(), bundle.chests());
		return values;
	}

	public DungeonChestCatalog(ItemCatalog items, DungeonCatalog dungeons, Map<String, DungeonChestDefinition> values) {
		values.forEach((id, value) -> validate(value, items, dungeons.definition(id).id()));
		definitions = Map.copyOf(values);
		definition = definition(dungeons.definition().id());
	}

	public DungeonChestDefinition definition(String dungeon) {
		var value = definitions.get(dungeon);
		if (value == null)
			throw new IllegalArgumentException("副本宝箱配置不存在");
		return value;
	}

	public static void validate(DungeonChestDefinition value, ItemCatalog items, String dungeon) {
		if (!dungeon.equals(value.dungeon()) || value.name() == null || value.name().isBlank())
			throw new IllegalArgumentException("宝箱副本引用或名称无效");
		weights(value.equipment(), DungeonChestEquipment::weight);
		weights(value.extraCounts(), DungeonChestCount::weight);
		weights(value.extras(), DungeonChestExtra::weight);
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

	public DungeonChestDefinition definition() {
		return definition;
	}

}
