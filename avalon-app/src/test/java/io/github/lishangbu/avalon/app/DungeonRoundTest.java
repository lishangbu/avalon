package io.github.lishangbu.avalon.app;

import tools.jackson.databind.ObjectMapper;
import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.domain.BattleState.*;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState.Submission;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 多人回合可序列化恢复；并发串行化由后续持久化层的副本行锁承担。 */
@Tag("rules")
class DungeonRoundTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DungeonBattlePolicy dungeon = fixture.dungeonPolicy;

	private final DungeonRoundCoordinator rounds = fixture.rounds;

	private final ObjectMapper json = new ObjectMapper();

	private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");

	private static final BattleEngine.Resources NO_ITEMS = new BattleEngine.Resources() {
		public int count(String code) {
			return 0;
		}

		public boolean room(Unit target) {
			return false;
		}

		public void take(String code) {
			fail("不得消耗物品");
		}

		public void capture(Unit target) {
			fail("不得捕捉");
		}
	};

	private Unit hero(String id) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setHero(true);
		unit.setLevel(15);
		unit.setStats(new Stats(2000, 100, 150, 150, 100, 100, 50));
		unit.setHp(2000);
		unit.setMp(100);
		return unit;
	}

	private Unit pet(String id, String owner, int position, int health) {
		var unit = hero(id);
		unit.setHero(false);
		unit.setOwnerHero(owner);
		unit.setPosition(position);
		unit.setHp(health);
		return unit;
	}

	private DungeonRoundState open(Unit... extra) {
		var battle = dungeon.create("D01_2", List.of(hero("a"), hero("b"), hero("c")), 83);
		battle.getUnits().addAll(List.of(extra));
		return rounds.open(battle, NOW);
	}

	private Submission defend(String... ids) {
		return new Submission(Arrays.stream(ids).map(id -> new Order(id, "DEFEND", id)).toList(), Map.of());
	}

	@Test
	void sealedSubmissionsSurviveReloadAndResolveOnlyOnceWhenEveryoneIsReady() throws Exception {
		var state = open();
		rounds.submit(state, "a", 1, defend("a"), NOW, NO_ITEMS);
		rounds.submit(state, "a", 1, defend("a"), NOW.plusSeconds(1), NO_ITEMS);
		assertEquals(1, state.getSubmissions().size());
		assertEquals(1, state.getBattle().getRound());
		assertFalse(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "a", 1,
				new Submission(List.of(new Order("a", "ATTACK", "dungeon-boss")), Map.of()), NOW, NO_ITEMS));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1, defend("c"), NOW, NO_ITEMS));
		var restored = json.readValue(json.writeValueAsString(state), DungeonRoundState.class);
		for (var next : List.of(state, restored)) {
			rounds.submit(next, "b", 1, defend("b"), NOW, NO_ITEMS);
			rounds.submit(next, "c", 1, defend("c"), NOW, NO_ITEMS);
			assertTrue(rounds.settle(next, NOW.plusSeconds(2), NO_ITEMS));
			assertEquals(2, next.getBattle().getRound());
			assertEquals(NOW.plusSeconds(47), next.getDeadline());
			assertTrue(next.getSubmissions().isEmpty());
			assertFalse(rounds.settle(next, NOW.plusSeconds(2), NO_ITEMS));
			assertTrue(rounds.eligible(next, "a"));
		}
		assertEquals(json.writeValueAsString(state), json.writeValueAsString(restored));
	}

	@Test
	void deadlineIsStrictMissingIsNotDefendAndTwoMissesRemoveAllOwnedUnits() {
		var state = open(pet("b-reserve", "b", 2, 2000));
		// 固定本轮已预告受击者，隔离验证未提交不获得防御减伤。
		state.getBattle().setEnemyOrders(List.of(new Order("dungeon-boss", "DUNGEON_HEAVY", "b")));
		assertThrows(BusinessException.class,
				() -> rounds.submit(state, "a", 1, defend("a"), NOW.plusSeconds(45), NO_ITEMS));
		rounds.submit(state, "a", 1, defend("a"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(45), NO_ITEMS));
		assertEquals(1, state.getMembers().get("b").getMissedRounds());
		assertTrue(state.getBattle().getLog().stream().anyMatch(line -> line.equals("b本轮未提交行动。")));
		assertFalse(state.getBattle().getLog().contains("b防御。"));
		rounds.submit(state, "a", 2, defend("a"), NOW.plusSeconds(46), NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(90), NO_ITEMS));
		assertTrue(state.getMembers().get("b").isRemoved());
		assertTrue(state.getBattle().getUnits().stream().noneMatch(unit -> unit.getId().startsWith("b")));
		assertTrue(
				state.getWithdrawn().get("b").stream().filter(Unit::isHero).findFirst().orElseThrow().getHp() < 2000);
		assertTrue(state.getWithdrawn().get("b").stream().filter(Unit::isHero).findFirst().orElseThrow().getHp() < state
			.getBattle()
			.getUnits()
			.stream()
			.filter(unit -> unit.getId().equals("a"))
			.findFirst()
			.orElseThrow()
			.getHp());
		assertEquals(2000,
				state.getWithdrawn()
					.get("b")
					.stream()
					.filter(unit -> !unit.isHero())
					.findFirst()
					.orElseThrow()
					.getHp());
		assertFalse(rounds.eligible(state, "b"));
		assertThrows(BusinessException.class,
				() -> rounds.submit(state, "b", 3, defend("b"), NOW.plusSeconds(91), NO_ITEMS));

		var abandoned = open();
		assertTrue(rounds.settle(abandoned, NOW.plusSeconds(3600), NO_ITEMS));
		assertEquals(2, abandoned.getBattle().getRound());
		assertFalse(rounds.settle(abandoned, NOW.plusSeconds(3600), NO_ITEMS));
		assertTrue(rounds.settle(abandoned, NOW.plusSeconds(3645), NO_ITEMS));
		assertEquals("LOST", abandoned.getBattle().getStatus());
		assertFalse(rounds.settle(abandoned, NOW.plusSeconds(4000), NO_ITEMS));
	}

	@Test
	void replacementPositionsBelongToEachMemberAndSubmissionDoesNotMovePetsEarly() {
		var aDead = pet("a-dead", "a", 0, 0);
		var bDead = pet("b-dead", "b", 0, 0);
		var aReserve = pet("a-reserve", "a", 2, 2000);
		var bReserve = pet("b-reserve", "b", 2, 2000);
		var state = open(aDead, bDead, aReserve, bReserve);
		assertThrows(BusinessException.class, () -> rounds.submit(state, "a", 1,
				new Submission(defend("a", "b-reserve").orders(), Map.of("b-reserve", 0)), NOW, NO_ITEMS));
		for (String hero : List.of("a", "b")) {
			rounds.submit(state, hero, 1,
					new Submission(defend(hero, hero + "-reserve").orders(), Map.of(hero + "-reserve", 0)), NOW,
					NO_ITEMS);
		}
		assertEquals(2, aReserve.getPosition());
		assertEquals(2, bReserve.getPosition());
		rounds.submit(state, "c", 1, defend("c"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS));
		assertEquals(0, aReserve.getPosition());
		assertEquals(0, bReserve.getPosition());
		assertEquals(2, aDead.getPosition());
		assertEquals(2, bDead.getPosition());
	}

	@Test
	void activeSwapCannotStealAnotherMembersReserveAndNoActionCannotBeSubmitted() {
		var state = open(pet("a-active", "a", 0, 2000), pet("b-reserve", "b", 2, 2000));
		assertThrows(BusinessException.class,
				() -> rounds.submit(state, "a", 1,
						new Submission(
								List.of(new Order("a", "DEFEND", "a"), new Order("a-active", "SWAP", "b-reserve")),
								Map.of()),
						NOW, NO_ITEMS));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1,
				new Submission(List.of(new Order("b", "NO_ACTION", null)), Map.of()), NOW, NO_ITEMS));
		assertTrue(state.getSubmissions().isEmpty());
		assertFalse(rounds.eligible(state, "a"));
	}

	@Test
	void voluntaryExitKeepsLockedActionButForfeitsRewardWithoutWaitingForLeaver() {
		var state = open();
		rounds.submit(state, "a", 1, new Submission(List.of(new Order("a", "ATTACK", "dungeon-boss")), Map.of()), NOW,
				NO_ITEMS);
		rounds.leave(state, "a");
		rounds.leave(state, "b");
		rounds.submit(state, "c", 1, defend("c"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS));
		assertTrue(state.getBattle()
			.getUnits()
			.stream()
			.filter(Unit::isEnemy)
			.filter(unit -> !unit.isMechanism())
			.findFirst()
			.orElseThrow()
			.getHp() < 3600);
		assertFalse(rounds.eligible(state, "a"));
		assertTrue(state.getMembers().get("a").isRemoved());
		assertTrue(state.getMembers().get("b").isRemoved());
		assertTrue(rounds.eligible(state, "c"));
	}

	@Test
	void onlyReservesSurvivingRequiresManualReplacementByNextDeadline() {
		var state = open(pet("reserve", "a", 2, 2000));
		state.getBattle().getUnits().stream().filter(Unit::isHero).forEach(unit -> unit.setHp(0));
		assertFalse(rounds.settle(state, NOW.plusSeconds(44), NO_ITEMS));
		assertTrue(rounds.settle(state, NOW.plusSeconds(45), NO_ITEMS));
		assertEquals("LOST", state.getBattle().getStatus());

		var rescued = open(pet("reserve", "a", 2, 2000));
		rescued.getBattle().getUnits().stream().filter(Unit::isHero).forEach(unit -> unit.setHp(0));
		rounds.submit(rescued, "a", 1, new Submission(defend("reserve").orders(), Map.of("reserve", 0)), NOW, NO_ITEMS);
		assertTrue(rounds.settle(rescued, NOW.plusSeconds(45), NO_ITEMS));
		assertEquals("ACTIVE", rescued.getBattle().getStatus());
		assertEquals(0,
				rescued.getBattle()
					.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals("reserve"))
					.findFirst()
					.orElseThrow()
					.getPosition());
	}

	@Test
	void defeatedMemberMayManuallyAcknowledgeButEmptySubmissionCannotCreateRewardEligibility() {
		var state = open();
		assertThrows(BusinessException.class, () -> rounds.submit(state, "a", 1, defend(), NOW, NO_ITEMS));
		state.getBattle().getUnits().getFirst().setHp(0);
		rounds.submit(state, "a", 1, defend(), NOW, NO_ITEMS);
		rounds.submit(state, "b", 1, defend("b"), NOW, NO_ITEMS);
		rounds.submit(state, "c", 1, defend("c"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW, NO_ITEMS));
		assertEquals(0, state.getMembers().get("a").getMissedRounds());
		assertFalse(rounds.eligible(state, "a"));
		assertTrue(rounds.eligible(state, "b"));
	}

	@Test
	void suppliesAreValidatedAndConsumedAgainstActingHeroNotSharedTeamInventory() {
		var state = open();
		var stock = new HashMap<>(Map.of("a", 1, "b", 0, "c", 0));
		var resources = new BattleEngine.Resources() {
			public int count(String code) {
				throw new AssertionError("多人不能读取共享库存");
			}

			public int count(Unit actor, String code) {
				return stock.get(actor.getId());
			}

			public boolean room(Unit target) {
				return false;
			}

			public void take(String code) {
				fail("多人不能扣共享库存");
			}

			public void take(Unit actor, String code) {
				stock.compute(actor.getId(), (id, count) -> count - 1);
			}

			public void capture(Unit target) {
				fail("副本不能捕捉");
			}
		};
		state.getBattle().getUnits().get(1).setHp(1700);
		rounds.submit(state, "a", 1, new Submission(List.of(new Order("a", "HEALTH_POTION", "b")), Map.of()), NOW,
				resources);
		assertEquals(1, stock.get("a"));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1,
				new Submission(List.of(new Order("b", "HEALTH_POTION", "b")), Map.of()), NOW, resources));
		rounds.submit(state, "b", 1, defend("b"), NOW, resources);
		rounds.submit(state, "c", 1, defend("c"), NOW, resources);
		assertTrue(rounds.settle(state, NOW, resources));
		assertEquals(0, stock.get("a"));
		assertEquals(0, stock.get("b"));
		assertEquals(1820, state.getBattle().getUnits().get(1).getHp());
	}

}
