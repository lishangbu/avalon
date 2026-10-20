package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.*;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PvpBattleEngineTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final BattleEngineResources resources = new BattleEngineResources() {
		public int count(BattleStateUnit actor, String code) {
			return 10;
		}

		public boolean room(BattleStateUnit target) {
			throw new AssertionError("PVP不能查询捕捉容量");
		}

		public void take(BattleStateUnit actor, String code) {
			throw new AssertionError("纯引擎不能扣真实物资");
		}

		public void capture(BattleStateUnit target) {
			throw new AssertionError("PVP不能捕捉");
		}
	};

	@Test
	void bothHumanOrdersResolveWithoutAiAndStateRemainsIsolated() {
		var state = battle();
		state.setEnemyOrders(List.of(new BattleStateOrder("b", "ATTACK", "a")));
		fixture.engine.planEnemies(state);
		assertTrue(state.getEnemyOrders().isEmpty());
		var orders = List.of(new BattleStateOrder("a", "ATTACK", "b"), new BattleStateOrder("b", "ATTACK", "a"));
		var result = fixture.engine.advance(state, orders, false, BattleResourceSnapshot.read(state, orders, resources),
				Map.of());
		assertEquals(1000, state.getUnits().getFirst().getHp());
		assertTrue(result.state().getUnits().stream().allMatch(unit -> unit.getHp() < 1000));
		state.copyFrom(result.state());
		assertTrue(state.isPvp());
		assertEquals(2, state.getRound());
		assertTrue(state.getEnemyOrders().isEmpty());
		assertThrows(BusinessException.class, () -> fixture.engine.validateMember(BattleStateCodec.copy(state), "a",
				List.of(new BattleStateOrder("b", "ATTACK", "a")), Map.of(), resources));
	}

	@Test
	void enemyReserveStaysOffFieldAndItsOwnerCanReplaceAFallenPet() {
		var state = battle();
		var fallen = unit("fallen", true, false, "b", 0);
		fallen.setHp(0);
		var reserve = unit("reserve", true, false, "b", 2);
		state.getUnits().addAll(List.of(fallen, reserve));
		assertFalse(reserve.active());
		var input = List.of(new BattleStateOrder("b", "DEFEND", null), new BattleStateOrder("reserve", "ATTACK", "a"));
		fixture.engine.validateMember(BattleStateCodec.copy(state), "b", input, Map.of("reserve", 0), resources);
		assertEquals(2, reserve.getPosition());
		var orders = new ArrayList<>(input);
		orders.add(new BattleStateOrder("a", "DEFEND", null));
		var result = fixture.engine.advance(state, orders, false, BattleResourceSnapshot.read(state, orders, resources),
				Map.of("reserve", 0));
		assertEquals(0,
				result.state()
					.getUnits()
					.stream()
					.filter(unit -> unit.getId().equals("reserve"))
					.findFirst()
					.orElseThrow()
					.getPosition());
		assertTrue(result.state().getUnits().getFirst().getHp() < 1000);
		assertThrows(BusinessException.class, () -> fixture.engine.validateMember(BattleStateCodec.copy(state), "a",
				input, Map.of("reserve", 0), resources));
	}

	@Test
	void bothSidesHaveInventorySnapshotsButCapturesAndRetreatAreRejected() {
		var state = battle();
		var snapshot = BattleResourceSnapshot.read(state, List.of(new BattleStateOrder("b", "HEALTH_POTION", "b")),
				resources);
		assertEquals(Set.of("a", "b"), snapshot.items().keySet());
		assertTrue(snapshot.capturable().isEmpty());
		for (String hero : List.of("a", "b")) {
			assertThrows(BusinessException.class, () -> fixture.engine.validateMember(BattleStateCodec.copy(state),
					hero, List.of(new BattleStateOrder(hero, "RETREAT", null)), Map.of(), resources));
			assertThrows(BusinessException.class,
					() -> fixture.engine.validateMember(
							BattleStateCodec.copy(state), hero, List.of(new BattleStateOrder(hero,
									fixture.balls.all().getFirst().code(), hero.equals("a") ? "b" : "a")),
							Map.of(), resources));
		}
	}

	@Test
	void absentOwnerWithOnlyReservesLosesAfterReplacementWindow() {
		var coordinator = new PvpRoundCoordinator(fixture.engine);
		var now = java.time.Instant.parse("2026-10-19T00:00:00Z");
		var battle = battle();
		battle.getUnits().getLast().setHp(0);
		battle.getUnits().add(unit("reserve", true, false, "b", 2));
		var state = coordinator.open(battle, now);
		assertTrue(coordinator.settle(state, now.plusSeconds(60), resources).changed());
		assertEquals("WON", battle.getStatus());
		assertFalse(coordinator.settle(state, now.plusSeconds(120), resources).changed());
	}

	private BattleState battle() {
		var state = new BattleState();
		state.setPvp(true);
		state.setEncounter("PVP");
		state.setSeed(123);
		state.setUnits(new ArrayList<>(List.of(unit("a", false, true, null, 0), unit("b", true, true, null, 0))));
		return state;
	}

	@Test
	void simultaneousWindowLocksOrdersWaitsForBothAndSettlesTimeoutOnce() {
		var coordinator = new PvpRoundCoordinator(fixture.engine);
		var now = java.time.Instant.parse("2026-10-19T00:00:00Z");
		var state = coordinator.open(battle(), now);
		var first = new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "ATTACK", "b")), Map.of());
		coordinator.submit(state, "a", 1, first, now, resources);
		coordinator.submit(state, "a", 1, first, now, resources);
		assertThrows(BusinessException.class,
				() -> coordinator.submit(state, "a", 1,
						new DungeonRoundStateSubmission(List.of(new BattleStateOrder("a", "DEFEND", null)), Map.of()),
						now, resources));
		assertFalse(coordinator.settle(state, now, resources).changed());
		assertTrue(coordinator.settle(state, now.plusSeconds(60), resources).changed());
		assertEquals(1000, state.getBattle().getUnits().getFirst().getHp());
		assertTrue(state.getBattle().getUnits().getLast().getHp() < 1000);
		assertFalse(coordinator.settle(state, now.plusSeconds(60), resources).changed());
		assertEquals(2, state.getBattle().getRound());
		coordinator.surrender(state, "b");
		assertEquals("WON", state.getBattle().getStatus());
	}

	@Test
	void defendingEnemyHeroProtectsOnlyItsOwnActivePartner() {
		var state = battle();
		var hero = state.getUnits().getLast();
		hero.setDefendShield(80);
		var own = unit("own", true, false, "b", 0);
		var reserve = unit("reserve", true, false, "b", 2);
		var other = unit("other", false, false, "a", 0);
		own.setHp(600);
		reserve.setHp(1);
		other.setHp(1);
		state.getUnits().addAll(List.of(own, reserve, other));
		EquipmentSetPolicy.defend(state, hero);
		assertEquals(80, own.getSetShield());
		assertEquals(0, reserve.getSetShield());
		assertEquals(0, other.getSetShield());
	}

	private BattleStateUnit unit(String id, boolean enemy, boolean hero, String owner, int position) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setEnemy(enemy);
		unit.setHero(hero);
		unit.setOwnerHero(owner);
		unit.setPosition(position);
		unit.setLevel(20);
		unit.setHp(1000);
		unit.setMp(100);
		unit.setStats(new BattleStateStats(1000, 100, 100, 100, 40, 40, 50));
		return unit;
	}

}
