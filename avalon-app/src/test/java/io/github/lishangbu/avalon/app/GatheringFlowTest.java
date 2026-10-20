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
	io.github.lishangbu.avalon.creature.application.model.CreatureResources creatureResources;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	LifeClock clock;

	@MockitoBean
	LifeToolRandom random;

	@MockitoBean
	GatheringRareRoll rareRoll;

	@MockitoBean
	io.github.lishangbu.avalon.world.domain.GatheringYieldRoll yieldRoll;

	private long tick;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.PvpCommandService pvp;

	@Autowired
	io.github.lishangbu.avalon.battle.application.query.BattleQueryService battleQueries;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.PvpProtectionService protection;

	@Test
	void pvpInterruptsRealGatheringRetainsYieldAndAllowsReturnAfterDefeat() {
		long attacker = owner(), defender = owner();
		long attackerId = queries.requireId(attacker), defenderId = queries.requireId(defender);
		for (long id : new long[] { attackerId, defenderId })
			jdbc.sql("UPDATE player_character SET location='F_VOLCANIC_MINE' WHERE id=:id").param("id", id).update();
		var pet = pets.list(defender).getFirst();
		String tool = items.list(defender)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow()
			.id();
		var session = gathering.begin(defender,
				new GatheringStart(UUID.randomUUID().toString(), "F_SUPPORT_FIELD", pet.id(), tool, 1));
		session = round(defender, session);
		assertEquals(3,
				items.list(defender)
					.stream()
					.filter(item -> item.code().equals("F_SUPPORT_ORE"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> inventory.grant(defender, "GATHER_TRUCE", "PVP_TRUCE_TOKEN", 1, false));
		protection.use(defender, UUID.randomUUID());
		assertThrows(BusinessException.class, () -> pvp.start(attacker, defenderId, UUID.randomUUID()));
		assertNotNull(sessions.find(defenderId));
		tick += 1_800_000;
		var battle = pvp.start(attacker, defenderId, UUID.randomUUID());
		assertNull(sessions.find(defenderId));
		assertEquals(battle.battle().id(), battleQueries.activePvp(defender));
		pvp.turn(defender, Long.parseLong(battle.battle().id()), UUID.randomUUID(), 1, 0, java.util.List.of(), Map.of(),
				true);
		assertNull(battleQueries.activePvp(defender));
		assertEquals("SQUARE", queries.find(defender).location());
		assertEquals(3,
				items.list(defender)
					.stream()
					.filter(item -> item.code().equals("F_SUPPORT_ORE"))
					.mapToInt(item -> item.quantity())
					.sum());
		jdbc.sql("UPDATE player_character SET location='F_VOLCANIC_MINE' WHERE id=:id")
			.param("id", defenderId)
			.update();
		var resumed = gathering.begin(defender,
				new GatheringStart(UUID.randomUUID().toString(), "F_SUPPORT_FIELD", pet.id(), tool, 1));
		assertNotEquals(session.id(), resumed.id());
		resumed = round(defender, resumed);
		assertEquals(1, resumed.completed());
		assertEquals(6,
				items.list(defender)
					.stream()
					.filter(item -> item.code().equals("F_SUPPORT_ORE"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void ordinaryZoneProtectionConsumesOnceBlocksAttackExpiresAndEndsOnAggression() {
		long a = owner(), b = owner();
		long aid = queries.requireId(a), bid = queries.requireId(b);
		for (long id : new long[] { aid, bid })
			jdbc.sql("UPDATE player_character SET location='COPPER_WETLAND' WHERE id=:id").param("id", id).update();
		transactions.run(() -> {
			inventory.grant(a, "TRUCE_A", "PVP_TRUCE_TOKEN", 2, false);
			inventory.grant(b, "TRUCE_B", "PVP_TRUCE_TOKEN", 1, false);
		});
		var request = UUID.randomUUID();
		var until = protection.use(a, request);
		assertEquals(until, protection.use(a, request));
		assertEquals(1,
				items.list(a)
					.stream()
					.filter(item -> item.code().equals("PVP_TRUCE_TOKEN"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertThrows(BusinessException.class, () -> protection.use(a, UUID.randomUUID()));
		assertThrows(BusinessException.class, () -> pvp.start(b, aid, UUID.randomUUID()));
		assertNull(queries.adventure(b).battle());
		assertEquals(until, protection.current(a));
		tick += 1_800_000;
		assertNull(protection.current(a));
		protection.use(b, UUID.randomUUID());
		var started = pvp.start(b, aid, UUID.randomUUID());
		assertNull(protection.current(b));
		pvp.turn(b, Long.parseLong(started.battle().id()), UUID.randomUUID(), 1, 0, java.util.List.of(), Map.of(),
				true);
	}

	@Test
	void pvpRejectsSafeZonesAndOutsidersAndAdvancesOnlyOneExpiredRound() {
		long a = owner(), b = owner(), outsider = owner();
		long aid = queries.requireId(a), bid = queries.requireId(b);
		for (long id : new long[] { aid, bid })
			jdbc.sql("UPDATE player_character SET location='SQUARE' WHERE id=:id").param("id", id).update();
		assertThrows(BusinessException.class, () -> pvp.start(a, bid, UUID.randomUUID()));
		assertNull(queries.adventure(a).battle());
		assertNull(queries.adventure(b).battle());
		for (long id : new long[] { aid, bid })
			jdbc.sql("UPDATE player_character SET location='COPPER_WETLAND' WHERE id=:id").param("id", id).update();
		var first = pvp.start(a, bid, UUID.randomUUID());
		long battle = Long.parseLong(first.battle().id());
		assertEquals(first.battle(), battleQueries.current(a));
		assertEquals(pvp.refresh(b, battle).battle(), battleQueries.current(b));
		assertThrows(BusinessException.class, () -> pvp.refresh(outsider, battle));
		assertThrows(BusinessException.class,
				() -> pvp.turn(outsider, battle, UUID.randomUUID(), 1, 0, java.util.List.of(), Map.of(), true));
		assertThrows(BusinessException.class, () -> pvp.start(b, aid, UUID.randomUUID()));
		tick += 600_000;
		var expired = pvp.refresh(a, battle);
		assertEquals(2, expired.battle().round());
		assertEquals(expired, pvp.refresh(a, battle));
		assertFalse(expired.submitted());
		assertEquals(first.battle().units(), expired.battle().units());
		pvp.turn(a, battle, UUID.randomUUID(), 2, 1, java.util.List.of(), Map.of(), true);
		assertNull(queries.adventure(a).battle());
		assertNull(queries.adventure(b).battle());
	}

	@Test
	void pvpHostLocksBothSidesProjectsTheirOwnPartyAndReplaysCommandsWithoutRewards() {
		long a = owner(), b = owner();
		long aid = queries.requireId(a), bid = queries.requireId(b);
		transactions.run(() -> {
			inventory.grant(a, "PVP_HP", "HEALTH_POTION", 3, false);
			vitalStore.saveVitals(aid, aid, 1, 0);
		});
		for (long id : new long[] { aid, bid })
			jdbc.sql("UPDATE player_character SET location='COPPER_WETLAND' WHERE id=:id").param("id", id).update();
		var start = UUID.randomUUID();
		var first = pvp.start(a, bid, start);
		long battle = Long.parseLong(first.battle().id());
		assertEquals(first, pvp.start(a, bid, start));
		assertEquals(battle, queries.adventure(a).battle());
		assertEquals(battle, queries.adventure(b).battle());
		var opponent = pvp.refresh(b, battle);
		assertTrue(opponent.battle()
			.units()
			.stream()
			.anyMatch(unit -> unit.id().equals(Long.toString(bid)) && !unit.enemy()));
		assertTrue(opponent.battle().captureSupplies().isEmpty());
		var orders = first.battle()
			.units()
			.stream()
			.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
			.map(unit -> new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(),
					unit.hero() ? "HEALTH_POTION" : "DEFEND", unit.hero() ? unit.id() : null))
			.toList();
		var request = UUID.randomUUID();
		var submitted = pvp.turn(a, battle, request, 1, 0, orders, Map.of(), false);
		assertTrue(submitted.submitted());
		assertFalse(submitted.opponentSubmitted());
		assertEquals(3,
				items.list(a)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(submitted, pvp.turn(a, battle, request, 1, 0, orders, Map.of(), false));
		var bOrders = opponent.battle()
			.units()
			.stream()
			.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
			.map(unit -> new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(), "DEFEND", null))
			.toList();
		var next = pvp.turn(b, battle, UUID.randomUUID(), 1, 0, bOrders, Map.of(), false);
		assertEquals(2, next.battle().round());
		assertTrue(pvp.refresh(a, battle)
			.battle()
			.units()
			.stream()
			.anyMatch(unit -> unit.id().equals(Long.toString(aid)) && unit.hp() > 1));
		assertEquals(submitted, pvp.turn(a, battle, request, 1, 0, orders, Map.of(), false));
		long aGold = queries.find(a).gold(), bGold = queries.find(b).gold();
		assertEquals(2,
				items.list(a)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		var surrendered = pvp.turn(b, battle, UUID.randomUUID(), 2, 1, java.util.List.of(), Map.of(), true);
		assertEquals("LOST", surrendered.battle().status());
		assertEquals("WON", pvp.refresh(a, battle).battle().status());
		assertNull(queries.adventure(a).battle());
		assertNull(queries.adventure(b).battle());
		assertEquals(aGold, queries.find(a).gold());
		assertEquals(bGold, queries.find(b).gold());
		assertEquals("SQUARE", queries.find(b).location());
	}

	@Test
	void opposingBattlePairLocksSerializeInStableCharacterOrder() throws Exception {
		long a = owner(), b = owner();
		long aid = queries.requireId(a), bid = queries.requireId(b);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var ready = new java.util.concurrent.CountDownLatch(2);
			var start = new java.util.concurrent.CountDownLatch(1);
			var first = executor.submit(() -> {
				ready.countDown();
				start.await();
				transactions.run(() -> {
					var pair = characters.lockBattlePair(a, bid);
					assertEquals(aid, pair.getFirst().character());
					assertEquals(b, pair.getLast().owner());
					characters.changeGold(a, 1);
					characters.changeGold(b, 1);
				});
				return true;
			});
			var second = executor.submit(() -> {
				ready.countDown();
				start.await();
				transactions.run(() -> {
					var pair = characters.lockBattlePair(b, aid);
					assertEquals(bid, pair.getFirst().character());
					assertEquals(a, pair.getLast().owner());
					characters.changeGold(b, 1);
					characters.changeGold(a, 1);
				});
				return true;
			});
			assertTrue(ready.await(10, TimeUnit.SECONDS));
			start.countDown();
			assertTrue(first.get(15, TimeUnit.SECONDS));
			assertTrue(second.get(15, TimeUnit.SECONDS));
		}
		assertThrows(BusinessException.class, () -> transactions.run(() -> characters.lockBattlePair(a, aid)));
	}

	@Autowired
	io.github.lishangbu.avalon.battle.persistence.repository.PvpBattleRepository pvpBattles;

	@Autowired
	io.github.lishangbu.avalon.battle.persistence.repository.PvpCommandReceiptRepository pvpReceipts;

	@Autowired
	tools.jackson.databind.ObjectMapper pvpJson;

	@Test
	void pvpSharedStateAndExactCommandReceiptSurviveDatabaseReload() {
		long attacker = queries.requireId(owner()), defender = queries.requireId(owner());
		String request = UUID.randomUUID().toString();
		var state = new io.github.lishangbu.avalon.battle.domain.PvpRoundState();
		var battle = new io.github.lishangbu.avalon.battle.domain.BattleState();
		battle.setPvp(true);
		battle.setEncounter("PVP");
		state.setBattle(battle);
		state.setDeadline(Instant.parse("2026-10-19T00:01:00Z"));
		state.getSubmissions()
			.put(Long.toString(attacker),
					new io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission(
							java.util.List.of(new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(
									Long.toString(attacker), "DEFEND", null)),
							Map.of()));
		transactions.run(() -> {
			var record = new io.github.lishangbu.avalon.battle.persistence.entity.PvpBattle();
			record.setAttackerId(attacker);
			record.setDefenderId(defender);
			record.setRequestId(request);
			record.setActive(true);
			record.setState(pvpJson.writeValueAsString(state));
			pvpBattles.saveAndFlush(record);
			var receipt = new io.github.lishangbu.avalon.battle.persistence.entity.PvpCommandReceipt();
			receipt.setBattleId(record.getId());
			receipt.setCharacterId(attacker);
			receipt.setRequestId(request);
			receipt.setPayload("confirmed payload");
			receipt.setResponse("confirmed response");
			pvpReceipts.saveAndFlush(receipt);
		});
		transactions.run(() -> {
			var record = pvpBattles.request(attacker, request);
			assertNotNull(record);
			var restored = pvpJson.readValue(record.getState(),
					io.github.lishangbu.avalon.battle.domain.PvpRoundState.class);
			assertTrue(restored.getBattle().isPvp());
			assertEquals(state.getDeadline(), restored.getDeadline());
			assertEquals(state.getSubmissions(), restored.getSubmissions());
			var receipt = pvpReceipts.request(record.getId(), attacker, request);
			assertEquals("confirmed payload", receipt.getPayload());
			assertEquals("confirmed response", receipt.getResponse());
			assertNull(pvpReceipts.request(record.getId(), defender, request));
		});
	}

	@Test
	void battleInterruptionPreservesEarnedItemsAndRollsBackWithFailedBattleStart() {
		long owner = owner();
		var state = round(owner, start(owner));
		assertEquals(1, ore(owner));
		String session = state.id();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			assertTrue(gathering.interruptForBattle(owner));
			throw new IllegalStateException("模拟对战开启失败");
		}));
		assertEquals(session, sessions.find(queries.requireId(owner)).id());
		assertEquals(1, ore(owner));
		transactions.run(() -> assertTrue(gathering.interruptForBattle(owner)));
		assertNull(sessions.find(queries.requireId(owner)));
		assertEquals(1, ore(owner));
		assertThrows(BusinessException.class, () -> gathering.heartbeat(owner, new GatheringBeat(session, 0, true)));
		transactions.run(() -> {
			assertFalse(gathering.interruptForBattle(owner));
			characters.requireNoLifeActivity(owner);
		});
		assertNotEquals(session, start(owner).id());
	}

	/** 新药圃使用正式采药与自动周期，成功产物即时保留且重放不追加。 */
	@Test
	void restoredGardenProducesHerbsThroughNormalGatheringCycle() {
		long owner = owner();
		learning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.HERBALISM, false),
				false);
		transactions.run(() -> skills.progress(queries.requireId(owner), LifeSkillRulesSkill.HERBALISM, 256));
		jdbc.sql("UPDATE player_character SET location='NOM_RESTORED_HERB_GARDEN' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		var pet = pets.list(owner).getFirst();
		var tool = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PINCERS"))
			.findFirst()
			.orElseThrow();
		var state = gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "NOM_RESTORED_HERBS", pet.id(), tool.id(), 1));
		state = round(owner, state);
		assertEquals(1, state.completed());
		assertEquals(2,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_HERB_0"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(state, gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision() - 1, true)));
		assertEquals(2,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("LIFE_HERB_0"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void fireRepairOreUsesNormalAutomaticGatheringAndDoesNotReplay() {
		long owner = owner();
		jdbc.sql("UPDATE player_character SET location='SALAMANDER_CITY' WHERE id=:id")
			.param("id", queries.requireId(owner))
			.update();
		var pet = pets.list(owner).getFirst();
		var tool = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("TOOL_WOOD_PICKAXE"))
			.findFirst()
			.orElseThrow();
		var state = gathering.begin(owner,
				new GatheringStart(UUID.randomUUID().toString(), "F_REPAIR_ORE", pet.id(), tool.id(), 1));
		org.mockito.Mockito.clearInvocations(gatheringFacts);
		state = round(owner, state);
		assertEquals(1, state.completed());
		assertEquals(1, ore(owner));
		verify(gatheringFacts, times(1)).record(owner, "GATHER_NODE:F_REPAIR_ORE");
		assertEquals(state, gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision() - 1, true)));
		assertEquals(1, ore(owner));
		verify(gatheringFacts, times(1)).record(owner, "GATHER_NODE:F_REPAIR_ORE");
	}

	/** 空产出仍损耗伙伴与工具，不发成功事件；心跳重放不能重新掷骰。 */
	@Test
	void emptyRoundSpendsResourcesAndToolWithoutQuestSuccess() {
		long owner = owner();
		when(yieldRoll.succeeds(anyInt(), anyInt())).thenReturn(false);
		when(random.breaks(anyInt())).thenReturn(true);
		var state = start(owner);
		// 资源可能尚未物化，先通过正式资源读取获得本轮前快照。
		var pet = pets.list(owner).getFirst();
		var initial = creatureResources.read(queries.requireId(owner), Long.parseLong(pet.id()), pet.species(),
				pet.level());
		org.mockito.Mockito.clearInvocations(gatheringFacts);
		state = round(owner, state);
		assertEquals(1, state.completed());
		assertEquals(0, ore(owner));
		assertEquals("TOOLS_EXHAUSTED", state.status());
		assertEquals(initial.hp() - 5, vitals.get(Long.parseLong(pet.id())).getHp());
		assertEquals(initial.mp() - 2, vitals.get(Long.parseLong(pet.id())).getMp());
		assertEquals(0L, skills.owned(queries.requireId(owner)).get(LifeSkillRulesSkill.MINING));
		verify(gatheringFacts, never()).record(owner, "GATHER_SUCCESS:MINING");
		verify(gatheringFacts, never()).record(owner, "GATHER_NODE:MINING_1");
		assertEquals(state, gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision() - 1, true)));
		verify(yieldRoll, times(1)).succeeds(1, 1);
	}

	/** 同一伙伴不变，只提升采矿专业，新会话周期随专业缩短。 */
	@Test
	void gatheringSpeedUsesProfessionalLevelRatherThanPetLevel() {
		long owner = owner();
		var first = start(owner);
		assertEquals(10000, first.roundMillis());
		gathering.stop(owner, new GatheringStop(UUID.randomUUID().toString(), first.id()));
		transactions.run(() -> skills.progress(queries.requireId(owner), LifeSkillRulesSkill.MINING, 65408));
		var faster = start(owner);
		assertEquals(first.creature(), faster.creature());
		assertEquals(6000, faster.roundMillis());
	}

	@org.springframework.test.context.bean.override.mockito.MockitoSpyBean
	io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService gatheringFacts;

	/** 真实周期提交才发送成功事件；已有库存、未完成心跳与重放均不得额外计数。 */
	@Test
	void successfulRoundEmitsOneQuestEventAndReplayEmitsNone() {
		long owner = owner();
		transactions.run(() -> inventory.grant(owner, "EXISTING_ORE", "LIFE_ORE_0", 5, false));
		org.mockito.Mockito.clearInvocations(gatheringFacts);
		var state = start(owner);
		org.mockito.Mockito.verify(gatheringFacts, org.mockito.Mockito.never()).record(owner, "GATHER_SUCCESS:MINING");
		state = round(owner, state);
		assertEquals(1, state.completed());
		org.mockito.Mockito.verify(gatheringFacts, org.mockito.Mockito.times(1)).record(owner, "GATHER_SUCCESS:MINING");
		org.mockito.Mockito.verify(gatheringFacts, org.mockito.Mockito.times(1)).record(owner, "GATHER_NODE:MINING_1");
		assertEquals(state, gathering.heartbeat(owner, new GatheringBeat(state.id(), state.revision() - 1, true)));
		org.mockito.Mockito.verify(gatheringFacts, org.mockito.Mockito.times(1)).record(owner, "GATHER_SUCCESS:MINING");
		org.mockito.Mockito.verify(gatheringFacts, org.mockito.Mockito.times(1)).record(owner, "GATHER_NODE:MINING_1");
	}

	private long owner() {
		when(yieldRoll.succeeds(anyInt(), anyInt())).thenReturn(true);
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
