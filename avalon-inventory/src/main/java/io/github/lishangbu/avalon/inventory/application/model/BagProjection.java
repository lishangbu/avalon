package io.github.lishangbu.avalon.inventory.application.model;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 按同一批库存读取生成格数和展示，不将不同绑定状态或装备实例合并。查询不会改动来源记录。 */
@Component
@RequiredArgsConstructor
public class BagProjection {

	private final ItemCatalog catalog;

	public BagView from(List<InventoryItemView> inventory) {
		var equipment = new ArrayList<InventoryItemView>();
		var tasks = new ArrayList<InventoryItemView>();
		Map<Key, Long> quantities = new LinkedHashMap<>();
		Map<Key, String> names = new LinkedHashMap<>();
		var entries = new ArrayList<BagCapacityRules.Entry>();
		for (var item : inventory) {
			boolean task = item.slot() == null && catalog.item(item.code()).task();
			entries
				.add(new BagCapacityRules.Entry(item.code(), item.bound(), item.quantity(), item.slot() != null, task));
			if (item.slot() != null)
				equipment.add(item);
			else if (task)
				tasks.add(item);
			else {
				var key = new Key(item.code(), item.bound());
				quantities.merge(key, (long) item.quantity(), Math::addExact);
				names.putIfAbsent(key, item.name());
			}
		}
		var stacks = new ArrayList<BagView.Stack>();
		quantities.forEach((key, quantity) -> {
			for (int index = 0; quantity > 0; index++) {
				int count = (int) Math.min(quantity, BagCapacityRules.STACK_LIMIT);
				stacks.add(new BagView.Stack(key.code() + ":" + key.bound() + ":" + index, key.code(), names.get(key),
						category(key.code()), key.bound(), count));
				quantity -= count;
			}
		});
		return new BagView(BagCapacityRules.slots(entries), BagCapacityRules.CAPACITY, List.copyOf(equipment),
				List.copyOf(stacks), List.copyOf(tasks));
	}

	private String category(String code) {
		return catalog.item(code).categoryLabel();
	}

	private record Key(String code, boolean bound) {
	}

}
