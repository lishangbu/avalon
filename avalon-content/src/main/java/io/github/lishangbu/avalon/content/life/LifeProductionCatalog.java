package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.ContentChecks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

/** 工具与生产目录启动校验；工具档位和配方资格不依赖人物等级。 */
@Component
public class LifeProductionCatalog {

	private final Definition definition;

	private final List<Tool> tools;

	private final Map<String, Integer> sellPrices;

	private final Map<String, Tool> toolsById;

	private final Map<String, Recipe> recipesById;

	@Autowired
	public LifeProductionCatalog(ItemCatalog items, WorldCatalog world) {
		this(ContentLoader.read("/content/life-production.json", Definition.class), items, world);
	}

	public LifeProductionCatalog(Definition definition, ItemCatalog items, WorldCatalog world) {
		this.definition = validate(definition, items, world);
		tools = items.ofCategory(ItemCatalog.Category.TOOL)
			.stream()
			.map(i -> new Tool(i.id(), i.tool().kind(), i.tool().tier(), i.tool().breakDenominator()))
			.toList();
		sellPrices = items.allItems()
			.stream()
			.filter(i -> i.sellPrice() != null)
			.collect(Collectors.toUnmodifiableMap(ItemCatalog.Item::id, ItemCatalog.Item::sellPrice));
		toolsById = ContentChecks.index("/content/items.json", "items.id", tools, Tool::item);
		recipesById = ContentChecks.index("/content/life-production.json", "recipes.id", definition.recipes(),
				Recipe::id);
	}

	/** 纯业务校验入口；保留数值保护，内存定义也不能绕过有效性约束。 */
	public static Definition validate(Definition definition, ItemCatalog items, WorldCatalog world) {
		var ids = new HashSet<String>();
		for (var offer : definition.offers()) {
			ContentChecks.checkId("/content/life-production.json", "offers.item", ids, offer.item());
			if (!items.hasItem(offer.item()) || items.item(offer.item()).task() || offer.price() < 1
					|| offer.locations().isEmpty())
				throw ContentChecks.invalid("/content/life-production.json", offer.item(), "offer", offer, "生活商店商品无效");
			for (String location : offer.locations())
				world.referencedPlace("/content/life-production.json", offer.item(), "locations", location);
		}
		ids.clear();
		for (var recipe : definition.recipes()) {
			ContentChecks.checkId("/content/life-production.json", "recipes.id", ids, recipe.id());
			if (!LifeSkillRules.belongsTo(recipe.skill(), LifeSkillRules.Category.MANUFACTURING) || recipe.level() < 1
					|| recipe.level() > LifeSkillRules.MAX_LEVEL || recipe.points() < 1 || recipe.quantity() < 1
					|| recipe.quantity() > 99 || recipe.ingredients().isEmpty() || recipe.locations().isEmpty()
					|| !productionItem(items, recipe.product()))
				throw ContentChecks.invalid("/content/life-production.json", recipe.id(), "recipe", recipe,
						"配方字段或产物无效");
			recipe.ingredients().forEach((code, count) -> {
				if (!productionItem(items, code) || count < 1 || count > 999)
					throw ContentChecks.invalid("/content/life-production.json", recipe.id(), "ingredients." + code,
							count, "材料不存在、属于任务物品或数量越界");
			});
			for (String location : recipe.locations())
				world.referencedPlace("/content/life-production.json", recipe.id(), "locations", location);
		}
		return definition;
	}

	private static boolean productionItem(ItemCatalog items, String code) {
		return items.hasEquipment(code) || items.hasItem(code) && !items.item(code).task();
	}

	/** 普通产物按技能等级选工具；制造升档工具允许使用前一档。 */
	public int minimumToolTier(Recipe recipe) {
		var product = tool(recipe.product());
		return product == null ? LifeSkillRules.toolTier(recipe.level()) : Math.max(0, product.tier() - 1);
	}

	public List<Tool> tools() {
		return tools;
	}

	public Map<String, Integer> sellPrices() {
		return sellPrices;
	}

	public List<Offer> offers() {
		return definition.offers();
	}

	public record Offer(String item, int price, List<String> locations) {
		public Offer {
			locations = List.copyOf(locations);
		}
	}

	public Tool tool(String item) {
		return toolsById.get(item);
	}

	public List<Recipe> recipes() {
		return definition.recipes();
	}

	public Recipe recipe(String id) {
		return recipesById.get(id);
	}

	public record Tool(String item, String kind, int tier, int breakDenominator) {
	}

	public record Recipe(String id, String skill, int level, String product, int quantity, int points,
			Map<String, Integer> ingredients, List<String> locations) {
		public Recipe {
			ingredients = Map.copyOf(ingredients);
			locations = List.copyOf(locations);
		}
	}

	public record Definition(List<Recipe> recipes, List<Offer> offers) {
		public Definition {
			recipes = List.copyOf(recipes);
			offers = List.copyOf(offers);
		}
	}

}
