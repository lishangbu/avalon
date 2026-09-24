package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.battle.domain.BattleState.Stats;
import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("rules")
class OffensiveAbilityTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DirectAttackResolver attacks = fixture.attacks;

	private final BattleEngine engine = fixture.engine;

	private final BattleCatalog catalog = fixture.battles;

	@Test
	void lightningRedirectIsOwnPartySingleTargetOncePerRound() {
		var state = new BattleState();
		var enemy = unit("enemy", true);
		var target = unit("target", false);
		target.setHero(true);
		var pet = unit("pet", false);
		pet.setOwnerHero("target");
		pet.setAbility("引雷");
		state.setUnits(List.of(enemy, target, pet));
		hit(state, enemy, target, "雷牙");
		assertEquals(1000, target.getHp());
		assertTrue(pet.getHp() < 1000);
		int after = pet.getHp();
		hit(state, enemy, target, "雷牙");
		assertTrue(target.getHp() < 1000);
		assertEquals(after, pet.getHp());
		state.setRound(2);
		pet.setOwnerHero("another-owner");
		hit(state, enemy, target, "雷牙");
		assertEquals(after, pet.getHp());
	}

	@Test
	void armorBreakIsConsumedByOwnPartyAndOnlyAfterFirstHit() {
		var state = new BattleState();
		var attacker = unit("attacker", false);
		attacker.setAbility("破甲");
		var enemy = unit("enemy", true);
		state.setUnits(List.of(attacker, enemy));
		hit(state, attacker, enemy, "碎岩击");
		assertEquals(920, enemy.getHp());
		assertEquals(2, enemy.getArmorBreaks().get("owner"));
		attacker.setAbility(null);
		hit(state, attacker, enemy, "碎岩击");
		assertEquals(832, enemy.getHp());
		assertFalse(enemy.getArmorBreaks().containsKey("owner"));
	}

	@Test
	void thunderChargePaysZeroExactlyTwiceAndDoesNotRequireRemainingMana() {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		var pet = unit("pet", false);
		pet.setAbility("雷能充蓄");
		pet.setSkills(List.of("雷牙"));
		var enemy = unit("enemy", true);
		state.setUnits(List.of(pet, enemy));
		for (int round = 1; round <= 2; round++) {
			state.setRound(round);
			pet.getReady().clear();
			hit(state, enemy, pet, "雷牙");
			assertTrue(pet.isThunderCharge());
			state.setEnemyOrders(List.of());
			engine.resolve(state, List.of(new Order("pet", "雷牙", "enemy")), false, new BattleRulesTest.FakeResources());
			assertEquals(0, pet.getMp());
			assertEquals(round, pet.getFreeThunderCasts());
		}
		hit(state, enemy, pet, "雷牙");
		assertFalse(pet.isThunderCharge());
		assertFalse(engine.available(pet, catalog.skill("雷牙"), state.getRound()));
	}

	@Test
	void surrenderControlModifiesOnlySlowAttackAndPreservesSharedTemplate() {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		var pet = unit("pet", false);
		pet.setAbility("舍控强攻");
		pet.setSkills(List.of("逆风"));
		pet.setMp(catalog.skill("逆风").mana() - 2);
		var enemy = unit("enemy", true);
		state.setUnits(List.of(pet, enemy));
		engine.resolve(state, List.of(new Order("pet", "逆风", "enemy")), false, new BattleRulesTest.FakeResources());
		assertEquals(0, pet.getMp());
		assertEquals(0, enemy.getSlowUntil());
		assertEquals("SLOW", catalog.skill("逆风").effect());
	}

	private void hit(BattleState state, Unit actor, Unit target, String skill) {
		attacks.apply(state, actor, List.of(target), catalog.skill(skill), false, 1, Set.of(), new HashMap<>());
	}

	private Unit unit(String id, boolean enemy) {
		var unit = new Unit();
		unit.setId(id);
		unit.setName(id);
		unit.setOwnerHero("owner");
		unit.setEnemy(enemy);
		unit.setLevel(5);
		unit.setElement("无");
		unit.setHp(1000);
		unit.setStats(new Stats(1000, 40, 100, 100, 100, 100, 30));
		return unit;
	}

}
