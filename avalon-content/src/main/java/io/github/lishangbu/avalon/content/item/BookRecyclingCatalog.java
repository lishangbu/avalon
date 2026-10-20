package io.github.lishangbu.avalon.content.item;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.item.model.BookRecyclingDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 回收目录只接受有商店定价的完整书；价格在启动时计算，非商店书不推测估价。 */
@Component
public class BookRecyclingCatalog {

	private final BookRecyclingDefinition definition;

	private final Map<String, Integer> prices;

	public BookRecyclingCatalog(BattleCatalog battle, LearningCatalog learning, WorldCatalog world) {
		definition = ContentLoader.read("/content/items/book-recycling.json", BookRecyclingDefinition.class);
		if (definition.percent() < 1 || definition.percent() >= 100 || definition.locations().isEmpty())
			throw new IllegalArgumentException("技能书回收配置无效");
		definition.locations().forEach(world::place);
		var values = new LinkedHashMap<String, Integer>();
		for (var product : battle.definition().products()) {
			if (learning.book(product.code()) == null)
				continue;
			int price = Math.toIntExact((long) product.price() * definition.percent() / 100);
			if (price <= 0 || values.putIfAbsent(product.code(), price) != null)
				throw new IllegalArgumentException("技能书回收价格无效或重复：" + product.code());
		}
		prices = Map.copyOf(values);
	}

	public Map<String, Integer> prices() {
		return prices;
	}

	public boolean available(String location) {
		return definition.locations().contains(location);
	}

}
