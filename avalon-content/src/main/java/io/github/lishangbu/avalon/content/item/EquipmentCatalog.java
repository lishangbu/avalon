package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 装备商店资格与售价；品质和职业限制读取物品目录；启动校验所有引用，未开放章节不提前出售。 */
@Component
public class EquipmentCatalog {

	private final Definition definition;

	private final Map<String, Metadata> metadata;

	public EquipmentCatalog(ItemCatalog items, WorldCatalog world, QuestCatalog quests) {
		definition = ContentLoader.read("/content/equipment.json", Definition.class);
		definition.shops().forEach(world::place);
		var prices = definition.offers().stream().collect(Collectors.toUnmodifiableMap(Offer::code, Offer::price));
		metadata = items.ofCategory(ItemCatalog.Category.EQUIPMENT)
			.stream()
			.collect(Collectors.toUnmodifiableMap(ItemCatalog.Item::id, i -> new Metadata(i.equipment().quality(),
					prices.getOrDefault(i.id(), 0), i.equipment().profession())));
		var seen = new HashSet<String>();
		for (var offer : definition.offers())
			if (!seen.add(offer.code()) || !metadata.containsKey(offer.code())
					|| metadata.get(offer.code()).price() <= 0 || quests.step(offer.requires()) == null
					|| offer.locations().isEmpty() || !definition.shops().containsAll(offer.locations()))
				throw new IllegalArgumentException("装备商品或解锁任务无效");
	}

	public Definition definition() {
		return definition;
	}

	public Metadata metadata(String code) {
		var value = metadata.get(code);
		if (value == null)
			throw new IllegalArgumentException("装备缺少品质价格配置：" + code);
		return value;
	}

	public record Metadata(String quality, int price, String profession) {
	}

	public record Offer(String code, String requires, List<String> locations, int price) {
		public Offer {
			locations = locations == null ? List.of() : List.copyOf(locations);
		}
	}

	public record Definition(List<String> shops, List<Offer> offers) {
		public Definition {
			shops = List.copyOf(shops);
			offers = List.copyOf(offers);
		}
	}

}
