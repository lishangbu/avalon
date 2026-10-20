package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;

import java.util.HashSet;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 每档生产从已知木工具和采集原料出发，检查工具与辅料能逐步制造，防止循环依赖锁死。 */
class LifeProductionCatalogTest {

	@Test
	void recipesUseTheirProfessionFacilityAndEveryFacilityHasAReturnRoad() {
		var facilities = Map.of("ARTISAN", "GNOME_FORGE", "LEATHERWORK", "GNOME_LEATHERWORKS", "ALCHEMY",
				"GNOME_APOTHECARY", "COOKING", "GNOME_KITCHEN");
		for (var recipe : catalog.recipes()) {
			assertTrue(recipe.locations().contains(facilities.get(recipe.skill())), recipe.id());
			for (String facility : recipe.locations()) {
				assertFalse(world.place(facility).exits().isEmpty(), recipe.id());
				assertTrue(world.place(facility)
					.exits()
					.stream()
					.anyMatch(parent -> world.place(parent).exits().contains(facility)), recipe.id());
			}
		}
		for (String facility : facilities.values()) {
			assertTrue(world.place("GNOME_CITY").exits().contains(facility));
			assertTrue(world.place(facility).exits().contains("GNOME_CITY"));
		}
	}

	private final WorldCatalog world = new WorldCatalog();

	private final LifeProductionCatalog catalog = new LifeProductionCatalog(new ItemCatalog(world), world);

	@Test
	void normalRecipesRequireTheirOwnTierAndOnlyToolUpgradesUsePreviousTier() {
		for (var recipe : catalog.recipes()) {
			var tool = catalog.tool(recipe.product());
			assertEquals(tool == null ? (recipe.level() - 1) / 2 : Math.max(0, tool.tier() - 1),
					catalog.minimumToolTier(recipe), recipe.id());
		}
	}

	@Test
	void everyCraftedProductCanBeSoldAndShopPurchasesCannotCreateGold() {
		for (var recipe : catalog.recipes())
			assertTrue(catalog.sellPrices().getOrDefault(recipe.product(), 0) > 0, recipe.product());
		for (var offer : catalog.offers())
			assertTrue(catalog.sellPrices().getOrDefault(offer.item(), 0) < offer.price(), offer.item());
	}

	@Test
	void allToolsCanBeProducedWithoutAlreadyOwningTheTargetTier() {
		var available = new HashSet<String>();
		var gathering = new GatheringCatalog(new ItemCatalog(world), world);
		var gatheringTools = Map.of("MINING", "PICKAXE", "LOGGING", "AXE", "HERBALISM", "PINCERS", "HUNTING", "TRAP");
		catalog.tools().stream().filter(tool -> tool.tier() == 0).forEach(tool -> available.add(tool.item()));
		var makerTools = Map.of("ARTISAN", "HAMMER", "LEATHERWORK", "NEEDLE", "ALCHEMY", "ROD", "COOKING", "POT");
		boolean changed;
		do {
			changed = false;
			for (var node : gathering.nodes()) {
				boolean canGather = catalog.tools()
					.stream()
					.anyMatch(tool -> available.contains(tool.item())
							&& tool.kind().equals(gatheringTools.get(node.skill()))
							&& tool.tier() >= (node.level() - 1) / 2);
				if (canGather)
					changed |= available.add(node.product());
			}
			for (var recipe : catalog.recipes()) {
				boolean toolReady = catalog.tools()
					.stream()
					.anyMatch(tool -> available.contains(tool.item())
							&& tool.kind().equals(makerTools.get(recipe.skill()))
							&& tool.tier() >= catalog.minimumToolTier(recipe));
				if (toolReady && available.containsAll(recipe.ingredients().keySet()))
					changed |= available.add(recipe.product());
			}
		}
		while (changed);
		for (var tool : catalog.tools())
			assertTrue(available.contains(tool.item()), "无法制造：" + tool.item());
		assertEquals(50, catalog.tools().size());
	}

	@Test
	void earlyToolsAvoidProcessedDependenciesAndIronIntroducesCooperation() {
		for (var tool : catalog.tools()) {
			var recipe = catalog.recipe(tool.item());
			assertNotNull(recipe);
			assertTrue(catalog.minimumToolTier(recipe) <= Math.max(0, tool.tier() - 1));
			if (tool.tier() < 2)
				assertTrue(recipe.ingredients()
					.keySet()
					.stream()
					.noneMatch(code -> code.startsWith("LIFE_PART_") || code.startsWith("LIFE_WRAP_")
							|| code.startsWith("LIFE_REAGENT_")));
			else {
				assertTrue(recipe.ingredients().containsKey("LIFE_PART_" + (tool.tier() - 1)));
				assertTrue(recipe.ingredients().containsKey("LIFE_WRAP_" + (tool.tier() - 1)));
				assertTrue(recipe.ingredients().containsKey("LIFE_REAGENT_" + (tool.tier() - 1)));
			}
		}
	}

}
