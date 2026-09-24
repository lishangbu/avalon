package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.EvolutionCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.LearningCatalog;
import io.github.lishangbu.avalon.content.creature.CollectionCatalog;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 验证路线输入与未取整成长边界，不将目录测试当作高等级实战或进化事务验收。 */
class EvolutionCatalogTest extends SpringIntegrationTest {

	@Autowired
	EvolutionCatalog evolution;

	@Autowired
	CreatureCatalog creatures;

	@Autowired
	ItemCatalog items;

	@Autowired
	BattleCatalog battle;

	@Autowired
	BattleRules rules;

	@Autowired
	LearningCatalog learning;

	@Autowired
	CollectionCatalog collection;

	@Test
	void formsKeepOriginalLearningPoolsButCannotBeGrantedOrCaptured() {
		assertSame(creatures.evolutionDefinition(), evolution.definition());
		for (var route : evolution.definition().routes()) {
			assertTrue(creatures.contains(route.target()));
			assertFalse(creatures.obtainableDirectly(route.target()));
			assertEquals(creatures.pet(route.source()).element(), creatures.pet(route.target()).element());
			assertEquals(creatures.pet(route.source()).skills(), creatures.pet(route.target()).skills());
			var pool = learning.pool(route.target()).stream().map(LearningCatalog.Book::skill).toList();
			assertTrue(
					pool.containsAll(learning.pool(route.source()).stream().map(LearningCatalog.Book::skill).toList()));
			if (route.skill() != null) {
				assertTrue(pool.contains(route.skill()));
				assertFalse(
						learning.pool(route.source()).stream().anyMatch(book -> book.skill().equals(route.skill())));
			}
			assertEquals("SEAL_II", collection.reward(route.target()).item());
			assertFalse(collection.definition().milestoneSpecies().contains(route.target()));
		}
	}

	@Test
	void combatCalculatesEachFormOnceBeforeApplyingOtherBonuses() {
		// 使用既有 15 级基础曲线隔离验证公式，不声称该级别能够实际进化。
		assertEquals(new Stats(786, 150, 57, 130, 106, 107, 77), rules.stats("GALE_FALCON", 15, false, Map.of()));
		assertEquals(new Stats(864, 170, 57, 175, 87, 107, 55),
				rules.stats("FLAME_SPIRIT_RABBIT", 15, false, Map.of()));
		var unmodified = rules.stats("GALE_FALCON", 15, false, Map.of());
		assertEquals(unmodified, rules.stats("GALE_FALCON", 15, false, Map.of()), "重复预览不能复利增加属性");
		assertEquals(unmodified,
				rules.stats("GALE_FALCON", 15, false, Map.of("health", 10, "mana", 3, "magicAttack", 10)),
				"幻兽不能继承人物装备属性");
		assertEquals(new Stats(504, 120, 57, 85, 76, 85, 65), rules.stats("GALE_SPARROW", 15, false, Map.of()));
	}

	@Test
	void registeringEvolutionDoesNotGrantSkillsOrOpenWildEncounters() {
		var route = evolution.route("GALE_SPARROW");
		assertEquals("STREAM_CLAW_CAT", evolution.route("CLAW_CAT").target());
		assertFalse(creatures.pet(route.source()).skills().contains(route.skill()));
		assertTrue(battle.definition()
			.encounters()
			.stream()
			.noneMatch(encounter -> encounter.species().containsKey(route.target())));
	}

	@Test
	void malformedRecipeAndDuplicateRoutesFailBeforeOpeningFacilities() {
		var route = evolution.route("FLAME_RABBIT");
		assertInvalid(List.of(route, route));
		assertInvalid(List.of(new EvolutionCatalog.Route(route.source(), route.target(), route.name(), route.level(),
				Map.of("MISSING_MATERIAL", 1), route.skill())));
		assertInvalid(List.of(new EvolutionCatalog.Route(route.source(), route.target(), route.name(), route.level(),
				Map.of("PET_GUIDE", 1), route.skill())));
		assertInvalid(List.of(new EvolutionCatalog.Route(route.source(), "MISSING_FORM", route.name(), route.level(),
				route.materials(), route.skill())));
		assertInvalid(List.of(new EvolutionCatalog.Route(route.source(), route.source(), route.name(), route.level(),
				route.materials(), route.skill())));
	}

	private void assertInvalid(List<EvolutionCatalog.Route> routes) {
		assertThrows(IllegalArgumentException.class,
				() -> EvolutionCatalog.validate(new EvolutionCatalog.Definition(16, routes), creatures, items, battle));
	}

}
