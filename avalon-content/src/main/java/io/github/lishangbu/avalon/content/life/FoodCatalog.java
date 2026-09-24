package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.ContentChecks;

import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 料理恢复量与配方分离；料理仅供战斗外使用，不注册为战斗药品。 */
@Component
public class FoodCatalog {

	private final List<Food> foods;

	private final Map<String, Food> byItem;

	public FoodCatalog(ItemCatalog items) {
		foods = items.consumables(ItemCatalog.ConsumableType.FOOD, ItemCatalog.Scope.GENERAL)
			.stream()
			.map(i -> new Food(i.id(), i.recovery().health(), i.recovery().mana()))
			.toList();
		byItem = ContentChecks.index("/content/items.json", "items.id", foods, Food::item);
	}

	public Food food(String item) {
		return byItem.get(item);
	}

	public List<Food> foods() {
		return foods;
	}

	public record Food(String item, int health, int mana) {
	}

}
