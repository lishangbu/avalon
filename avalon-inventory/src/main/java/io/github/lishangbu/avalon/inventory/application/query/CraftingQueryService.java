package io.github.lishangbu.avalon.inventory.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.CraftingCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.application.model.CraftingView;
import io.github.lishangbu.avalon.inventory.domain.BagCapacityRules;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryRepository;
import org.springframework.stereotype.Component;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/** 合成预览只读库存，格数是当前状态；命令会在角色锁内重新检查消费后的净空间。 */
@Component
@RequiredArgsConstructor
public class CraftingQueryService {

	private final CharacterQueryService characters;

	private final InventoryRepository inventory;

	private final CraftingCatalog crafting;

	private final ItemCatalog items;

	@Transactional
	public CraftingView preview(long owner) {
		var owned = inventory.available(characters.requireId(owner));
		int used = BagCapacityRules.slots(owned.stream()
			.map(item -> new BagCapacityRules.Entry(item.getCode(), item.isBound(), item.getQuantity(),
					item.getSlot() != null, item.getSlot() == null && items.item(item.getCode()).task()))
			.toList());
		var recipes = crafting.recipes().stream().map(recipe -> {
			var fragments = owned.stream()
				.filter(item -> item.getCode().equals(recipe.fragment()))
				.map(item -> new CraftingView.Fragment(item.getId().toString(), item.getName(), item.getQuantity(),
						item.isBound()))
				.toList();
			return new CraftingView.Recipe(recipe.id(), items.item(recipe.product()).name(), recipe.quantity(),
					recipe.gold(), fragments);
		}).filter(recipe -> !recipe.fragments().isEmpty()).toList();
		return new CraftingView(used, BagCapacityRules.CAPACITY, recipes);
	}

}
