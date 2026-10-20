package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 多人回合可序列化恢复；并发串行化由后续持久化层的副本行锁承担。 */
@Tag("rules")
class DungeonRoundTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DungeonBattlePolicy dungeon = fixture.dungeonPolicy;

	private final DungeonRoundCoordinator rounds = fixture.rounds;

	private final ObjectMapper json = new ObjectMapper();

	private static final Instant NOW = Instant.parse("2026-09-17T00:00:00Z");

	private static final BattleEngineResources NO_ITEMS = new BattleEngineResources() {
		public int count(BattleStateUnit actor, String code) {
			return 0;
		}

		public void take(BattleStateUnit actor, String code) {
			fail("不得扣除物品");
		}

		public boolean room(BattleStateUnit target) {
			return false;
		}

		public void capture(BattleStateUnit target) {
			fail("不得捕捉");
		}
	};

	private BattleStateUnit hero(String id) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setHero(true);
		unit.setLevel(15);
		unit.setStats(new BattleStateStats(2000, 100, 150, 150, 100, 100, 50));
		unit.setHp(2000);
		unit.setMp(100);
		return unit;
	}

	private BattleStateUnit pet(String id, String owner, int position, int health) {
		var unit = hero(id);
		unit.setHero(false);
		unit.setOwnerHero(owner);
		unit.setPosition(position);
		unit.setHp(health);
		return unit;
	}

	private DungeonRoundState open(BattleStateUnit... extra) {
		var battle = dungeon.create("D01_2", List.of(hero("a"), hero("b"), hero("c")), 83);
		battle.getUnits().addAll(List.of(extra));
		return rounds.open(battle, NOW);
	}

	private DungeonRoundStateSubmission defend(String... ids) {
		return new DungeonRoundStateSubmission(
				Arrays.stream(ids).map(id -> new BattleStateOrder(id, "DEFEND", id)).toList(), Map.of());
	}

	/** 同目录单人阶段使用自身人数、提交窗口和缺席阈值。 */
	@Test
	void soloStageUsesItsOwnRoundConfigurationInsteadOfDefaultDungeon() {
		var source = fixture.dungeon.definition();
		var defaultDefinition = new io.github.lishangbu.avalon.content.battle.model.DungeonDefinition(source.id(),
				source.name(), source.level(), source.requires(), source.entrance(), 3, 3, 5, source.zone(), 45, 2, 60,
				180, 300, List.of(0), source.supplies(), List.of(source.stages().getFirst()));
		var soloDefinition = new io.github.lishangbu.avalon.content.battle.model.DungeonDefinition("SOLO", "单人封印室", 20,
				"N020", "NOM_SEAL_FORECOURT", 1, 3, 5, source.zone(), 90, 3, 60, 180, 300, List.of(0),
				source.supplies(), List.of(source.stages().get(1)));
		var catalog = new io.github.lishangbu.avalon.content.battle.DungeonCatalog(
				List.of(defaultDefinition, soloDefinition), fixture.creatures, fixture.quests, fixture.items,
				fixture.world);
		var coordinator = new DungeonRoundCoordinator(fixture.engine, catalog);
		var battle = dungeon.create("D01_2", List.of(hero("a")), 83);
		var state = coordinator.open(battle, NOW);
		assertEquals(NOW.plusSeconds(90), state.getDeadline());
		assertFalse(coordinator.settle(state, NOW.plusSeconds(45), NO_ITEMS).changed());
		coordinator.submit(state, "a", 1, defend("a"), NOW.plusSeconds(60), NO_ITEMS);
		assertTrue(coordinator.settle(state, NOW.plusSeconds(60), NO_ITEMS).changed());
		assertEquals(NOW.plusSeconds(150), state.getDeadline());
		for (int missed = 1; missed <= 3; missed++) {
			assertTrue(coordinator.settle(state, state.getDeadline(), NO_ITEMS).changed());
			assertEquals(missed, state.getMembers().get("a").getMissedRounds());
			assertEquals(missed == 3, state.getMembers().get("a").isRemoved());
		}
		assertEquals("LOST", state.getBattle().getStatus());
		assertFalse(coordinator.settle(state, state.getDeadline(), NO_ITEMS).changed());
	}

	@Test
	void sealedSubmissionsSurviveReloadAndResolveOnlyOnceWhenEveryoneIsReady() throws Exception {
		var state = open();
		rounds.submit(state, "a", 1, defend("a"), NOW, NO_ITEMS);
		rounds.submit(state, "a", 1, defend("a"), NOW.plusSeconds(1), NO_ITEMS);
		assertEquals(1, state.getSubmissions().size());
		assertEquals(1, state.getBattle().getRound());
		assertFalse(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS).changed());
		assertThrows(BusinessException.class, () -> rounds.submit(state, "a", 1,
				new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "ATTACK", "dungeon-boss")), Map.of()),
				NOW, NO_ITEMS));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1, defend("c"), NOW, NO_ITEMS));
		var restored = json.readValue(json.writeValueAsString(state), DungeonRoundState.class);
		for (var next : List.of(state, restored)) {
			rounds.submit(next, "b", 1, defend("b"), NOW, NO_ITEMS);
			rounds.submit(next, "c", 1, defend("c"), NOW, NO_ITEMS);
			assertTrue(rounds.settle(next, NOW.plusSeconds(2), NO_ITEMS).changed());
			assertEquals(2, next.getBattle().getRound());
			assertEquals(NOW.plusSeconds(47), next.getDeadline());
			assertTrue(next.getSubmissions().isEmpty());
			assertFalse(rounds.settle(next, NOW.plusSeconds(2), NO_ITEMS).changed());
			assertTrue(rounds.eligible(next, "a"));
		}
		assertEquals(json.writeValueAsString(state), json.writeValueAsString(restored));
	}

	@Test
	void deadlineIsStrictMissingIsNotDefendAndTwoMissesRemoveAllOwnedUnits() {
		var state = open(pet("b-reserve", "b", 2, 2000));
		// 固定本轮已预告受击者，隔离验证未提交不获得防御减伤。
		state.getBattle().setEnemyOrders(List.of(new BattleStateOrder("dungeon-boss", "DUNGEON_HEAVY", "b")));
		assertThrows(BusinessException.class,
				() -> rounds.submit(state, "a", 1, defend("a"), NOW.plusSeconds(45), NO_ITEMS));
		rounds.submit(state, "a", 1, defend("a"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(45), NO_ITEMS).changed());
		assertEquals(1, state.getMembers().get("b").getMissedRounds());
		assertTrue(state.getBattle().getLog().stream().anyMatch(line -> line.equals("b本轮未提交行动。")));
		assertFalse(state.getBattle().getLog().contains("b防御。"));
		rounds.submit(state, "a", 2, defend("a"), NOW.plusSeconds(46), NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(90), NO_ITEMS).changed());
		assertTrue(state.getMembers().get("b").isRemoved());
		assertTrue(state.getBattle().getUnits().stream().noneMatch(unit -> unit.getId().startsWith("b")));
		assertTrue(state.getWithdrawn()
			.get("b")
			.stream()
			.filter(BattleStateUnit::isHero)
			.findFirst()
			.orElseThrow()
			.getHp() < 2000);
		assertTrue(state.getWithdrawn()
			.get("b")
			.stream()
			.filter(BattleStateUnit::isHero)
			.findFirst()
			.orElseThrow()
			.getHp() < state.getBattle()
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
		assertTrue(rounds.settle(abandoned, NOW.plusSeconds(3600), NO_ITEMS).changed());
		assertEquals(2, abandoned.getBattle().getRound());
		assertFalse(rounds.settle(abandoned, NOW.plusSeconds(3600), NO_ITEMS).changed());
		assertTrue(rounds.settle(abandoned, NOW.plusSeconds(3645), NO_ITEMS).changed());
		assertEquals("LOST", abandoned.getBattle().getStatus());
		assertFalse(rounds.settle(abandoned, NOW.plusSeconds(4000), NO_ITEMS).changed());
	}

	@Test
	void replacementPositionsBelongToEachMemberAndSubmissionDoesNotMovePetsEarly() {
		var aDead = pet("a-dead", "a", 0, 0);
		var bDead = pet("b-dead", "b", 0, 0);
		var aReserve = pet("a-reserve", "a", 2, 2000);
		var bReserve = pet("b-reserve", "b", 2, 2000);
		var state = open(aDead, bDead, aReserve, bReserve);
		assertThrows(BusinessException.class,
				() -> rounds.submit(state, "a", 1,
						new DungeonRoundStateSubmission(defend("a", "b-reserve").orders(), Map.of("b-reserve", 0)), NOW,
						NO_ITEMS));
		for (String hero : List.of("a", "b")) {
			rounds.submit(state, hero, 1, new DungeonRoundStateSubmission(defend(hero, hero + "-reserve").orders(),
					Map.of(hero + "-reserve", 0)), NOW, NO_ITEMS);
		}
		assertEquals(2, aReserve.getPosition());
		assertEquals(2, bReserve.getPosition());
		rounds.submit(state, "c", 1, defend("c"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS).changed());
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
						new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "DEFEND", "a"),
								new BattleStateOrder("a-active", "SWAP", "b-reserve")), Map.of()),
						NOW, NO_ITEMS));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1,
				new DungeonRoundStateSubmission(List.of(new BattleStateOrder("b", "NO_ACTION", null)), Map.of()), NOW,
				NO_ITEMS));
		assertTrue(state.getSubmissions().isEmpty());
		assertFalse(rounds.eligible(state, "a"));
	}

	@Test
	void voluntaryExitKeepsLockedActionButForfeitsRewardWithoutWaitingForLeaver() {
		var state = open();
		rounds.submit(state, "a", 1,
				new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "ATTACK", "dungeon-boss")), Map.of()),
				NOW, NO_ITEMS);
		rounds.leave(state, "a");
		rounds.leave(state, "b");
		rounds.submit(state, "c", 1, defend("c"), NOW, NO_ITEMS);
		assertTrue(rounds.settle(state, NOW.plusSeconds(1), NO_ITEMS).changed());
		assertTrue(state.getBattle()
			.getUnits()
			.stream()
			.filter(BattleStateUnit::isEnemy)
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
		state.getBattle().getUnits().stream().filter(BattleStateUnit::isHero).forEach(unit -> unit.setHp(0));
		assertFalse(rounds.settle(state, NOW.plusSeconds(44), NO_ITEMS).changed());
		assertTrue(rounds.settle(state, NOW.plusSeconds(45), NO_ITEMS).changed());
		assertEquals("LOST", state.getBattle().getStatus());

		var rescued = open(pet("reserve", "a", 2, 2000));
		rescued.getBattle().getUnits().stream().filter(BattleStateUnit::isHero).forEach(unit -> unit.setHp(0));
		rounds.submit(rescued, "a", 1,
				new DungeonRoundStateSubmission(defend("reserve").orders(), Map.of("reserve", 0)), NOW, NO_ITEMS);
		assertTrue(rounds.settle(rescued, NOW.plusSeconds(45), NO_ITEMS).changed());
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
		assertTrue(rounds.settle(state, NOW, NO_ITEMS).changed());
		assertEquals(0, state.getMembers().get("a").getMissedRounds());
		assertFalse(rounds.eligible(state, "a"));
		assertTrue(rounds.eligible(state, "b"));
	}

	@Test
	void suppliesAreValidatedAndConsumedAgainstActingHeroNotSharedTeamInventory() {
		var state = open();
		var stock = new HashMap<>(Map.of("a", 1, "b", 0, "c", 0));
		var resources = new BattleEngineResources() {
			public int count(String code) {
				throw new AssertionError("多人不能读取共享库存");
			}

			public int count(BattleStateUnit actor, String code) {
				return stock.get(actor.getId());
			}

			public boolean room(BattleStateUnit target) {
				return false;
			}

			public void take(String code) {
				fail("多人不能扣共享库存");
			}

			public void take(BattleStateUnit actor, String code) {
				stock.compute(actor.getId(), (id, count) -> count - 1);
			}

			public void capture(BattleStateUnit target) {
				fail("副本不能捕捉");
			}
		};
		state.getBattle().getUnits().get(1).setHp(1700);
		rounds.submit(state, "a", 1,
				new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "HEALTH_POTION", "b")), Map.of()),
				NOW, resources);
		assertEquals(1, stock.get("a"));
		assertThrows(BusinessException.class, () -> rounds.submit(state, "b", 1,
				new DungeonRoundStateSubmission(List.of(new BattleStateOrder("b", "HEALTH_POTION", "b")), Map.of()),
				NOW, resources));
		rounds.submit(state, "b", 1, defend("b"), NOW, resources);
		rounds.submit(state, "c", 1, defend("c"), NOW, resources);
		var result = rounds.settle(state, NOW, resources);
		assertTrue(result.changed());
		// 领域结算不得触碰真实库存；应用事务消费输出后，才发生一次指定归属的扣除。
		assertEquals(1, stock.get("a"));
		assertEquals(1, result.round().changes().size());
		var change = result.round().changes().getFirst();
		assertEquals("a", change.owner());
		assertEquals("HEALTH_POTION", change.reference());
		var actor = result.round()
			.state()
			.getUnits()
			.stream()
			.filter(unit -> unit.getId().equals(change.owner()))
			.findFirst()
			.orElseThrow();
		resources.take(actor, change.reference());
		assertEquals(0, stock.get("a"));
		assertEquals(0, stock.get("b"));
		assertEquals(1820, state.getBattle().getUnits().get(1).getHp());
	}

}
