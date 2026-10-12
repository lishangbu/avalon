package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.item.model.EquipmentDefinition;
import io.github.lishangbu.avalon.content.item.model.EquipmentMetadata;
import io.github.lishangbu.avalon.content.item.model.EquipmentOffer;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** 装备商店资格与售价；品质和职业限制读取物品目录；启动校验所有引用，未开放章节不提前出售。 */
@Component
public class EquipmentCatalog {

	private final EquipmentDefinition definition;

	private final Map<String, EquipmentMetadata> metadata;

	public EquipmentCatalog(ItemCatalog items, WorldCatalog world, QuestCatalog quests) {
		definition = ContentLoader.read("/content/equipment.json", EquipmentDefinition.class);
		definition.shops().forEach(world::place);
		var prices = definition.offers()
			.stream()
			.collect(Collectors.toUnmodifiableMap(EquipmentOffer::code, EquipmentOffer::price));
		metadata = items.ofCategory(ItemCategory.EQUIPMENT)
			.stream()
			.collect(Collectors.toUnmodifiableMap(ItemItem::id, i -> new EquipmentMetadata(i.equipment().quality(),
					prices.getOrDefault(i.id(), 0), i.equipment().profession())));
		var seen = new HashSet<String>();
		for (var offer : definition.offers())
			if (!seen.add(offer.code()) || !metadata.containsKey(offer.code())
					|| metadata.get(offer.code()).price() <= 0 || quests.step(offer.requires()) == null
					|| offer.locations().isEmpty() || !definition.shops().containsAll(offer.locations()))
				throw new IllegalArgumentException("装备商品或解锁任务无效");
	}

	public EquipmentDefinition definition() {
		return definition;
	}

	public EquipmentMetadata metadata(String code) {
		var value = metadata.get(code);
		if (value == null)
			throw new IllegalArgumentException("装备缺少品质价格配置：" + code);
		return value;
	}

}
