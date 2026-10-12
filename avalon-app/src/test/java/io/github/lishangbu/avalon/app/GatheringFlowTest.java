package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.GatheringCommandService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.GatheringBeat;
import io.github.lishangbu.avalon.world.application.model.GatheringSession;
import io.github.lishangbu.avalon.world.application.model.GatheringStart;
import io.github.lishangbu.avalon.world.application.model.GatheringStop;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import io.github.lishangbu.avalon.world.domain.LifeClock;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.GatheringSessionRepository;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** 不替换生产服务或数据库，只有时钟和报废抽签可控，验证在线结算与重放。 */
class GatheringFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	LifeSkillCommandService learning;

	@Autowired
	GatheringCommandService gathering;

	@Autowired
	GatheringSessionRepository sessions;

	@Autowired
	LifeSkillRepository skills;

	@Autowired
	CombatVitalsRepository vitals;

	@Autowired
	CombatVitalsStore vitalStore;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	LifeClock clock;

	@MockitoBean
	LifeToolRandom random;

	@MockitoBean
	GatheringRareRoll rareRoll;

	private long tick;

	private long owner() {
		String name = "gth_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Gather-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, 7380);
			creatures.grant(owner, "GATHER", "SPROUT");
		});
		learning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.MINING, false),
				false);
		jdbc.sql("UPDATE player_character SET location='CAVE_ENTRANCE' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		when(clock.now()).thenAnswer(call -> Instant.ofEpochMilli(tick));
		return owner;
	}

	private GatheringSession start(long owner) {
		var pet = pets.list(owner).getFirst();
		var tool = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow();
		return gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "MINING_1", pet.id(), tool.id(), 1));
	}

	private GatheringSession beat(long owner, GatheringSession state, boolean foreground, long elapsed) {
		tick += elapsed;
		return gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision(), foreground));
	}

	private GatheringSession round(long owner, GatheringSession state) {
		state = beat(owner, state, true, 0);
		for (int i = 0; i < 10; i++)
			state = beat(owner, state, true, 1000);
		return state;
	}

	private int ore(long owner) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_0"))
			.mapToInt(item -> item.quantity())
			.sum();
	}

	@Test
	void goldResourceUsesLifeSkillAndToolRatherThanCharacterLevel() {
		when(rareRoll.succeeds(anyInt(), anyInt())).thenReturn(true);
		long owner = owner(), character = queries.requireId(owner);
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(character, LifeSkillRulesSkill.MINING, 65408);
			inventory.grant(owner, "GOLD_TOOL", "TOOL_GOLD_PICKAXE", 1, false);
		});
		jdbc.sql("UPDATE player_character SET location='GOLD_DAWN_VALLEY' WHERE id=:id")
			.param("id", character)
			.update();
		var pet = pets.list(owner).getFirst();
		String wood = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		assertThrows(BusinessException.class, () -> gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "MINING_ORE_9", pet.id(), wood, 1)));
		String gold = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_GOLD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		var state = gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "MINING_ORE_9", pet.id(), gold, 1));
		transactions.run(() -> {
			for (int index = 0; index < 56; index++)
				inventory.grantStrict(owner, "RARE_FILL:" + index, "STARTER_WEAPON", 1, true, false);
		});
		var paused = round(owner, state);
		assertEquals("BAG_FULL", paused.status());
		assertEquals(0, paused.completed());
		verifyNoInteractions(rareRoll);
		String spare = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		transactions.run(() -> inventory.consume(owner, Long.parseLong(spare), "TOOL_WOOD_PICKAXE", 1));
		var result = beat(owner, paused, true, 0);
		assertEquals(1, result.completed());
		assertEquals(1,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("RARE_MINING_4"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(result, gathering.heartbeat(owner, new GatheringBeat(result.id(), result.revision() - 1, true)));
		verify(rareRoll, times(1)).succeeds(9, 9);
		assertEquals(6, queries.find(owner).level());
		assertEquals(1,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_4"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void onlineRoundDebitsOnceAndDuplicateHeartbeatCannotDuplicateRewards() {
		long owner = owner();
		var state = start(owner);
		state = round(owner, state);
		assertEquals(1, state.completed());
		assertEquals(1, ore(owner));
		assertEquals(2L, skills.owned(queries.requireId(owner)).get(LifeSkillRulesSkill.MINING));
		var before = vitals.get(Long.parseLong(state.creature()));
		int hp = before.getHp(), mp = before.getMp();
		assertEquals(state, gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision() - 1, true)));
		assertEquals(hp, vitals.get(Long.parseLong(state.creature())).getHp());
		assertEquals(mp, vitals.get(Long.parseLong(state.creature())).getMp());
		assertEquals(1, ore(owner));
		assertEquals(state, sessions.find(queries.requireId(owner)));
	}

	@Test
	void disconnectAndBackgroundPauseWhileStopReleasesMovement() {
		long owner = owner();
		var state = start(owner);
		state = beat(owner, state, true, 0);
		state = beat(owner, state, true, 1000);
		long progress = state.progressMillis();
		state = beat(owner, state, true, 3600000);
		assertEquals(progress, state.progressMillis());
		state = beat(owner, state, false, 1000);
		state = beat(owner, state, true, 3600000);
		assertEquals(progress, state.progressMillis());
		assertEquals(0, ore(owner));
		var stop = new GatheringStop(UUID.randomUUID().toString(), state.id());
		gathering.stop(owner, stop);
		gathering.stop(owner, stop);
		assertNull(sessions.find(queries.requireId(owner)));
		var next = start(owner);
		gathering.stop(owner, stop);
		assertEquals(next, sessions.find(queries.requireId(owner)));
	}

	@Test
	void lastToolBreaksAfterOutputAndNeverConsumesUnselectedStock() {
		long owner = owner();
		transactions.run(() -> inventory.grant(owner, "EXTRA_TOOL", "TOOL_WOOD_PICKAXE", 2, false));
		when(random.breaks(anyInt())).thenReturn(true);
		var state = round(owner, start(owner));
		assertEquals(1, state.completed());
		assertEquals("TOOLS_EXHAUSTED", state.status());
		assertEquals(1, ore(owner));
		state = round(owner, state);
		assertEquals(1, state.completed());
		assertEquals(1, ore(owner));
		assertEquals(2,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void lowResourcesAndFullBagDoNotConsumeOrProduce() {
		long owner = owner();
		var state = start(owner);
		long pet = Long.parseLong(state.creature());
		transactions.run(() -> vitalStore.saveVitals(queries.requireId(owner), pet, 5, 2));
		state = round(owner, state);
		assertEquals("RESOURCES_LOW", state.status());
		assertEquals(0, ore(owner));
		assertEquals(5, vitals.get(pet).getHp());
		transactions.run(() -> {
			vitalStore.saveVitals(queries.requireId(owner), pet, 20, 10);
			for (int i = 0; i < 59; i++)
				inventory.grant(owner, "FILL:" + i, "STARTER_WEAPON", 1, true);
		});
		state = beat(owner, state, true, 1000);
		assertEquals("BAG_FULL", state.status());
		assertEquals(0, ore(owner));
		assertEquals(20, vitals.get(pet).getHp());
		verifyNoInteractions(random);
	}

	@Test
	void concurrentCompletionReplaysAndFailedSettlementRollsBackEverything() throws Exception {
		long owner = owner();
		var state = beat(owner, start(owner), true, 0);
		for (int index = 0; index < 9; index++)
			state = beat(owner, state, true, 1000);
		var before = state;
		tick += 1000;
		var request = new GatheringBeat(state.id(), state.revision(), true);
		when(random.breaks(anyInt())).thenThrow(new IllegalStateException("模拟报废结算失败"));
		assertThrows(IllegalStateException.class, () -> gathering.heartbeat(owner, request));
		assertEquals(0, ore(owner));
		assertEquals(before, sessions.find(queries.requireId(owner)));
		assertEquals(0L, skills.owned(queries.requireId(owner)).get(LifeSkillRulesSkill.MINING));
		doReturn(false).when(random).breaks(anyInt());
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> gathering.heartbeat(owner, request));
			var second = executor.submit(() -> gathering.heartbeat(owner, request));
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(1, ore(owner));
		assertEquals(1, sessions.find(queries.requireId(owner)).completed());
	}

	@Test
	void aHundredGatheringReceiptsUseTwoOreSlotsAndRemainUsable() {
		long owner = owner();
		when(random.breaks(anyInt())).thenReturn(false);
		transactions.run(() -> {
			inventory.grantWithBinding(owner, "LONG_HP", "HEALTH_POTION", 99, false, false);
			inventory.grantWithBinding(owner, "LONG_MP", "MANA_POTION", 99, false, false);
		});
		var owned = items.list(owner);
		String hp = owned.stream().filter(i -> i.code().equals("HEALTH_POTION")).findFirst().orElseThrow().id();
		String mp = owned.stream().filter(i -> i.code().equals("MANA_POTION")).findFirst().orElseThrow().id();
		String tool = owned.stream().filter(i -> i.code().equals("TOOL_WOOD_PICKAXE")).findFirst().orElseThrow().id();
		var state = gathering.begin(owner, new GatheringStart(UUID.randomUUID().toString(), "MINING_1",
				pets.list(owner).getFirst().id(), tool, 1, Map.of(hp, 99, mp, 99)));
		int before = items.bag(owner).usedSlots();
		for (int index = 0; index < 99; index++)
			state = round(owner, state);
		assertEquals(99, ore(owner));
		assertEquals(before + 1, items.bag(owner).usedSlots());
		state = round(owner, state);
		assertEquals(100, state.completed());
		assertEquals(100, ore(owner));
		assertEquals(before + 2, items.bag(owner).usedSlots());
		gathering.stop(owner, new GatheringStop(UUID.randomUUID().toString(), state.id()));
		transactions.run(() -> {
			for (var item : items.list(owner))
				if (item.code().equals("LIFE_ORE_0"))
					inventory.consume(owner, Long.parseLong(item.id()), item.code(), item.quantity());
		});
		assertEquals(0, ore(owner));
		assertEquals(before, items.bag(owner).usedSlots());
	}

	@Test
	void selectedAutoSuppliesAreConsumedOnlyForSuccessfulRoundAndBudgetIsRespected() {
		long owner = owner();
		transactions.run(() -> {
			inventory.grantWithBinding(owner, "HP", "HEALTH_POTION", 3, false, false);
			inventory.grantWithBinding(owner, "MP", "MANA_POTION", 3, false, false);
		});
		var owned = items.list(owner);
		String hp = owned.stream().filter(item -> item.code().equals("HEALTH_POTION")).findFirst().orElseThrow().id();
		String mp = owned.stream().filter(item -> item.code().equals("MANA_POTION")).findFirst().orElseThrow().id();
		var pet = pets.list(owner).getFirst();
		String tool = owned.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		long petId = Long.parseLong(pet.id());
		transactions.run(() -> vitalStore.saveVitals(queries.requireId(owner), petId, 5, 0));
		var state = gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "MINING_1", pet.id(), tool, 1, Map.of(hp, 1, mp, 1)));
		state = round(owner, state);
		assertEquals(1, ore(owner));
		assertEquals(0, state.supplies().get(hp));
		assertEquals(0, state.supplies().get(mp));
		assertEquals(2,
				items.list(owner).stream().filter(item -> item.id().equals(hp)).findFirst().orElseThrow().quantity());
		assertTrue(vitals.get(petId).getHp() > 5);
		transactions.run(() -> vitalStore.saveVitals(queries.requireId(owner), petId, 5, 0));
		state = round(owner, state);
		assertEquals("RESOURCES_LOW", state.status());
		assertEquals(1, ore(owner));
		assertEquals(2,
				items.list(owner).stream().filter(item -> item.id().equals(mp)).findFirst().orElseThrow().quantity());
	}

	@Test
	void fullBagDoesNotWasteAutoSupplies() {
		long owner = owner();
		transactions.run(() -> {
			inventory.grant(owner, "HP", "HEALTH_POTION", 2, false);
			for (int i = 0; i < 58; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_WEAPON", 1, true);
		});
		var owned = items.list(owner);
		String hp = owned.stream().filter(item -> item.code().equals("HEALTH_POTION")).findFirst().orElseThrow().id();
		String tool = owned.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		var pet = pets.list(owner).getFirst();
		transactions.run(() -> vitalStore.saveVitals(queries.requireId(owner), Long.parseLong(pet.id()), 5, 10));
		var state = gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "MINING_1", pet.id(), tool, 1, Map.of(hp, 1)));
		state = round(owner, state);
		assertEquals("BAG_FULL", state.status());
		assertEquals(1, state.supplies().get(hp));
		assertEquals(5, vitals.get(Long.parseLong(pet.id())).getHp());
		assertEquals(2,
				items.list(owner).stream().filter(item -> item.id().equals(hp)).findFirst().orElseThrow().quantity());
		assertEquals(0, ore(owner));
	}

	@Test
	void anotherOwnerCannotAdvanceOrStopTheSession() {
		long owner = owner(), other = owner();
		var state = start(owner);
		assertThrows(BusinessException.class, () -> gathering.heartbeat(other, new GatheringBeat(state.id(), 0, true)));
		assertThrows(BusinessException.class,
				() -> gathering.stop(other, new GatheringStop(UUID.randomUUID().toString(), state.id())));
		assertEquals(state, sessions.find(queries.requireId(owner)));
	}

}
