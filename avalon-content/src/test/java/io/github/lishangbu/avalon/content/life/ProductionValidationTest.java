package io.github.lishangbu.avalon.content.life;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.item.model.ItemTool;
import io.github.lishangbu.avalon.content.life.model.LifeProductionDefinition;
import io.github.lishangbu.avalon.content.life.model.LifeProductionOffer;
import io.github.lishangbu.avalon.content.life.model.LifeProductionRecipe;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductionValidationTest {

	@Test
	void missingFacilityIdentifiesTheReferringRecipe() {
		var recipe = new LifeProductionRecipe("BAD_FACILITY", "ALCHEMY", 1, "NORMAL", 1, 1, Map.of("NORMAL", 1),
				List.of("MISSING"));
		var error = assertThrows(IllegalArgumentException.class, () -> catalog(List.of(), List.of(recipe), List.of()));
		assertTrue(error.getMessage().contains("/content/life-production.json [BAD_FACILITY] locations=MISSING"));
	}

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(new ItemDefinition(List.of(
			new ItemItem("NORMAL", "普通物品", "材料", ItemCategory.MATERIAL, null, ItemScope.GENERAL, false, null, null,
					null, null, null, null),
			new ItemItem("TOOL", "工具", "工具", ItemCategory.MATERIAL, null, ItemScope.GENERAL, false, null, null, null,
					null, null, null),
			new ItemItem("QUEST", "任务物品", "任务", ItemCategory.MATERIAL, null, ItemScope.GENERAL, true, null, null, null,
					null, null, null))),
			world);

	@Test
	void rejectsTaskItemsAsIngredientsProductsToolsAndShopOffers() {
		for (var recipe : List.of(recipe("QUEST", "NORMAL"), recipe("NORMAL", "QUEST")))
			assertThrows(IllegalArgumentException.class, () -> catalog(List.of(), List.of(recipe), List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> catalog(List.of(new LifeProductionTool("QUEST", "AXE", 0, 100)), List.of(), List.of()));
		assertThrows(IllegalArgumentException.class, () -> catalog(List.of(), List.of(),
				List.of(new LifeProductionOffer("QUEST", 1, List.of(world.definition().start())))));
		assertDoesNotThrow(() -> catalog(List.of(), List.of(recipe("NORMAL", "NORMAL")), List.of()));
	}

	@Test
	void rejectsUnknownToolKindsAndUsesPreviousTierForToolManufacture() {
		for (String kind : List.of("AXEE", "", "MINING"))
			assertThrows(IllegalArgumentException.class,
					() -> catalog(List.of(new LifeProductionTool("TOOL", kind, 0, 100)), List.of(), List.of()));
		var recipe = recipe("TOOL", "NORMAL");
		for (int tier = 0; tier <= 4; tier++) {
			var catalog = catalog(List.of(new LifeProductionTool("TOOL", "AXE", tier, 100)), List.of(recipe),
					List.of());
			assertEquals(Math.max(0, tier - 1), catalog.minimumToolTier(recipe));
		}
	}

	@Test
	void ordinaryProductsUseTwoSkillLevelsPerTier() {
		for (int level = 1; level <= 10; level++) {
			var recipe = new LifeProductionRecipe("R", "ALCHEMY", level, "NORMAL", 1, 1, Map.of("NORMAL", 1),
					List.of(world.definition().start()));
			assertEquals((level - 1) / 2, catalog(List.of(), List.of(recipe), List.of()).minimumToolTier(recipe));
		}
		assertTrue(LifeSkillRules.belongsTo("MINING", LifeSkillRulesCategory.GATHERING));
		assertFalse(LifeSkillRules.belongsTo("MINING", LifeSkillRulesCategory.MANUFACTURING));
		assertFalse(LifeSkillRules.belongsTo("UNKNOWN", LifeSkillRulesCategory.GATHERING));
	}

	private LifeProductionRecipe recipe(String product, String ingredient) {
		return new LifeProductionRecipe("R", "ALCHEMY", 1, product, 1, 1, Map.of(ingredient, 1),
				List.of(world.definition().start()));
	}

	private LifeProductionCatalog catalog(List<LifeProductionTool> tools, List<LifeProductionRecipe> recipes,
			List<LifeProductionOffer> offers) {
		var definitions = new ArrayList<>(items.allItems());
		for (var tool : tools) {
			var original = items.item(tool.item());
			definitions.removeIf(i -> i.id().equals(tool.item()));
			definitions.add(new ItemItem(original.id(), original.name(), original.description(), ItemCategory.TOOL,
					null, ItemScope.GENERAL, original.task(), null, null,
					new ItemTool(tool.kind(), tool.tier(), tool.breakDenominator()), null, null, null));
		}
		var catalog = new ItemCatalog(new ItemDefinition(definitions), world);
		return new LifeProductionCatalog(new LifeProductionDefinition(recipes, offers), catalog, world);
	}

}
