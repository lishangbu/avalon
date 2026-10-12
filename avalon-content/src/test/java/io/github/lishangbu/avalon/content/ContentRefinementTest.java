package io.github.lishangbu.avalon.content;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRulesRecovery;
import io.github.lishangbu.avalon.content.item.model.ItemCategory;
import io.github.lishangbu.avalon.content.item.model.ItemConsumableType;
import io.github.lishangbu.avalon.content.item.model.ItemDefinition;
import io.github.lishangbu.avalon.content.item.model.ItemItem;
import io.github.lishangbu.avalon.content.item.model.ItemRecovery;
import io.github.lishangbu.avalon.content.item.model.ItemScope;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.model.LifeProductionDefinition;
import io.github.lishangbu.avalon.content.life.model.LifeProductionRecipe;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContentRefinementTest {

	@Test
	void academySuppliesAndFoodNeverEnterOrdinaryPotionOrResourceLists() {
		var items = new ItemCatalog(new WorldCatalog());
		var potions = new PotionRecoveryRules(items);
		for (String code : List.of("HEALTH_POTION_II", "MANA_POTION_II")) {
			assertFalse(items.hasItem(code));
			assertFalse(potions.usablePotions().contains(code));
			assertNull(potions.recovery(code));
			assertNotNull(potions.recovery(code, true));
			assertThrows(IllegalArgumentException.class, () -> items.ordinaryReference("test", "supply", "item", code));
		}
		assertEquals(200, potions.recovery("HEALTH_POTION_II", true).health());
		assertEquals(40, potions.recovery("MANA_POTION_II", true).mana());
		assertNull(potions.recovery("HEALTH_POTION", true));
		for (var food : items.consumables(ItemConsumableType.FOOD, ItemScope.GENERAL))
			assertNull(potions.recovery(food.id()));
		assertThrows(IllegalArgumentException.class,
				() -> new ItemCatalog(
						new ItemDefinition(List.of(items.item("HEALTH_POTION"), items.item("HEALTH_POTION"))),
						new WorldCatalog()));
	}

	@Test
	void potionInstancesUseTheirInjectedDefinitionsWithoutGlobalState() {
		var world = new WorldCatalog();
		var first = new ItemItem("CUSTOM", "药", "补给。", ItemCategory.CONSUMABLE, ItemConsumableType.POTION,
				ItemScope.GENERAL, false, null, new ItemRecovery(13, 7), null, null, null, null);
		var second = new ItemItem("CUSTOM", "药", "补给。", ItemCategory.CONSUMABLE, ItemConsumableType.POTION,
				ItemScope.GENERAL, false, null, new ItemRecovery(29, 0), null, null, null, null);
		var left = new PotionRecoveryRules(new ItemCatalog(new ItemDefinition(List.of(first)), world));
		var right = new PotionRecoveryRules(new ItemCatalog(new ItemDefinition(List.of(second)), world));
		assertEquals(List.of("CUSTOM"), left.usablePotions());
		assertEquals(new PotionRecoveryRulesRecovery(13, 7), left.recovery("CUSTOM"));
		assertEquals(new PotionRecoveryRulesRecovery(29, 0), right.recovery("CUSTOM"));
		assertNull(left.recovery("HEALTH_POTION"));
		assertThrows(UnsupportedOperationException.class, () -> left.usablePotions().clear());
	}

	@Test
	void mapAndQuestAcceptanceAgreeOnAlternativeAndClosingFacts() {
		var original = new WorldCatalog().definition();
		var places = original.places()
			.stream()
			.map(place -> place.id().equals("PET_CENTER") ? new WorldPlace(place.id(), place.name(),
					place.description(), place.x(), place.y(), place.exits(), "QUEST:BQ006", place.services(),
					"QUEST:BQ010", place.npcs(), List.of("QUEST:BQ008", "QUEST:BQ009")) : place)
			.toList();
		var world = new WorldCatalog(new WorldDefinition(original.start(), original.professions(), places,
				original.safeReturns(), original.npcs()));
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var step = quests.step("BQ007");
		for (var facts : List.of(Set.of("QUEST:BQ006", "QUEST:BQ008"), Set.of("QUEST:BQ006", "QUEST:BQ009"))) {
			assertTrue(world.accessible(world.place("PET_CENTER"), facts::contains));
			assertTrue(quests.acceptanceAvailable(step, facts::contains));
		}
		for (var facts : List.of(Set.of("QUEST:BQ008"), Set.of("QUEST:BQ006"),
				Set.of("QUEST:BQ006", "QUEST:BQ008", "QUEST:BQ010"))) {
			assertFalse(world.accessible(world.place("PET_CENTER"), facts::contains));
			assertFalse(quests.acceptanceAvailable(step, facts::contains));
		}
	}

	@Test
	void itemEffectsDrivePotionSelectionRecoveryAndDescriptions() {
		var items = new ItemCatalog(new WorldCatalog());
		var potions = new PotionRecoveryRules(items);
		assertEquals(10, potions.usablePotions().size());
		for (String code : potions.usablePotions()) {
			var item = items.item(code);
			var effect = potions.recovery(code);
			assertEquals(item.recovery().health(), effect.health());
			assertEquals(item.recovery().mana(), effect.mana());
		}
		assertEquals(120, potions.recovery("HEALTH_POTION").health());
		assertEquals(240, potions.recovery("LIFE_MANA_POTION_4").mana());
		var custom = new ItemItem("CUSTOM", "药", "补给。", ItemCategory.CONSUMABLE, ItemConsumableType.POTION,
				ItemScope.GENERAL, false, null, new ItemRecovery(13, 7), null, null, null, null);
		assertEquals("补给。", custom.description());
		assertNull(potions.recovery("NOT_A_POTION"));
	}

	@Test
	void memoryDefinitionsCannotBypassRecoveryValidation() {
		var world = new WorldCatalog();
		for (var recovery : List.of(new ItemRecovery(-1, 0), new ItemRecovery(0, 0))) {
			var item = new ItemItem("BAD", "药", "补给", ItemCategory.CONSUMABLE, ItemConsumableType.POTION,
					ItemScope.GENERAL, false, null, recovery, null, null, null, null);
			var error = assertThrows(IllegalArgumentException.class,
					() -> new ItemCatalog(new ItemDefinition(List.of(item)), world));
			assertTrue(error.getMessage().contains("/content/items.json [BAD] recovery="));
		}
	}

	@Test
	void manufacturingErrorsIdentifyRecipeAndMissingReference() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var recipe = new LifeProductionRecipe("BROKEN", "ALCHEMY", 1, "HEALTH_POTION", 1, 1, Map.of("MISSING", 1),
				List.of("PET_CENTER"));
		var error = assertThrows(IllegalArgumentException.class,
				() -> new LifeProductionCatalog(new LifeProductionDefinition(List.of(recipe), List.of()), items,
						world));
		assertTrue(error.getMessage().contains("/content/life-production.json [BROKEN] ingredients.MISSING=1"));
	}

	@Test
	void indexesRejectDuplicatesAndCannotChangeOrderingOrContents() {
		var index = ContentChecks.index("sample.json", "id", List.of("B", "A"), value -> value);
		assertEquals(List.of("B", "A"), List.copyOf(index.keySet()));
		assertThrows(UnsupportedOperationException.class, () -> index.put("C", "C"));
		var error = assertThrows(IllegalArgumentException.class,
				() -> ContentChecks.index("sample.json", "id", List.of("A", "A"), value -> value));
		assertTrue(error.getMessage().contains("sample.json [A] id=A"));
	}

}
