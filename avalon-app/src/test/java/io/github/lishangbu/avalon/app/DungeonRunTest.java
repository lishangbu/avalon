package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonCombatQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonProgressQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.DungeonProgressState;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonBattleRecordRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonRunRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
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
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

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

	private long owner() {
		String name = "run_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:BQ029");
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
		return transactions.call(() -> commands.create(owners.getFirst(), owners, request));
	}

	@Test
	void allMembersMustWalkToEntranceBeforeChargingStartQuota() {
		var owners = List.of(owner(), owner(), owner());
		long traveler = owners.getLast();
		transactions.run(() -> facts.record(traveler, "QUEST:BQ025"));
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
		var potion = new DungeonRoundState.Submission(List.of(new BattleState.Order(hero, "HEALTH_POTION", hero)),
				Map.of());
		commands.submit(run, leader, battle, round, potion);
		commands.submit(run, leader, battle, round, potion);
		assertEquals(2, supplies.available(run, leader).get("HEALTH_POTION"));
		for (long owner : owners.subList(1, 3)) {
			String id = characterQueries.requireId(owner).toString();
			commands.submit(run, owner, battle, round,
					new DungeonRoundState.Submission(List.of(new BattleState.Order(id, "DEFEND", id)), Map.of()));
		}
		commands.submit(run, leader, battle, round, potion);
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
				var enemies = state.getBattle().getUnits().stream().filter(BattleState.Unit::isEnemy).toList();
				enemies.forEach(enemy -> enemy.setHp(1));
				row.setState(codec.writeValueAsString(state));
				return enemies.stream().map(BattleState.Unit::getId).toList();
			});
			int round = transactions.call(
					() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class)
						.getBattle()
						.getRound());
			for (int index = 0; index < owners.size(); index++) {
				long owner = owners.get(index);
				String hero = characterQueries.requireId(owner).toString();
				var input = new DungeonRoundState.Submission(
						List.of(new BattleState.Order(hero, "ATTACK", targets.get(index % targets.size()))), Map.of());
				commands.submit(run, owner, battle, round, input);
				commands.submit(run, owner, battle, round, input);
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
				new CreatureManagementService.Release(UUID.randomUUID().toString(), List.of(carried))));
		assertEquals("请先退出当前副本", release.getMessage());
		assertEquals(before, creatureQueries.list(leader), "被拒绝的存仓与放生不能改变个体、位置或来源");
		owners.forEach(owner -> commands.ready(run, owner));
		long battle = commands.begin(run, leader);
		var state = transactions
			.call(() -> codec.readValue(dungeonBattleRecordRepository.get(battle).getState(), DungeonRoundState.class));
		var pets = state.getBattle().getUnits().stream().filter(unit -> !unit.isEnemy() && !unit.isHero()).toList();
		assertEquals(4, pets.size());
		assertEquals(2, pets.stream().filter(BattleState.Unit::active).count());
		assertTrue(pets.stream().allMatch(unit -> member.getRoster().get(unit.getId()) == unit.getPosition()));
		assertTrue(pets.stream().noneMatch(unit -> unit.getId().equals(carried.id())));
	}

}
