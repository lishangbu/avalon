package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.ContentChecks;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.life.model.FoodItem;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 料理恢复量与配方分离；料理仅供战斗外使用，不注册为战斗药品。 */
@Component
public class FoodCatalog {

	private final List<FoodItem> foods;

	private final Map<String, FoodItem> byItem;

	public FoodCatalog(ItemCatalog items) {
		foods = items.consumables(ItemConsumableType.FOOD, ItemScope.GENERAL)
			.stream()
			.map(i -> new FoodItem(i.id(), i.recovery().health(), i.recovery().mana()))
			.toList();
		byItem = ContentChecks.index("/content/items.json", "items.id", foods, FoodItem::item);
	}

	public FoodItem food(String item) {
		return byItem.get(item);
	}

	public List<FoodItem> foods() {
		return foods;
	}

}
