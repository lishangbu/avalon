package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonProgressQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonRunRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
import io.github.lishangbu.avalon.creature.application.model.CreatureManagementRelease;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.DungeonSupplyQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 正式开本事务测试：使用真实角色与数据库，不把独立战斗模拟等同于副本闭环。 */
class DungeonRunTest extends SpringIntegrationTest {

	@Autowired
	GrowthRules growth;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	DungeonSupplyQueryService supplies;

	@Autowired
	DungeonRunCommandService commands;

	@Autowired
	DungeonRunQueryService queries;

	@Autowired
	io.github.lishangbu.avalon.battle.persistence.repository.DungeonDailyStartStore dailyStarts;

	@Autowired
	io.github.lishangbu.avalon.content.battle.DungeonCatalog dungeonCatalog;

	@Test
	void dailyStartsAreIndependentForEachDungeonAndRollBackWithTheRun() {
		long owner = owner();
		long character = characterQueries.requireId(owner);
		var definition = dungeonCatalog.definition("U_SEAL");
		var day = io.github.lishangbu.avalon.battle.application.model.DungeonDay.at(Instant.now(), definition.zone(),
				definition.resetHour());
		int other = queries.remainingStarts(owner, "F_SEAL");
		transactions.run(() -> {
			characters.lock(owner);
			dailyStarts.useStart(character, "U_SEAL", day);
			dailyStarts.useStart(character, "U_SEAL", day);
		});
		assertEquals(definition.dailyStarts() - 2, queries.remainingStarts(owner, "U_SEAL"));
		assertEquals(other, queries.remainingStarts(owner, "F_SEAL"));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			characters.lock(owner);
			dailyStarts.useStart(character, "F_SEAL", day);
			throw new IllegalStateException("开本事务失败");
		}));
		assertEquals(other, queries.remainingStarts(owner, "F_SEAL"));
		assertEquals(definition.dailyStarts() - 2, queries.remainingStarts(owner, "U_SEAL"));
	}

	@Autowired
	DungeonProgressQueryService progressQueries;

	@Autowired
	DungeonCombatQueryService combatQueries;

	@Autowired
	DungeonBattleRecordRepository dungeonBattleRecordRepository;

	@Autowired
	DungeonRunRecordRepository dungeonRunRecordRepository;

	@Autowired
	ObjectMapper codec;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureManagementService creatureManagement;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	EntityManager em;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService questAcceptance;

	@Autowired
	io.github.lishangbu.avalon.world.application.query.QuestQueryService questQueries;

	@Autowired
	io.github.lishangbu.avalon.world.persistence.repository.QuestRepository questReceipts;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.query.DungeonChestQueryService chestQueries;

	@Autowired
	io.github.lishangbu.avalon.world.quest.QuestRuntimeService questRuntime;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.DungeonChestCommandService chestCommands;

	/** 可选副本独立开本、发布正式任务事件并发独立宝箱；短血量仅隔离事务联动。 */
	@Test
	void optionalRelicVaultCompletesItsOwnAssignmentAndCanBeRepeated() {
		long owner = owner();
		transactions.run(() -> {
			for (String gate : List.of("O014", "N056", "N058")) {
				var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
				receipt.setCharacterId(characterQueries.requireId(owner));
				receipt.setStep(gate);
				receipt.setQuestCode(gate);
				receipt.setCompleted(true);
				receipt.setResponse("{}");
				receipt.setCreatedAt(Instant.now());
				questReceipts.save(receipt);
				facts.record(owner, "QUEST:" + gate);
			}
			em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
				.setParameter("id", characterQueries.requireId(owner))
				.executeUpdate();
		});
		questAcceptance.accept(owner, "N059", new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
				UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
		var assignment = questRuntime.state(owner).getLast();
		transactions.run(() -> em
			.createNativeQuery("UPDATE player_character SET location='NOM_RELIC_VAULT_ENTRANCE' WHERE id=:id")
			.setParameter("id", characterQueries.requireId(owner))
			.executeUpdate());
		assertEquals("NOM_RELIC_VAULT", queries.soloEntrances(owner).getFirst().id());
		String hero = characterQueries.requireId(owner).toString();
		for (int attempt = 0; attempt < 2; attempt++) {
			long run = commands.solo("NOM_RELIC_VAULT", owner, UUID.randomUUID().toString());
			commands.ready(run, owner);
			long battle = commands.begin(run, owner);
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				assertEquals("NOM_RELIC_KEEPER", state.getBattle().getDungeonStage());
				state.getBattle().getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> unit.setHp(1));
				row.setState(codec.writeValueAsString(state));
			});
			var input = new DungeonRoundStateSubmission(List.of(new BattleStateOrder(hero, "ATTACK", "dungeon-boss")),
					Map.of());
			DungeonTestCommands.submit(commands, run, owner, battle, 1, input);
			assertEquals(1, queries.restore(run, owner).getCleared());
			assertNull(characterQueries.adventure(owner).dungeon());
			long xp = characterQueries.find(owner).experience();
			DungeonTestCommands.submit(commands, run, owner, battle, 1, input);
			assertEquals(xp, characterQueries.find(owner).experience());
			assertEquals(attempt + 1, chestQueries.list(owner).size());
			assertTrue(chestQueries.list(owner).stream().allMatch(chest -> chest.name().equals("诺姆遗物库战利品箱")));
		}
		var earnedChests = chestQueries.list(owner);
		for (var chest : earnedChests)
			chestCommands.claim(owner, Long.parseLong(chest.id()));
		assertEquals(2, items.list(owner).stream().filter(item -> item.code().startsWith("NOM_VAULT_")).count());
		var claimedItems = items.list(owner);
		for (var chest : earnedChests)
			chestCommands.claim(owner, Long.parseLong(chest.id()));
		assertEquals(claimedItems, items.list(owner));
		assertTrue(chestQueries.list(owner).isEmpty());
		assertFalse(questQueries.done(owner, "N020"));
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.setParameter("id", characterQueries.requireId(owner))
			.executeUpdate());
		var request = new io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest(
				UUID.randomUUID().toString(), assignment.id(), assignment.round(),
				io.github.lishangbu.avalon.world.quest.model.QuestOperation.DELIVER, "N059", null, null);
		assertEquals(io.github.lishangbu.avalon.quest.model.QuestStatus.COMPLETED,
				questRuntime.command(owner, request).instance().status());
		long xp = characterQueries.find(owner).experience();
		questRuntime.command(owner, request);
		assertEquals(xp, characterQueries.find(owner).experience());
	}

	/** 调低敌人生命隔离事务联动；正式提交、战斗引擎、任务与奖励观察器均真实执行。 */
	@Test
	void nomThreeVictoriesCompleteAcceptedAssignmentsAndReleasePlayer() {
		long owner = owner();
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(20)));
			for (String gate : List.of("N019", "N020")) {
				var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
				receipt.setCharacterId(characterQueries.requireId(owner));
				receipt.setStep(gate);
				receipt.setQuestCode(gate);
				receipt.setCompleted(true);
				receipt.setResponse("{}");
				receipt.setCreatedAt(Instant.now());
				questReceipts.save(receipt);
				facts.record(owner, "QUEST:" + gate);
			}
		});
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=:id")
				.setParameter("id", characterQueries.requireId(owner))
				.executeUpdate());
		for (String step : List.of("N021", "N022_BRIEFING", "N023_BRIEFING"))
			questAcceptance.accept(owner, step,
					new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
							UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
		long run = commands.solo("NOM_SEAL", owner, UUID.randomUUID().toString());
		String hero = characterQueries.requireId(owner).toString();
		for (int stage = 0; stage < 3; stage++) {
			commands.ready(run, owner);
			long battle = commands.begin(run, owner);
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				state.getBattle().getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> unit.setHp(1));
				row.setState(codec.writeValueAsString(state));
			});
			var input = new DungeonRoundStateSubmission(List.of(new BattleStateOrder(hero, "ATTACK", "dungeon-boss")),
					Map.of());
			DungeonTestCommands.submit(commands, run, owner, battle, 1, input);
			assertEquals(stage + 1, queries.restore(run, owner).getCleared());
			assertTrue(questQueries.done(owner, "N0" + (21 + stage)));
			long xp = characterQueries.find(owner).experience();
			long gold = characterQueries.find(owner).gold();
			DungeonTestCommands.submit(commands, run, owner, battle, 1, input);
			assertEquals(xp, characterQueries.find(owner).experience());
			assertEquals(gold, characterQueries.find(owner).gold());
		}
		assertNull(characterQueries.adventure(owner).dungeon());
		assertNull(characterQueries.adventure(owner).battle());
		assertTrue(supplies.available(run, owner).isEmpty());
		assertEquals(1, chestQueries.list(owner).size());
	}

	/** 三阶段逐场由正式提交和战斗结算推进；简化前置与敌方生命只隔离自然成长及难度。 */
	@Test
	void nomSealDungeonCompletesAllThreeStagesAndAwardsCompletionChest() {
		long owner = owner();
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(20)));
			for (String gate : List.of("N019", "N020")) {
				var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
				receipt.setCharacterId(characterQueries.requireId(owner));
				receipt.setStep(gate);
				receipt.setQuestCode(gate);
				receipt.setCompleted(true);
				receipt.setResponse("{}");
				receipt.setCreatedAt(Instant.now());
				questReceipts.save(receipt);
				facts.record(owner, "QUEST:" + gate);
			}
			em.createNativeQuery("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=:id")
				.setParameter("id", characterQueries.requireId(owner))
				.executeUpdate();
		});
		for (String step : List.of("N021", "N022_BRIEFING", "N023_BRIEFING"))
			questAcceptance.accept(owner, step,
					new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
							UUID.randomUUID().toString(), characterQueries.find(owner).revision()));

		long run = commands.solo("NOM_SEAL", owner, UUID.randomUUID().toString());
		String hero = characterQueries.requireId(owner).toString();
		for (int stage = 0; stage < 3; stage++) {
			commands.ready(run, owner);
			long battle = commands.begin(run, owner);
			String expectedStage = List.of("NOM_SEAL_OUTER", "NOM_SEAL_CREATION", "NOM_SEAL_CORE").get(stage);
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				assertEquals(expectedStage, state.getBattle().getDungeonStage());
				state.getBattle().getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> unit.setHp(1));
				row.setState(codec.writeValueAsString(state));
			});
			var input = new DungeonRoundStateSubmission(List.of(new BattleStateOrder(hero, "ATTACK", "dungeon-boss")),
					Map.of());
			DungeonTestCommands.submit(commands, run, owner, battle, 1, input);
			assertEquals("WON", combatQueries.view(run, battle, owner).status());
			assertEquals(stage + 1, queries.restore(run, owner).getCleared());
			assertTrue(questQueries.done(owner, "N0" + (21 + stage)));
		}
		assertNull(characterQueries.adventure(owner).dungeon());
		assertNull(characterQueries.adventure(owner).battle());
		assertEquals(1, chestQueries.list(owner).size());
	}

	@org.junit.jupiter.params.ParameterizedTest
	@org.junit.jupiter.params.provider.ValueSource(strings = { "F", "J" })
	void storyDungeonStagesCompleteAssignmentsAndAwardTheirOwnChest(String prefix) {
		boolean joint = prefix.equals("J");
		String dungeonId = joint ? "J_ABYSS" : "F_SEAL";
		String entrance = joint ? "J_ABYSS_ENTRANCE" : "F_SEAL_FORECOURT";
		long owner = owner();
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(joint ? 40 : 36)));
			for (String gate : joint ? List.of("J005", "J014", "J015") : List.of("F015", "F019", "F020")) {
				var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
				receipt.setCharacterId(characterQueries.requireId(owner));
				receipt.setStep(gate);
				receipt.setQuestCode(gate);
				receipt.setCompleted(true);
				receipt.setResponse("{}");
				receipt.setCreatedAt(Instant.now());
				questReceipts.save(receipt);
				facts.record(owner, "QUEST:" + gate);
			}
		});
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location=:location WHERE id=:id")
			.setParameter("location", entrance)
			.setParameter("id", characterQueries.requireId(owner))
			.executeUpdate());
		assertEquals(dungeonId, queries.soloEntrances(owner).getFirst().id());
		String request = UUID.randomUUID().toString();
		long run = commands.solo(dungeonId, owner, request);
		assertEquals(run, commands.solo(dungeonId, owner, request));
		assertEquals("请先退出当前副本",
				assertThrows(BusinessException.class,
						() -> questAcceptance.accept(owner, "F024",
								new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
										UUID.randomUUID().toString(), characterQueries.find(owner).revision())))
					.getMessage());
		String hero = characterQueries.requireId(owner).toString();
		for (int stage = 0; stage < (joint ? 5 : 3); stage++) {
			String step = prefix + "0" + ((joint ? 16 : 21) + stage);
			var offered = questQueries.state(owner)
				.available()
				.stream()
				.filter(value -> value.id().equals(step))
				.findFirst()
				.orElseThrow();
			assertTrue(offered.dungeonStage());
			assertTrue(offered.acceptAvailable());
			assertFalse(offered.accepted());
			questAcceptance.accept(owner, step,
					new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
							UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
			commands.ready(run, owner);
			long battle = commands.begin(run, owner);
			String expected = dungeonId + "_" + (joint ? List.of("OUTER", "PACK", "CREATION", "PASSAGE", "CORE")
					: List.of("OUTER", "CREATION", "CORE"))
				.get(stage);
			assertThrows(BusinessException.class,
					() -> questAcceptance.accept(owner, step,
							new io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest(
									UUID.randomUUID().toString(), characterQueries.find(owner).revision())));
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				assertEquals(expected, state.getBattle().getDungeonStage());
				state.getBattle().getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(unit -> {
					unit.setHp(1);
					unit.setMp(0);
				});
				state.getBattle()
					.getUnits()
					.stream()
					.filter(unit -> !unit.isEnemy())
					.forEach(unit -> unit.setHp(unit.getStats().health()));
				row.setState(codec.writeValueAsString(state));
			});
			for (int round = 1; round <= 4; round++) {
				var targets = new java.util.ArrayList<String>();
				transactions.run(() -> {
					var row = dungeonBattleRecordRepository.lock(battle);
					var state = codec.readValue(row.getState(), DungeonRoundState.class);
					if (!state.getBattle().getStatus().equals("ACTIVE"))
						return;
					state.getBattle()
						.getUnits()
						.stream()
						.filter(unit -> unit.isEnemy() && unit.alive())
						.forEach(unit -> targets.add(unit.getId()));
				});
				if (targets.isEmpty())
					break;
				var input = new DungeonRoundStateSubmission(
						List.of(new BattleStateOrder(hero, "ATTACK", targets.getFirst())), Map.of());
				DungeonTestCommands.submit(commands, run, owner, battle, round, input);
				long xp = characterQueries.find(owner).experience();
				long gold = characterQueries.find(owner).gold();
				DungeonTestCommands.submit(commands, run, owner, battle, round, input);
				assertEquals(xp, characterQueries.find(owner).experience());
				assertEquals(gold, characterQueries.find(owner).gold());
			}
			String finalState = transactions.call(() -> dungeonBattleRecordRepository.lock(battle).getState());
			assertEquals(stage + 1, queries.restore(run, owner).getCleared(), finalState);
			assertTrue(questQueries.done(owner, step));
		}
		assertNull(characterQueries.adventure(owner).dungeon());
		assertNull(characterQueries.adventure(owner).battle());
		var chests = chestQueries.list(owner);
		assertEquals(1, chests.size());
		assertEquals(joint ? "封印深渊战利品箱" : "火山封印室战利品箱", chests.getFirst().name());
		chestCommands.claim(owner, Long.parseLong(chests.getFirst().id()));
		assertEquals(1,
				items.list(owner)
					.stream()
					.filter(item -> item.code().startsWith(joint ? "CRAFT_8_" : "CRAFT_7_"))
					.count());
	}

	private long owner() {
		String name = "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:O014");
			inventory.grant(owner, "ENTRY", "HEALTH_POTION", 2, false);
			inventory.grant(owner, "BODY", "STARTER_BODY", 1, true);
		});
		// 本测试隔离副本事务，起始地点单独提交，避免与发奖更新相互覆盖。
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE id=:id")
			.setParameter("id", characterQueries.requireId(owner))
			.executeUpdate());
		return owner;
	}

	private long create(List<Long> owners, String request) {
		return transactions.call(() -> commands.create("D01", owners.getFirst(), owners, request));
	}

	/** 正式诺姆资源走真实开本与战局创建，单人不等待其他成员。 */
	@Test
	void nomDungeonStartsOnePlayerAtItsOwnEntranceAndOpensFirstStage() {
		long owner = owner();
		assertTrue(queries.soloEntrances(owner).isEmpty());
		assertThrows(BusinessException.class, () -> commands.solo("D01", owner, UUID.randomUUID().toString()));
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(20)));
			facts.record(owner, "QUEST:N020");
		});
		String request = UUID.randomUUID().toString();
		assertThrows(BusinessException.class,
				() -> transactions.call(() -> commands.create("NOM_SEAL", owner, List.of(owner), request)));
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location='NOM_SEAL_FORECOURT' WHERE id=:id")
				.setParameter("id", characterQueries.requireId(owner))
				.executeUpdate());
		assertEquals("NOM_SEAL", queries.soloEntrances(owner).getFirst().id());
		assertTrue(queries.soloEntrances(owner).getFirst().unlocked());
		long run = commands.solo("NOM_SEAL", owner, request);
		assertEquals(run, commands.solo("NOM_SEAL", owner, request));
		assertEquals("NOM_SEAL", queries.restore(run, owner).getDungeon());
		assertEquals(4, progressQueries.view(run, owner).route().size());
		commands.ready(run, owner);
		long battle = commands.begin(run, owner);
		var state = transactions
			.call(() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class));
		assertEquals(1, state.getMembers().size());
		assertEquals("NOM_SEAL_OUTER", state.getBattle().getDungeonStage());
		assertTrue(state.getBattle()
			.getUnits()
			.stream()
			.filter(BattleStateUnit::isEnemy)
			.allMatch(unit -> unit.getLevel() == 20));
	}

	/** 未知副本不能创建进度或扣发起次数，原入口重放返回同一进度。 */
	@Test
	void explicitDungeonRequiresKnownDefinitionAndSharesIdempotencyWithDefaultEntry() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		String request = UUID.randomUUID().toString();
		assertThrows(IllegalArgumentException.class,
				() -> transactions.call(() -> commands.create("UNKNOWN", leader, owners, request)));
		assertEquals(3, queries.remainingStarts(leader));
		owners.forEach(owner -> assertNull(characterQueries.adventure(owner).dungeon()));
		long run = create(owners, request);
		assertEquals(run, transactions.call(() -> commands.create(leader, owners, request)));
		assertEquals(2, queries.remainingStarts(leader));
	}

	@Test
	void allMembersMustWalkToEntranceBeforeChargingStartQuota() {
		var owners = List.of(owner(), owner(), owner());
		long traveler = owners.getLast();
		transactions.run(() -> {
			facts.record(traveler, "QUEST:O013");
			facts.record(traveler, "QUEST:O014");
		});
		characters.move(traveler, "CITY_OUTSKIRTS", characterQueries.find(traveler).revision());
		String request = UUID.randomUUID().toString();
		assertThrows(BusinessException.class, () -> create(owners, request));
		assertEquals(3, queries.remainingStarts(owners.getFirst()));
		owners.forEach(owner -> assertNull(characterQueries.adventure(owner).dungeon()));
		characters.move(traveler, "FOREST_PEN", characterQueries.find(traveler).revision());
		long run = create(owners, request);
		assertEquals(run, create(owners, request));
		assertEquals(2, queries.remainingStarts(owners.getFirst()));
		owners.forEach(owner -> assertEquals("FOREST_PEN", characterQueries.find(owner).location()));
	}

	@Test
	void expiredPresenceCannotLaunchOrAutomaticallyReadyAfterReconnect() throws Exception {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long run = create(owners, UUID.randomUUID().toString());
		owners.forEach(owner -> commands.ready(run, owner));
		String missing = characterQueries.requireId(owners.get(1)).toString();
		// 模拟持久化截止已过，等同停机或断网跨过租约；不以测试时钟改写生产逻辑。
		transactions.run(() -> {
			var entity = dungeonRunRecordRepository.lock(run);
			var state = codec.readValue(entity.getState(), DungeonProgressState.class);
			state.getMembers().get(missing).setPresenceUntil(Instant.now().minusSeconds(1));
			entity.setState(codec.writeValueAsString(state));
		});
		assertFalse(progressQueries.view(run, leader).canBegin());
		assertTrue(progressQueries.view(run, owners.get(1)).canReady());
		assertThrows(BusinessException.class, () -> commands.begin(run, leader));
		// 无论续期还是开战先取得同一副本锁，旧准备都不能借重连复活。
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var reconnect = executor.submit(() -> commands.presence(run, owners.get(1)));
			var begin = executor.submit(() -> assertThrows(BusinessException.class, () -> commands.begin(run, leader)));
			reconnect.get(10, TimeUnit.SECONDS);
			begin.get(10, TimeUnit.SECONDS);
		}
		assertFalse(queries.restore(run, leader).getMembers().get(missing).isReady());
		assertThrows(BusinessException.class, () -> commands.presence(run, owner()));
		commands.ready(run, owners.get(1));
		long battle = commands.begin(run, leader);
		String before = codec.writeValueAsString(combatQueries.view(run, battle, leader));
		commands.presence(run, owners.get(1));
		assertEquals(before, codec.writeValueAsString(combatQueries.view(run, battle, leader)));
	}

	@Test
	void entryRetryChargesOnlyLeaderAndPreparationChangesRequireNewReady() {
		var owners = List.of(owner(), owner(), owner());
		String request = UUID.randomUUID().toString();
		long run = create(owners, request);
		assertEquals(run, create(owners, request));
		assertEquals(2, queries.remainingStarts(owners.getFirst()));
		assertEquals(3, queries.remainingStarts(owners.get(1)));
		for (long owner : owners) {
			assertEquals(run, characterQueries.adventure(owner).dungeon());
			assertThrows(BusinessException.class,
					() -> transactions.run(() -> characters.requireOutsideDungeon(owner)));
			commands.ready(run, owner);
		}
		long leader = owners.getFirst();
		long gear = Long.parseLong(items.list(leader)
			.stream()
			.filter(item -> item.code().equals("STARTER_BODY"))
			.findFirst()
			.orElseThrow()
			.id());
		inventory.equip(leader, gear);
		assertFalse(
				queries.restore(run, leader).getMembers().get(characterQueries.requireId(leader).toString()).isReady());
		assertThrows(BusinessException.class, () -> commands.begin(run, leader));
		commands.ready(run, leader);
		long battle = commands.begin(run, leader);
		assertEquals(battle, commands.begin(run, owners.get(1)));
		for (long owner : owners)
			assertEquals(battle, characterQueries.adventure(owner).battle());
	}

	@Test
	void outerRollbackRestoresQuotaGatesAndSupplyRegistration() {
		var owners = List.of(owner(), owner(), owner());
		String request = UUID.randomUUID().toString();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.create(owners.getFirst(), owners, request);
			throw new IllegalStateException("模拟匹配事务提交失败");
		}));
		for (long owner : owners) {
			assertNull(characterQueries.adventure(owner).dungeon());
			assertEquals(3, queries.remainingStarts(owner));
		}
		long run = create(owners, request);
		for (long owner : owners)
			assertEquals(2, supplies.available(run, owner).get("HEALTH_POTION"));
		for (long owner : owners)
			commands.leave(run, owner);
		for (long owner : owners)
			assertNull(characterQueries.adventure(owner).dungeon());
		assertThrows(BusinessException.class, () -> commands.rest(run, owners.getFirst()));
	}

	@Test
	void twoAbsentRoundsEndEncounterButKeepRunAndAllowExplicitRetry() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long run = create(owners, UUID.randomUUID().toString());
		assertNull(progressQueries.view(run, leader).previousBattle());
		owners.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, leader);
		// 只提前截止时间，伤害、缺席计数、退场和资源写回仍走正式结算路径。
		for (int round = 0; round < 2; round++) {
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				state.setDeadline(Instant.now().minusSeconds(1));
				row.setDeadline(state.getDeadline());
				row.setState(codec.writeValueAsString(state));
			});
			assertTrue(commands.expire(run, battle));
		}
		var progress = queries.restore(run, leader);
		assertNull(progress.getActiveBattle());
		assertEquals(0, progress.getCleared());
		assertFalse(commands.expire(run, battle));
		assertEquals(Long.toString(battle), progressQueries.view(run, leader).previousBattle());
		for (long owner : owners) {
			assertNull(characterQueries.adventure(owner).battle());
			assertEquals(run, characterQueries.adventure(owner).dungeon());
			commands.rest(run, owner);
			commands.ready(run, owner);
		}
		long retry = commands.begin(run, leader);
		assertNotEquals(battle, retry);
		assertEquals(Long.toString(battle), progressQueries.view(run, leader).previousBattle());
		assertEquals(2, queries.remainingStarts(leader));
	}

	@Test
	void sealedOrdersConsumeOnlyActingPlayersEntrySuppliesOnce() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long run = create(owners, UUID.randomUUID().toString());
		owners.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, leader);
		int round = transactions
			.call(() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class)
				.getBattle()
				.getRound());
		String hero = characterQueries.requireId(leader).toString();
		var potion = new DungeonRoundStateSubmission(List.of(new BattleStateOrder(hero, "HEALTH_POTION", hero)),
				Map.of());
		DungeonTestCommands.submit(commands, run, leader, battle, round, potion);
		DungeonTestCommands.submit(commands, run, leader, battle, round, potion);
		assertEquals(2, supplies.available(run, leader).get("HEALTH_POTION"));
		for (long owner : owners.subList(1, 3)) {
			String id = characterQueries.requireId(owner).toString();
			DungeonTestCommands.submit(commands, run, owner, battle, round,
					new DungeonRoundStateSubmission(List.of(new BattleStateOrder(id, "DEFEND", id)), Map.of()));
		}
		DungeonTestCommands.submit(commands, run, leader, battle, round, potion);
		assertEquals(1, supplies.available(run, leader).get("HEALTH_POTION"));
		for (long owner : owners.subList(1, 3))
			assertEquals(2, supplies.available(run, owner).get("HEALTH_POTION"));
		assertEquals(1,
				items.list(leader)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertTrue(queries.due(Instant.now()).stream().noneMatch(candidate -> candidate.run() == run));
		assertTrue(queries.due(Instant.now().plusSeconds(60)).stream().anyMatch(candidate -> candidate.run() == run));
	}

	@Test
	void fourVictoriesAdvanceOnceAndReleaseGatesWithoutGrantingAnotherRest() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long run = create(owners, UUID.randomUUID().toString());
		for (int stage = 1; stage <= 4; stage++) {
			assertThrows(BusinessException.class, () -> commands.begin(run, leader));
			owners.forEach(owner -> commands.ready(run, owner));
			long battle = commands.begin(run, leader);
			// 该夹具只验证跨场事务。敌人生命降低后仍由正式手动提交、引擎及结算推进，不能用来证明难度。
			var targets = transactions.call(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				var enemies = state.getBattle().getUnits().stream().filter(BattleStateUnit::isEnemy).toList();
				enemies.forEach(enemy -> enemy.setHp(1));
				row.setState(codec.writeValueAsString(state));
				return enemies.stream().map(BattleStateUnit::getId).toList();
			});
			int round = transactions.call(
					() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class)
						.getBattle()
						.getRound());
			for (int index = 0; index < owners.size(); index++) {
				long owner = owners.get(index);
				String hero = characterQueries.requireId(owner).toString();
				var input = new DungeonRoundStateSubmission(
						List.of(new BattleStateOrder(hero, "ATTACK", targets.get(index % targets.size()))), Map.of());
				DungeonTestCommands.submit(commands, run, owner, battle, round, input);
				DungeonTestCommands.submit(commands, run, owner, battle, round, input);
			}
			var state = queries.restore(run, leader);
			assertEquals(stage, state.getCleared());
			assertNull(state.getActiveBattle());
			assertEquals(stage, state.getResolvedBattles().size());
			for (long owner : owners) {
				assertEquals(Long.toString(battle), progressQueries.view(run, owner).previousBattle());
				assertEquals("WON", combatQueries.view(run, battle, owner).status());
			}
			for (long owner : owners)
				assertNull(characterQueries.adventure(owner).battle());
		}
		for (long owner : owners) {
			assertNull(characterQueries.adventure(owner).dungeon());
			assertTrue(supplies.available(run, owner).isEmpty());
			assertEquals(56, characterQueries.find(owner).gold());
			assertTrue(items.list(owner)
				.stream()
				.filter(item -> item.code().equals("MANA_POTION"))
				.mapToInt(item -> item.quantity())
				.sum() >= 1);
		}
		assertThrows(BusinessException.class, () -> commands.rest(run, leader));
		assertThrows(BusinessException.class, () -> commands.begin(run, leader));
		assertEquals(2, queries.remainingStarts(leader));
	}

	@Test
	void replacementCannotReceivePreviousMembersBattleReport() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long run = create(owners, UUID.randomUUID().toString());
		owners.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, leader);
		// 只提前回合截止，仍由正式缺席结算写入已结束战局及参战关系。
		for (int round = 0; round < 2; round++) {
			transactions.run(() -> {
				var row = dungeonBattleRecordRepository.lock(battle);
				var state = codec.readValue(row.getState(), DungeonRoundState.class);
				state.setDeadline(Instant.now().minusSeconds(1));
				row.setDeadline(state.getDeadline());
				row.setState(codec.writeValueAsString(state));
			});
			commands.expire(run, battle);
		}
		long departed = owners.get(2);
		commands.leave(run, departed);
		long replacement = owner();
		transactions.run(() -> commands.join(run, replacement));
		assertNull(progressQueries.view(run, replacement).previousBattle());
		assertEquals(Long.toString(battle), progressQueries.view(run, leader).previousBattle());
		assertThrows(BusinessException.class, () -> combatQueries.view(run, battle, replacement));
		assertThrows(BusinessException.class, () -> progressQueries.view(run, departed));
		assertThrows(BusinessException.class, () -> progressQueries.view(run, owner()));
	}

	@Test
	void replacementPreservesOriginalRequestAndCannotRecycleExitedMembersOrSupplies() {
		var owners = List.of(owner(), owner(), owner());
		String request = UUID.randomUUID().toString();
		long run = create(owners, request);
		long departed = owners.get(2);
		commands.leave(run, departed);
		assertThrows(BusinessException.class, () -> transactions.run(() -> commands.join(run, departed)));
		long replacement = owner();
		transactions.run(() -> commands.join(run, replacement));
		transactions.run(() -> {
			inventory.grant(replacement, "AFTER_ENTRY", "HEALTH_POTION", 3, false);
			commands.join(run, replacement);
		});
		assertEquals(2, supplies.available(run, replacement).get("HEALTH_POTION"));
		assertEquals(3, queries.remainingStarts(replacement));
		assertEquals(run, create(owners, request));
		assertEquals(2, queries.remainingStarts(owners.getFirst()));
		var state = queries.restore(run, replacement);
		assertEquals(4, state.getMembers().size());
		assertEquals(3, state.getMembers().values().stream().filter(member -> !member.isExited()).count());
		assertFalse(state.getMembers().get(characterQueries.requireId(replacement).toString()).isReady());
		var current = List.of(owners.getFirst(), owners.get(1), replacement);
		current.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, replacement);
		assertEquals(battle, characterQueries.adventure(replacement).battle());
		assertNull(characterQueries.adventure(departed).battle());
		assertNull(characterQueries.adventure(departed).dungeon());
	}

	@Test
	void concurrentReplacementsFillOnlyOneVacancyAndRollbackLeavesItAvailable() throws Exception {
		var owners = List.of(owner(), owner(), owner());
		long run = create(owners, UUID.randomUUID().toString());
		commands.leave(run, owners.get(2));
		long first = owner();
		long second = owner();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.join(run, first);
			throw new IllegalStateException("模拟补员确认提交失败");
		}));
		assertNull(characterQueries.adventure(first).dungeon());
		assertTrue(supplies.available(run, first).isEmpty());
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var results = List.of(first, second).stream().map(owner -> executor.submit(() -> {
				start.await();
				try {
					transactions.run(() -> commands.join(run, owner));
					return true;
				}
				catch (BusinessException error) {
					return false;
				}
			})).toList();
			start.countDown();
			assertNotEquals(results.getFirst().get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
		}
		var state = queries.restore(run, owners.getFirst());
		assertEquals(3, state.getMembers().values().stream().filter(member -> !member.isExited()).count());
		for (long candidate : List.of(first, second)) {
			if (characterQueries.adventure(candidate).dungeon() == null)
				assertTrue(supplies.available(run, candidate).isEmpty());
			else
				assertEquals(2, supplies.available(run, candidate).get("HEALTH_POTION"));
		}
	}

	@Test
	void carryingSixStillLocksOnlyFourBattlePetsAndRejectsRosterChanges() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		transactions.run(() -> {
			for (int index = 0; index < 6; index++)
				creatures.grant(leader, "PET:" + index, "CLAW_CAT");
		});
		long run = create(owners, UUID.randomUUID().toString());
		var member = queries.restore(run, leader).getMembers().get(characterQueries.requireId(leader).toString());
		assertEquals(4, member.getRoster().size());
		assertEquals(6, member.getCarried().size());
		var carried = creatureQueries.list(leader)
			.stream()
			.filter(pet -> pet.partyPosition() == 4)
			.findFirst()
			.orElseThrow();
		assertThrows(BusinessException.class, () -> creatures.position(leader, Long.parseLong(carried.id()), 0));
		// 选择本来可放生的随身个体，明确验证副本门禁，而不是误通过出战保护或地点校验。
		var before = creatureQueries.list(leader);
		var deposit = assertThrows(BusinessException.class,
				() -> creatureManagement.deposit(leader, Long.parseLong(carried.id())));
		assertEquals("请先退出当前副本", deposit.getMessage());
		var release = assertThrows(BusinessException.class, () -> creatureManagement.release(leader,
				new CreatureManagementRelease(UUID.randomUUID().toString(), List.of(carried))));
		assertEquals("请先退出当前副本", release.getMessage());
		assertEquals(before, creatureQueries.list(leader), "被拒绝的存仓与放生不能改变个体、位置或来源");
		owners.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, leader);
		var state = transactions
			.call(() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class));
		var pets = state.getBattle().getUnits().stream().filter(unit -> !unit.isEnemy() && !unit.isHero()).toList();
		assertEquals(4, pets.size());
		assertEquals(2, pets.stream().filter(BattleStateUnit::active).count());
		assertTrue(pets.stream().allMatch(unit -> member.getRoster().get(unit.getId()) == unit.getPosition()));
		assertTrue(pets.stream().noneMatch(unit -> unit.getId().equals(carried.id())));
	}

}
