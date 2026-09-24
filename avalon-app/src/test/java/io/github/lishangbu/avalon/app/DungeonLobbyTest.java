package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonLobbyCommandService;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.application.query.DungeonLobbyQueryService;
import io.github.lishangbu.avalon.battle.application.query.DungeonRunQueryService;
import io.github.lishangbu.avalon.battle.domain.DungeonLobbyState;
import io.github.lishangbu.avalon.battle.persistence.repository.DungeonLobbyRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

/** 确认与个人战斗门禁的真实事务测试；不以内部房间调用替代自动匹配或页面验收。 */
class DungeonLobbyTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entranceFixture;

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
	DungeonLobbyCommandService commands;

	@Autowired
	DungeonLobbyQueryService queries;

	@Autowired
	DungeonRunQueryService runs;

	@Autowired
	DungeonLobbyRepository rows;

	@Autowired
	ObjectMapper codec;

	@Autowired
	DungeonRunCommandService runCommands;

	private long owner() {
		String name = "lobby_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Dungeon-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		tx(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:BQ029");
		});
		// 副本专项从集合地点开始；相邻移动由独立入口用例验证。
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return owner;
	}

	private void tx(Runnable work) {
		transactions.run(work);
	}

	private long lobby(List<Long> owners) {
		long lobby = commands.create(owners.getFirst(), UUID.randomUUID().toString());
		owners.subList(1, owners.size()).forEach(owner -> commands.join(lobby, owner));
		return lobby;
	}

	@Test
	void matchingFillsOldestWaitingTeamWithoutConfirmingOrSpendingStarts() {
		var players = List.of(owner(), owner(), owner(), owner());
		long first = commands.match(players.getFirst());
		assertEquals(first, commands.match(players.getFirst()));
		assertNull(queries.restore(first, players.getFirst()).getMembers().get(players.getFirst()).getDeadline());
		assertEquals(first, commands.match(players.get(1)));
		assertEquals(first, commands.match(players.get(2)));
		var full = queries.restore(first, players.getFirst());
		assertEquals(3, full.getMembers().size());
		assertTrue(full.isAutomatic());
		full.getMembers().values().forEach(member -> {
			assertNotNull(member.getDeadline());
			assertFalse(member.isConfirmed());
			assertFalse(member.isReady());
		});
		players.forEach(player -> assertEquals(3, runs.remainingStarts(player)));
		commands.leave(first, players.get(2));
		assertEquals(first, commands.match(players.get(3)), "拒绝后剩余队员继续补人");
		for (long player : List.of(players.get(0), players.get(1), players.get(3)))
			commands.leave(first, player);
	}

	@Test
	void concurrentMatchingCreatesOneTeamAndRollsBackFailedAllocation() throws Exception {
		var players = List.of(owner(), owner(), owner());
		var start = new CountDownLatch(1);
		try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
			var futures = players.stream().map(player -> pool.submit(() -> {
				start.await();
				return commands.match(player);
			})).toList();
			start.countDown();
			long team = futures.getFirst().get(10, TimeUnit.SECONDS);
			for (var future : futures)
				assertEquals(team, future.get(10, TimeUnit.SECONDS));
			assertEquals(3, queries.restore(team, players.getFirst()).getMembers().size());
			for (long player : players)
				commands.leave(team, player);
		}
		long candidate = owner();
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			commands.match(candidate);
			throw new IllegalStateException("模拟匹配提交失败");
		}));
		assertNull(characterQueries.lobby(candidate));
		long retry = commands.match(candidate);
		commands.leave(retry, candidate);
	}

	@Test
	void replacementRequiresConsentAndRechecksRevisionWithoutSpendingStarts() {
		var owners = List.of(owner(), owner(), owner());
		long lobby = lobby(owners);
		owners.forEach(player -> {
			commands.confirm(lobby, player);
			commands.ready(lobby, player);
		});
		long run = commands.launch(lobby, owners.getFirst());
		long candidate = owner();
		assertThrows(BusinessException.class, () -> runCommands.recruit(run, candidate, true));
		assertThrows(BusinessException.class, () -> runCommands.recruit(run, owners.getFirst(), true));
		runCommands.leave(run, owners.get(2));
		assertThrows(BusinessException.class, () -> commands.replacement(run, candidate, UUID.randomUUID().toString()));
		runCommands.recruit(run, owners.getFirst(), true);
		String request = UUID.randomUUID().toString();
		long replacement = commands.replacement(run, candidate, request);
		assertEquals(replacement, commands.replacement(run, candidate, request));
		assertNull(characterQueries.adventure(candidate).dungeon(), "申请不等于入本");
		assertThrows(BusinessException.class, () -> commands.join(replacement, owner()));
		assertThrows(BusinessException.class, () -> commands.ready(replacement, candidate));
		assertThrows(BusinessException.class, () -> commands.launch(replacement, candidate));
		commands.confirm(replacement, candidate);
		commands.ready(replacement, candidate);
		tx(() -> characters.invalidatePreparation(candidate));
		assertThrows(BusinessException.class, () -> commands.launch(replacement, candidate));
		commands.ready(replacement, candidate);
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			commands.launch(replacement, candidate);
			throw new IllegalStateException("模拟补员确认提交失败");
		}));
		assertNull(characterQueries.adventure(candidate).dungeon());
		assertEquals(replacement, characterQueries.lobby(candidate));
		assertEquals(run, commands.launch(replacement, candidate));
		assertEquals(run, commands.launch(replacement, candidate));
		assertEquals(replacement, commands.replacement(run, candidate, request));
		assertNull(characterQueries.lobby(candidate));
		assertEquals(3, runs.remainingStarts(candidate));
		assertEquals(2, runs.remainingStarts(owners.getFirst()));
		assertFalse(runs.restore(run, candidate).isRecruiting());
		assertFalse(runs.restore(run, candidate)
			.getMembers()
			.get(characterQueries.requireId(candidate).toString())
			.isReady());
	}

	@Test
	void concurrentConfirmedCandidatesCannotOverfillTheLastVacancy() throws Exception {
		var owners = List.of(owner(), owner(), owner());
		long lobby = lobby(owners);
		owners.forEach(player -> {
			commands.confirm(lobby, player);
			commands.ready(lobby, player);
		});
		long run = commands.launch(lobby, owners.getFirst());
		runCommands.leave(run, owners.get(2));
		runCommands.recruit(run, owners.getFirst(), true);
		var candidates = List.of(owner(), owner());
		var pending = candidates.stream().map(candidate -> {
			long room = commands.replacement(run, candidate, UUID.randomUUID().toString());
			commands.confirm(room, candidate);
			commands.ready(room, candidate);
			return room;
		}).toList();
		var gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var results = IntStream.range(0, 2).mapToObj(index -> executor.submit(() -> {
				gate.await();
				try {
					commands.launch(pending.get(index), candidates.get(index));
					return true;
				}
				catch (BusinessException error) {
					return false;
				}
			})).toList();
			gate.countDown();
			assertNotEquals(results.getFirst().get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
		}
		assertEquals(3,
				runs.restore(run, owners.getFirst())
					.getMembers()
					.values()
					.stream()
					.filter(member -> !member.isExited())
					.count());
		for (int index = 0; index < candidates.size(); index++) {
			long candidate = candidates.get(index);
			assertEquals(3, runs.remainingStarts(candidate));
			if (characterQueries.adventure(candidate).dungeon() == null) {
				commands.leave(pending.get(index), candidate);
				assertNull(characterQueries.lobby(candidate));
			}
		}
	}

	@Test
	void cancelledRecruitmentAndExpiryPreventAdmissionAndReleaseReservation() {
		var owners = List.of(owner(), owner(), owner());
		long lobby = lobby(owners);
		owners.forEach(player -> {
			commands.confirm(lobby, player);
			commands.ready(lobby, player);
		});
		long run = commands.launch(lobby, owners.getFirst());
		runCommands.leave(run, owners.get(2));
		runCommands.recruit(run, owners.getFirst(), true);
		assertThrows(BusinessException.class,
				() -> commands.replacement(run, owners.get(2), UUID.randomUUID().toString()));
		long candidate = owner();
		long replacement = commands.replacement(run, candidate, UUID.randomUUID().toString());
		commands.confirm(replacement, candidate);
		commands.ready(replacement, candidate);
		runCommands.recruit(run, owners.getFirst(), false);
		assertThrows(BusinessException.class, () -> commands.launch(replacement, candidate));
		tx(() -> {
			var row = rows.lock(replacement);
			var state = codec.readValue(row.getState(), DungeonLobbyState.class);
			state.getMembers().get(candidate).setDeadline(Instant.now().minusSeconds(1));
			row.setState(codec.writeValueAsString(state));
		});
		commands.expire(replacement);
		assertNull(characterQueries.lobby(candidate));
		assertNull(characterQueries.adventure(candidate).dungeon());
	}

	@Test
	void confirmationKeepsExistingBattleButBlocksNewBattleUntilRelease() {
		var owners = List.of(owner(), owner(), owner());
		long player = owners.get(1);
		long lobby = lobby(owners);
		// 用角色战局门禁夹具模拟已经存在的个人战斗；确认不能清除该编号。
		tx(() -> characters.setBattle(player, 123L, false));
		commands.confirm(lobby, player);
		assertEquals(123L, characterQueries.adventure(player).battle());
		assertThrows(BusinessException.class, () -> commands.ready(lobby, player));
		tx(() -> characters.setBattle(player, null, false));
		assertThrows(BusinessException.class, () -> tx(() -> characters.setBattle(player, 124L, false)));
		assertNull(characterQueries.adventure(player).battle());
		commands.ready(lobby, player);
		commands.leave(lobby, player);
		tx(() -> characters.setBattle(player, 125L, false));
		assertEquals(125L, characterQueries.adventure(player).battle());
		assertNull(characterQueries.lobby(player));
		assertEquals(3, runs.remainingStarts(owners.getFirst()));
	}

	@Test
	void fullManualConfirmationLaunchesOnceAndChangedPreparationBlocksLaunch() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		long lobby = lobby(owners);
		assertThrows(BusinessException.class, () -> commands.launch(lobby, leader));
		for (long owner : owners) {
			commands.confirm(lobby, owner);
			commands.confirm(lobby, owner);
			commands.ready(lobby, owner);
		}
		tx(() -> characters.invalidatePreparation(leader));
		assertFalse(queries.restore(lobby, leader).getMembers().get(leader).isReady());
		assertThrows(BusinessException.class, () -> commands.launch(lobby, leader));
		assertEquals(3, runs.remainingStarts(leader));
		commands.ready(lobby, leader);
		long run = commands.launch(lobby, leader);
		assertEquals(run, commands.launch(lobby, owners.get(1)));
		assertEquals(2, runs.remainingStarts(leader));
		for (long owner : owners) {
			assertEquals(run, characterQueries.adventure(owner).dungeon());
			assertNull(characterQueries.lobby(owner));
		}
	}

	@Test
	void expiryReleasesOnlyExpiredMemberAndDoesNotStopTheirPersonalBattle() {
		var owners = List.of(owner(), owner(), owner());
		long lobby = lobby(owners);
		long player = owners.get(1);
		tx(() -> characters.setBattle(player, 126L, false));
		commands.confirm(lobby, player);
		tx(() -> {
			var row = rows.lock(lobby);
			var state = codec.readValue(row.getState(), DungeonLobbyState.class);
			state.getMembers().get(player).setDeadline(Instant.now().minusSeconds(1));
			row.setDeadline(Instant.now().minusSeconds(1));
			row.setState(codec.writeValueAsString(state));
		});
		assertThrows(BusinessException.class, () -> commands.confirm(lobby, player));
		assertTrue(queries.due(Instant.now()).contains(lobby));
		commands.expire(lobby);
		commands.expire(lobby);
		assertNull(characterQueries.lobby(player));
		assertEquals(126L, characterQueries.adventure(player).battle());
		assertEquals(2, queries.restore(lobby, owners.getFirst()).getMembers().size());
		long replacement = owner();
		commands.join(lobby, replacement);
		assertEquals(3, queries.restore(lobby, replacement).getMembers().size());
		assertEquals(3, runs.remainingStarts(owners.getFirst()));
	}

	@Test
	void reservationAndLaunchRollbackLeaveNoOrphanGateOrSpentQuota() {
		var owners = List.of(owner(), owner(), owner());
		long leader = owners.getFirst();
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			commands.create(leader, UUID.randomUUID().toString());
			throw new IllegalStateException("模拟招募提交失败");
		}));
		assertNull(characterQueries.lobby(leader));
		long lobby = lobby(owners);
		owners.forEach(owner -> {
			commands.confirm(lobby, owner);
			commands.ready(lobby, owner);
		});
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			commands.launch(lobby, leader);
			throw new IllegalStateException("模拟确认开本提交失败");
		}));
		assertNull(queries.restore(lobby, leader).getRun());
		assertEquals(3, runs.remainingStarts(leader));
		for (long owner : owners) {
			assertNull(characterQueries.adventure(owner).dungeon());
			assertEquals(lobby, characterQueries.lobby(owner));
		}
		assertTrue(commands.launch(lobby, leader) > 0);
	}

	@Test
	void concurrentRoomsCannotReserveSamePlayerAndRepeatedConfirmDoesNotExtendDeadline() throws Exception {
		long firstLeader = owner();
		long secondLeader = owner();
		long player = owner();
		long first = commands.create(firstLeader, UUID.randomUUID().toString());
		long second = commands.create(secondLeader, UUID.randomUUID().toString());
		assertNull(queries.restore(first, firstLeader).getMembers().get(firstLeader).getDeadline());
		assertThrows(BusinessException.class, () -> commands.confirm(first, firstLeader));
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var results = List.of(first, second).stream().map(lobby -> executor.submit(() -> {
				start.await();
				try {
					commands.join(lobby, player);
					return true;
				}
				catch (BusinessException error) {
					return false;
				}
			})).toList();
			start.countDown();
			assertNotEquals(results.getFirst().get(15, TimeUnit.SECONDS), results.get(1).get(15, TimeUnit.SECONDS));
		}
		long joined = characterQueries.lobby(player);
		long third = owner();
		commands.join(joined, third);
		commands.confirm(joined, player);
		var deadline = queries.restore(joined, player).getMembers().get(player).getDeadline();
		commands.confirm(joined, player);
		assertEquals(deadline, queries.restore(joined, player).getMembers().get(player).getDeadline());
		assertEquals(3, runs.remainingStarts(firstLeader));
		assertEquals(3, runs.remainingStarts(secondLeader));
	}

	@Test
	void declinedLeaderDoesNotDisbandOthersOrChargeReplacementWithoutConsent() {
		var owners = List.of(owner(), owner(), owner());
		long lobby = lobby(owners);
		commands.leave(lobby, owners.getFirst());
		var state = queries.restore(lobby, owners.get(1));
		assertFalse(state.isClosed());
		assertEquals(0, state.getLeader());
		assertEquals(2, state.getMembers().size());
		assertNull(characterQueries.lobby(owners.getFirst()));
		long replacement = owner();
		commands.join(lobby, replacement);
		for (long owner : List.of(owners.get(1), owners.get(2), replacement)) {
			commands.confirm(lobby, owner);
			commands.ready(lobby, owner);
		}
		assertThrows(BusinessException.class, () -> commands.launch(lobby, replacement));
		commands.lead(lobby, replacement);
		assertTrue(commands.launch(lobby, replacement) > 0);
		assertEquals(2, runs.remainingStarts(replacement));
		for (long owner : owners)
			assertEquals(3, runs.remainingStarts(owner));
	}

}
