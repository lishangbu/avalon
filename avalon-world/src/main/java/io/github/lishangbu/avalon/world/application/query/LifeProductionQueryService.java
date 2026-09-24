package io.github.lishangbu.avalon.world.application.query;

import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 配方和材料来自正式目录，页面不推导资格；地点名称由世界目录提供。 */
@Component
@RequiredArgsConstructor
public class LifeProductionQueryService {

	private final CharacterQueryService characters;

	private final InventoryQueryService inventory;

	private final LifeSkillRepository skills;

	private final LifeProductionCatalog catalog;

	private final ItemCatalog items;

	private final WorldCatalog world;

	public View view(long owner) {
		long character = characters.requireId(owner);
		var context = characters.find(owner);
		var learned = skills.owned(character);
		var owned = inventory.list(owner);
		var recipes = catalog.recipes()
			.stream()
			.filter(recipe -> learned.containsKey(Skill.valueOf(recipe.skill())))
			.map(recipe -> new RecipeView(recipe.id(), recipe.skill(), recipe.level(), catalog.minimumToolTier(recipe),
					recipe.product(),
					items.hasEquipment(recipe.product()) ? items.equipment(recipe.product()).name()
							: items.item(recipe.product()).name(),
					recipe.quantity(), recipe.points(),
					LifeSkillRules.level(learned.get(Skill.valueOf(recipe.skill()))),
					proficiencyGain(learned.get(Skill.valueOf(recipe.skill())), recipe),
					recipe.ingredients()
						.entrySet()
						.stream()
						.map(entry -> new Ingredient(entry.getKey(),
								items.hasEquipment(entry.getKey()) ? items.equipment(entry.getKey()).name()
										: items.item(entry.getKey()).name(),
								entry.getValue()))
						.toList(),
					recipe.locations().stream().map(location -> world.place(location).name()).toList(),
					recipe.locations().contains(context.location())
							&& LifeSkillRules.level(learned.get(Skill.valueOf(recipe.skill()))) >= recipe.level()))
			.toList();
		var offers = catalog.offers()
			.stream()
			.map(offer -> new OfferView(offer.item(), items.item(offer.item()).name(), offer.price(),
					offer.locations().stream().map(location -> world.place(location).name()).toList(),
					offer.locations().contains(context.location())))
			.toList();
		return new View(recipes, owned, catalog.tools(), offers, catalog.sellPrices(),
				Set.of("SUPPLY_SHOP", "CITY_SHOP").contains(context.location()));
	}

	/** 下一件成功产出的实际收益；尚未解锁的配方不提供可执行收益。 */
	private Double proficiencyGain(long current, LifeProductionCatalog.Recipe recipe) {
		if (LifeSkillRules.level(current) < recipe.level())
			return null;
		return (LifeSkillRules.gain(current, recipe.level(), recipe.points()) - current) / 2.0;
	}

	public record View(List<RecipeView> recipes, List<InventoryItemView> inventory,
			List<LifeProductionCatalog.Tool> tools, List<OfferView> offers, Map<String, Integer> sellPrices,
			boolean canSell) {
	}

	public record OfferView(String item, String name, int price, List<String> locations, boolean available) {
	}

	public record RecipeView(String id, String skill, int level, int minimumToolTier, String product, String name,
			int quantity, int points, int skillLevel, Double proficiencyGain, List<Ingredient> ingredients,
			List<String> locations, boolean available) {
	}

	public record Ingredient(String code, String name, int quantity) {
	}

}
