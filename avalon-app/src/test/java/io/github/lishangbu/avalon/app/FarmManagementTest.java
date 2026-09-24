package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.world.application.command.FarmCommandService;
import io.github.lishangbu.avalon.world.application.command.FarmProductionService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyInt;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.FarmState;
import io.github.lishangbu.avalon.content.life.LifeSkillRules.Skill;
import io.github.lishangbu.avalon.world.persistence.repository.FarmRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.*;

class FarmManagementTest extends SpringIntegrationTest {

	@Autowired
	FarmProductionService production;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService bag;

	@Autowired
	LifeSkillRepository learned;

	@MockitoBean
	LifeClock clock;

	@MockitoBean
	LifeToolRandom random;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	LifeSkillCommandService skills;

	@Autowired
	FarmCommandService commands;

	@Autowired
	FarmRepository farms;

	@Autowired
	JdbcClient jdbc;

	private long owner() {
		when(clock.now()).thenReturn(Instant.parse("2026-09-22T00:00:00Z"));
		String name = "farm_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Farm-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 7380);
			characters.changeGold(owner, 2000);
		});
		skills.change(owner, new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.FARMING, false),
				false);
		jdbc.sql("UPDATE player_character SET location='SQUARE' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		return owner;
	}

	private String item(long owner, String code) {
		return bag.list(owner).stream().filter(value -> value.code().equals(code)).findFirst().orElseThrow().id();
	}

	@Test
	void relearningPreservesHighLevelCropsButHarvestCannotSkipProgression() {
		long owner = owner(), id = queries.requireId(owner);
		transactions.run(() -> {
			characters.lock(owner);
			learned.progress(id, Skill.FARMING, LifeSkillRules.threshold(9) * 2);
			inventory.grantStrict(owner, "GOLD_SEED", "SEED_4_0", 1, false, true);
			inventory.grantStrict(owner, "GOLD_HOE", "TOOL_GOLD_HOE", 1, false, false);
		});
		production.perform(owner, new FarmProductionService.Request(UUID.randomUUID().toString(), "PLANT", List.of(0),
				"CROP_4_0", item(owner, "SEED_4_0"), item(owner, "TOOL_GOLD_HOE"), 1));
		commands.manage(owner, new FarmCommandService.Request(UUID.randomUUID().toString(), "WATER", List.of(0)));
		var before = farms.find(id);
		skills.change(owner, new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.FARMING, true),
				true);
		assertEquals(before, farms.find(id));
		when(clock.now()).thenReturn(Instant.parse("2026-10-22T00:00:00Z"));
		var harvest = new FarmProductionService.Request(UUID.randomUUID().toString(), "HARVEST", List.of(0), null, null,
				null, 0);
		assertThrows(BusinessException.class, () -> production.perform(owner, harvest));
		assertEquals(before, farms.find(id));
		skills.change(owner, new LifeSkillCommandService.Request(UUID.randomUUID().toString(), Skill.FARMING, false),
				false);
		var result = production.perform(owner, harvest);
		assertEquals(1, result.completed());
		assertEquals(2, result.produced());
		assertEquals(0, result.proficiency());
		assertEquals(0L, learned.owned(id).get(Skill.FARMING));
		assertEquals(result, production.perform(owner, harvest));
		var crop = bag.list(owner).stream().filter(i -> i.code().equals("CROP_4_0")).findFirst().orElseThrow();
		assertTrue(crop.bound());
		assertEquals(2, crop.quantity());
		assertTrue(farms.find(id).crops().isEmpty());
		assertEquals(1,
				bag.list(owner)
					.stream()
					.filter(i -> i.code().equals("TOOL_WOOD_HOE"))
					.mapToInt(i -> i.quantity())
					.sum());
		verify(random, times(1)).breaks(1000);
	}

	@Test
	void brokenHoeStopsAfterCurrentPlotAndReceiptDoesNotConsumeAgain() {
		long owner = owner();
		transactions.run(() -> inventory.grant(owner, "SEEDS", "SEED_0_0", 3, false));
		when(random.breaks(anyInt())).thenReturn(true);
		var request = new FarmProductionService.Request(UUID.randomUUID().toString(), "PLANT", List.of(0, 1, 2),
				"CROP_0_0", item(owner, "SEED_0_0"), item(owner, "TOOL_WOOD_HOE"), 1);
		var result = production.perform(owner, request);
		assertEquals(1, result.completed());
		assertEquals(1, result.broken());
		assertEquals("TOOLS_EXHAUSTED", result.stop());
		assertEquals(result, production.perform(owner, request));
		assertEquals(1, farms.find(queries.requireId(owner)).crops().size());
		assertEquals(2,
				bag.list(owner)
					.stream()
					.filter(value -> value.code().equals("SEED_0_0"))
					.findFirst()
					.orElseThrow()
					.quantity());
		assertEquals(0L, learned.owned(queries.requireId(owner)).get(Skill.FARMING));
		verify(random, times(1)).breaks(100);
	}

	@Test
	void wateredCropsMatureOfflineAndBoundHarvestIsIdempotent() {
		long owner = owner();
		transactions.run(() -> inventory.grant(owner, "SEEDS", "SEED_0_0", 2, false));
		production.perform(owner, new FarmProductionService.Request(UUID.randomUUID().toString(), "PLANT",
				List.of(0, 1), "CROP_0_0", item(owner, "SEED_0_0"), item(owner, "TOOL_WOOD_HOE"), 1));
		var harvest = new FarmProductionService.Request(UUID.randomUUID().toString(), "HARVEST", List.of(0, 1), null,
				null, null, 0);
		assertThrows(BusinessException.class, () -> production.perform(owner, harvest));
		commands.manage(owner, new FarmCommandService.Request(UUID.randomUUID().toString(), "WATER", List.of(0, 1)));
		when(clock.now()).thenReturn(Instant.parse("2026-10-22T00:00:00Z"));
		var result = production.perform(owner, harvest);
		assertEquals(2, result.completed());
		assertEquals(4, result.produced());
		assertEquals(6, result.proficiency());
		assertEquals(result, production.perform(owner, harvest));
		var crop = bag.list(owner).stream().filter(value -> value.code().equals("CROP_0_0")).findFirst().orElseThrow();
		assertTrue(crop.bound());
		assertEquals(4,
				bag.list(owner)
					.stream()
					.filter(value -> value.code().equals("CROP_0_0"))
					.mapToInt(value -> value.quantity())
					.sum());
		assertTrue(farms.find(queries.requireId(owner)).crops().isEmpty());
		verify(random, times(2)).breaks(100);
	}

	@Test
	void unexpectedPlantingFailureRollsBackSeedsPlotsAndTool() {
		long owner = owner();
		transactions.run(() -> inventory.grant(owner, "SEEDS", "SEED_0_0", 2, false));
		var request = new FarmProductionService.Request(UUID.randomUUID().toString(), "PLANT", List.of(0, 1),
				"CROP_0_0", item(owner, "SEED_0_0"), item(owner, "TOOL_WOOD_HOE"), 1);
		when(random.breaks(anyInt())).thenReturn(false).thenThrow(new IllegalStateException("模拟结算失败"));
		assertThrows(IllegalStateException.class, () -> production.perform(owner, request));
		assertTrue(farms.find(queries.requireId(owner)).crops().isEmpty());
		assertEquals(2,
				bag.list(owner)
					.stream()
					.filter(value -> value.code().equals("SEED_0_0"))
					.findFirst()
					.orElseThrow()
					.quantity());
		doReturn(false).when(random).breaks(anyInt());
		assertEquals(2, production.perform(owner, request).completed());
	}

	@Test
	void fullBagKeepsMatureCropAndDoesNotAwardProficiency() {
		long owner = owner(), id = queries.requireId(owner);
		transactions.run(() -> {
			characters.lock(owner);
			farms.save(id, new FarmState(6,
					Map.of(0, new FarmState.Crop("CROP_0_0", false, Instant.parse("2026-09-21T00:00:00Z")))));
			for (int index = 0; index < 59; index++)
				inventory.grantStrict(owner, "FARM_FILL:" + index, "STARTER_WEAPON", 1, true, false);
		});
		var result = production.perform(owner, new FarmProductionService.Request(UUID.randomUUID().toString(),
				"HARVEST", List.of(0), null, null, null, 0));
		assertEquals("BAG_FULL", result.stop());
		assertEquals(0, result.completed());
		assertEquals(0, result.proficiency());
		assertTrue(farms.find(id).crops().containsKey(0));
		assertFalse(bag.list(owner).stream().anyMatch(value -> value.code().equals("CROP_0_0")));
		verifyNoInteractions(random);
	}

	@Test
	void expansionIsSharedAcrossEntrancesAndReplayDoesNotChargeTwice() {
		long owner = owner();
		var request = new FarmCommandService.Request(UUID.randomUUID().toString(), "EXPAND", List.of());
		assertEquals(7, commands.manage(owner, request).plots());
		assertEquals(7, commands.manage(owner, request).plots());
		assertEquals(1900, queries.find(owner).gold());
		jdbc.sql("UPDATE player_character SET location='GNOME_CITY' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		for (int i = 7; i < 24; i++)
			commands.manage(owner, new FarmCommandService.Request(UUID.randomUUID().toString(), "EXPAND", List.of()));
		assertEquals(24, farms.find(queries.requireId(owner)).plots());
		assertThrows(BusinessException.class, () -> commands.manage(owner,
				new FarmCommandService.Request(UUID.randomUUID().toString(), "EXPAND", List.of())));
		assertEquals(200, queries.find(owner).gold());
	}

	@Test
	void wateringPreservesFirstTimestampAndInvalidBatchLeavesAllUnwatered() {
		long owner = owner(), id = queries.requireId(owner);
		transactions.run(() -> {
			characters.lock(owner);
			farms.save(id, new FarmState(6, Map.of(0, new FarmState.Crop("CROP_0_0", true, null))));
		});
		assertThrows(BusinessException.class, () -> commands.manage(owner,
				new FarmCommandService.Request(UUID.randomUUID().toString(), "WATER", List.of(0, 1))));
		assertNull(farms.find(id).crops().get(0).wateredAt());
		var result = commands.manage(owner,
				new FarmCommandService.Request(UUID.randomUUID().toString(), "WATER", List.of(0)));
		var again = commands.manage(owner,
				new FarmCommandService.Request(UUID.randomUUID().toString(), "WATER", List.of(0)));
		assertEquals(result, again);
		assertTrue(again.crops().get(0).bound());
	}

}
