package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.DungeonRewardRules;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonRewardCatalog;
import io.github.lishangbu.avalon.content.battle.model.DungeonDefinition;
import io.github.lishangbu.avalon.content.battle.model.DungeonRewardDefinition;
import io.github.lishangbu.avalon.content.battle.model.DungeonRewardDrop;
import io.github.lishangbu.avalon.content.battle.model.DungeonStage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 多副本奖励独立计算，不借用默认副本等级、通关经验或掉落池。 */
@Tag("rules")
class DungeonRewardRoutingTest {

	@Test
	void sealedTrialOwnsTwoStagesAndDropsCompleteLevel45Equipment() {
		var fixture = new RuleTestFixture();
		var trial = fixture.dungeon.definition("E_SEALED_TRIAL");
		assertEquals(1, trial.players());
		assertEquals(45, trial.level());
		assertEquals("J027", trial.requires());
		assertEquals("E_CHALLENGE_ENTRANCE", trial.entrance());
		assertEquals(3, trial.dailyStarts());
		assertEquals(List.of("E_TRIAL_SENTINEL", "E_TRIAL_MASTER"),
				trial.stages().stream().map(DungeonStage::id).toList());
		var state = fixture.progress.create(trial.id(), "a", List.of("a"));
		for (int index = 0; index < 2; index++) {
			assertEquals(trial, fixture.dungeon.owningStage(trial.stages().get(index).id()));
			fixture.progress.ready(state, "a");
			assertEquals(trial.stages().get(index).id(), fixture.progress.nextStage(state));
			fixture.progress.begin(state, 500 + index);
			assertTrue(fixture.progress.resolve(state, 500 + index, "WON"));
		}
		var rewards = new DungeonRewardCatalog(fixture.dungeon).definition(trial.id());
		assertEquals(1500, rewards.completionExperience());
		assertEquals(9000, rewards.firstCompletionExperience());
		assertEquals(List.of("E_TRIAL_SENTINEL"), rewards.supplyStages());
		var chest = fixture.chests.definition(trial.id());
		assertEquals(18, chest.equipment().size());
		assertEquals(100, chest.equipment().stream().mapToInt(gear -> gear.weight()).sum());
		assertTrue(chest.equipment().stream().allMatch(gear -> fixture.items.equipment(gear.code()).level() == 45));
		assertTrue(chest.equipment().stream().anyMatch(gear -> gear.code().equals("E_FORGED_BODY")));
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "U", "S", "F", "J" })
	void middleSealStagesAndLootBelongToTheirOwnDungeon(String prefix) {
		var fixture = new RuleTestFixture();
		var dungeon = fixture.dungeon.definition(prefix.equals("J") ? "J_ABYSS" : prefix + "_SEAL");
		assertEquals(1, dungeon.players());
		assertEquals(prefix.equals("J") ? "J015" : prefix + "020", dungeon.requires());
		assertEquals(prefix.equals("J") ? "J_ABYSS_ENTRANCE" : prefix + "_SEAL_FORECOURT", dungeon.entrance());
		assertEquals(prefix.equals("J") ? 5 : 3, dungeon.stages().size());
		if (prefix.equals("J"))
			assertEquals(3, dungeon.stages().stream().filter(stage -> stage.wildSpecies().isEmpty()).count());
		var state = fixture.progress.create(dungeon.id(), "a", List.of("a"));
		for (int index = 0; index < dungeon.stages().size(); index++) {
			var stage = dungeon.stages().get(index);
			assertEquals(dungeon, fixture.dungeon.owningStage(stage.id()));
			fixture.progress.ready(state, "a");
			assertEquals(stage.id(), fixture.progress.nextStage(state));
			fixture.progress.begin(state, 200 + index);
			assertTrue(fixture.progress.resolve(state, 200 + index, "WON"));
		}
		var rewards = new DungeonRewardCatalog(fixture.dungeon).definition(dungeon.id());
		assertEquals(prefix.equals("J") ? 8000 : prefix.equals("F") ? 6000 : 4000, rewards.firstCompletionExperience());
		assertEquals(dungeon.stages().subList(0, dungeon.stages().size() - 1).stream().map(DungeonStage::id).toList(),
				rewards.supplyStages());
		var loot = fixture.chests.definition(dungeon.id());
		assertEquals(9, loot.equipment().size());
		assertTrue(loot.equipment()
			.stream()
			.allMatch(gear -> gear.code()
				.startsWith(prefix.equals("J") ? "CRAFT_8_" : prefix.equals("F") ? "CRAFT_7_" : "CRAFT_5_")));
	}

	/** 正式资源通过同一启动加载与规则链，诺姆三首领属于一个单人副本。 */
	@Test
	void productionNomDungeonLoadsThreeStagesAndOwnRewardPools() {
		var fixture = new RuleTestFixture();
		var nom = fixture.dungeon.definition("NOM_SEAL");
		assertEquals(1, nom.players());
		assertEquals(20, nom.level());
		assertEquals("N020", nom.requires());
		assertEquals("NOM_SEAL_FORECOURT", nom.entrance());
		assertEquals(List.of("NOM_SEAL_OUTER", "NOM_SEAL_CREATION", "NOM_SEAL_CORE"),
				nom.stages().stream().map(DungeonStage::id).toList());
		var state = fixture.progress.create(nom.id(), "a", List.of("a"));
		for (int index = 0; index < 3; index++) {
			fixture.progress.ready(state, "a");
			assertEquals(nom.stages().get(index).id(), fixture.progress.nextStage(state));
			fixture.progress.begin(state, 100 + index);
			assertTrue(fixture.progress.resolve(state, 100 + index, "WON"));
		}
		var rewards = new DungeonRewardCatalog(fixture.dungeon).definition(nom.id());
		assertEquals(500, rewards.completionExperience());
		assertEquals(3000, rewards.firstCompletionExperience());
		var chest = fixture.chests.definition(nom.id());
		assertEquals(100, chest.equipment().stream().mapToInt(gear -> gear.weight()).sum());
		assertTrue(chest.equipment().stream().allMatch(gear -> fixture.items.equipment(gear.code()).level() == 20));
	}

	@Test
	void stageOwnershipControlsExperienceDayAndSupplyPool() {
		var fixture = new RuleTestFixture();
		var source = fixture.dungeon.definition();
		var stage = new DungeonStage("SOLO_CORE", "封印核心", List.of(), 1, 3000, 90, 90, 30, 0, 0, 100, 10,
				source.stages().get(1).actions());
		var solo = new DungeonDefinition("SOLO", "封印室", 20, "N020", "NOM_SEAL_FORECOURT", 1, 3, 0, "UTC", 90, 3, 60,
				180, 300, List.of(0), List.of("HEALTH_POTION"), List.of(stage));
		var dungeons = new DungeonCatalog(List.of(source, solo), fixture.creatures, fixture.quests, fixture.items,
				fixture.world);
		var original = new DungeonRewardCatalog(fixture.dungeon).definition();
		var ownRewards = new DungeonRewardDefinition(700, 900, 0, Map.of(), List.of(stage.id()),
				List.of(new DungeonRewardDrop("HEALTH_POTION", 100)));
		var catalog = new DungeonRewardCatalog(dungeons, Map.of(source.id(), original, solo.id(), ownRewards));
		var rules = new DungeonRewardRules(dungeons, catalog);
		assertEquals(100, rules.experience(stage, 20, false));
		assertEquals(800, rules.experience(stage, 20, true));
		assertEquals(820, rules.experience(stage, 10, true));
		assertEquals("HEALTH_POTION", rules.supply("SOLO", new Random(1)));
		var defaultChest = fixture.chests.definition();
		var ownChest = new io.github.lishangbu.avalon.content.battle.model.DungeonChestDefinition("SOLO", "封印室宝箱",
				List.of(new io.github.lishangbu.avalon.content.battle.model.DungeonChestEquipment("NOM_RUIN_BOOTS", 100,
						12)),
				List.of(new io.github.lishangbu.avalon.content.battle.model.DungeonChestCount(1, 100)),
				List.of(new io.github.lishangbu.avalon.content.battle.model.DungeonChestExtra("GOLD", 19, 100, false)));
		var chests = new io.github.lishangbu.avalon.content.battle.DungeonChestCatalog(fixture.items, dungeons,
				Map.of(source.id(), defaultChest, solo.id(), ownChest));
		var chestRules = new io.github.lishangbu.avalon.inventory.domain.DungeonChestRules(chests);
		var loot = chestRules.roll("SOLO", new Random(1));
		assertEquals("NOM_RUIN_BOOTS", loot.equipment());
		assertEquals(19, loot.gold());
		assertEquals(defaultChest, chests.definition());
		assertThrows(IllegalArgumentException.class, () -> chestRules.roll("UNKNOWN", new Random(1)));
		var instant = Instant.parse("2026-10-19T00:00:00Z");
		assertEquals(LocalDate.of(2026, 10, 19), rules.day("SOLO", instant));
		assertThrows(IllegalArgumentException.class, () -> catalog.definition("UNKNOWN"));
		var foreign = new DungeonRewardDefinition(0, 0, 0, Map.of(), List.of(source.stages().getFirst().id()),
				ownRewards.supplyPool());
		assertThrows(IllegalArgumentException.class,
				() -> new DungeonRewardCatalog(dungeons, Map.of(source.id(), original, solo.id(), foreign)));
		var forbidden = new DungeonRewardDefinition(0, 0, 0, Map.of("MANA_POTION", 1), List.of(),
				ownRewards.supplyPool());
		assertThrows(IllegalArgumentException.class,
				() -> new DungeonRewardCatalog(dungeons, Map.of(source.id(), original, solo.id(), forbidden)));
	}

}
