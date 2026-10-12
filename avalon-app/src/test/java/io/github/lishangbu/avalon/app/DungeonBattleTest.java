package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 使用真实回合引擎验证副本机制；不以直接调用机制方法代替速度、选招和胜负结算。 */
@Tag("rules")
class DungeonBattleTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DungeonBattlePolicy dungeon = fixture.dungeonPolicy;

	private final BattleEngine engine = fixture.engine;

	private final BattleRules rules = fixture.rules;

	private final ObjectMapper json = new ObjectMapper();

	private static final BattleEngineResources NO_ITEMS = new BattleEngineResources() {
		public int count(String code) {
			return 1;
		}

		public boolean room(BattleStateUnit target) {
			return true;
		}

		public void take(String code) {
			fail("机制测试不得消耗物品");
		}

		public void capture(BattleStateUnit target) {
			fail("副本目标不能捕捉");
		}
	};

	private BattleStateUnit hero(String id) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setHero(true);
		unit.setLevel(15);
		unit.setStats(new BattleStateStats(5000, 100, 200, 200, 100, 100, 50));
		unit.setHp(5000);
		unit.setMp(100);
		return unit;
	}

	private BattleState stage(String id) {
		var state = dungeon.create(id, List.of(hero("a"), hero("b"), hero("c")), 8301);
		engine.planEnemies(state);
		return state;
	}

	private void defend(BattleState state) {
		engine.resolve(state,
				state.getUnits()
					.stream()
					.filter(unit -> !unit.isEnemy() && unit.active() && unit.alive())
					.map(unit -> new BattleStateOrder(unit.getId(), "DEFEND", unit.getId()))
					.toList(),
				false, NO_ITEMS);
	}

	@Test
	void namedPetTelegraphIncludesItsOwnerWhenCompanionsShareNames() {
		var first = hero("first");
		first.setName("晨风");
		first.setHp(0);
		var second = hero("second");
		second.setName("晴川");
		second.setHp(0);
		var petA = hero("pet-a");
		petA.setHero(false);
		petA.setName("利爪猫");
		petA.setOwnerHero(first.getId());
		var petB = hero("pet-b");
		petB.setHero(false);
		petB.setName("利爪猫");
		petB.setOwnerHero(second.getId());
		var state = dungeon.create("D01_3", List.of(first, second, petA, petB), 8301);
		engine.planEnemies(state);
		String expected = state.getEnemyOrders().getFirst().target().equals("pet-a") ? "晨风" : "晴川";
		assertTrue(state.getTelegraph().contains("目标：" + expected + "的利爪猫。"));
	}

	@Test
	void keeperNamesAHeroAndDefendingReducesItsHeavyStrike() throws Exception {
		var state = stage("D01_2");
		String target = state.getEnemyOrders().getFirst().target();
		assertTrue(state.getTelegraph().contains(target));
		var defended = json.readValue(json.writeValueAsString(state), BattleState.class);
		defend(defended);
		engine.resolve(state,
				List.of(new BattleStateOrder("a", "ATTACK", "dungeon-boss"),
						new BattleStateOrder("b", "ATTACK", "dungeon-boss"),
						new BattleStateOrder("c", "ATTACK", "dungeon-boss")),
				false, NO_ITEMS);
		assertTrue(state.getUnits()
			.stream()
			.filter(u -> u.getId().equals(target))
			.findFirst()
			.orElseThrow()
			.getHp() < defended.getUnits()
				.stream()
				.filter(u -> u.getId().equals(target))
				.findFirst()
				.orElseThrow()
				.getHp());
		assertFalse(state.getUnits().stream().anyMatch(BattleStateUnit::isMechanism));
	}

	@Test
	void pounceHitsOnlyTheAnnouncedIndividual() {
		var state = stage("D01_3");
		String target = state.getEnemyOrders().getFirst().target();
		defend(state);
		assertEquals(1, state.getUnits().stream().filter(u -> !u.isEnemy() && u.getHp() < 5000).count());
		assertTrue(state.getUnits()
			.stream()
			.filter(u -> u.getId().equals(target))
			.findFirst()
			.orElseThrow()
			.getHp() < 5000);
		assertFalse(state.getUnits().stream().anyMatch(BattleStateUnit::isMechanism));
	}

	@Test
	void suppressingGuardPreventsEmpoweredAttackAndBossDefeatEndsFight() throws Exception {
		var intact = stage("D01_4");
		defend(intact);
		var suppressed = json.readValue(json.writeValueAsString(intact), BattleState.class);
		suppressed.getUnits().stream().filter(BattleStateUnit::isMechanism).forEach(u -> u.setHp(0));
		defend(intact);
		defend(suppressed);
		assertTrue(intact.getUnits()
			.stream()
			.filter(u -> !u.isEnemy())
			.mapToInt(BattleStateUnit::getHp)
			.sum() < suppressed.getUnits().stream().filter(u -> !u.isEnemy()).mapToInt(BattleStateUnit::getHp).sum());
		assertTrue(suppressed.getLog().stream().anyMatch(l -> l.contains("未能发动")));
		intact.getUnits().stream().filter(u -> u.getId().equals("dungeon-boss")).forEach(u -> u.setHp(1));
		engine.resolve(
				intact, List.of(new BattleStateOrder("a", "ATTACK", "dungeon-boss"),
						new BattleStateOrder("b", "DEFEND", "b"), new BattleStateOrder("c", "DEFEND", "c")),
				false, NO_ITEMS);
		assertEquals("WON", intact.getStatus());
		assertFalse(intact.getUnits().stream().anyMatch(BattleStateUnit::isMechanism));
	}

	@Test
	void openingGroupKeepsSpeciesStatsExceptHealthAndReplayIsDeterministic() throws Exception {
		var state = stage("D01_1");
		var enemies = state.getUnits().stream().filter(BattleStateUnit::isEnemy).toList();
		assertEquals(3, enemies.size());
		assertEquals(3, state.getEnemyOrders().size());
		for (var enemy : enemies) {
			var base = rules.stats(enemy.getSpecies(), 10, false, Map.of());
			assertEquals((int) Math.floor(base.health() * 1.5), enemy.getHp());
			assertEquals(base.physicalAttack(), enemy.getStats().physicalAttack());
			assertFalse(enemy.isMechanism());
		}
		var restored = json.readValue(json.writeValueAsString(state), BattleState.class);
		defend(state);
		defend(restored);
		assertEquals(json.writeValueAsString(state), json.writeValueAsString(restored));
	}

}
