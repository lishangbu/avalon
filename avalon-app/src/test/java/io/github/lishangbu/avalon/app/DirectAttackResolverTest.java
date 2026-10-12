package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolverProtection;
import io.github.lishangbu.avalon.battle.domain.EquipmentSetPolicy;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 群体攻击与保护的交互测试，覆盖单体掩护跳过及保护者自身、代接两份伤害。 */
@Tag("rules")
class DirectAttackResolverTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DirectAttackResolver attacks = fixture.attacks;

	@Test
	void areaSkipsCoverButInterventionTakesTwoHitsEvenWhenProtectorFalls() {
		var state = new BattleState();
		var boss = unit("boss", 500);
		boss.setEnemy(true);
		var warrior = unit("warrior", 30);
		var stone = unit("stone", 200);
		var cat = unit("cat", 200);
		state.setUnits(List.of(boss, warrior, stone, cat));
		var protections = new HashMap<String, List<DirectAttackResolverProtection>>();
		protections.put(cat.getId(), new ArrayList<>(List.of(new DirectAttackResolverProtection(stone.getId(), false),
				new DirectAttackResolverProtection(warrior.getId(), true))));
		var quake = new BattleSkill("震击", "DAMAGE", "无", "MA", "MD", 70, 0, 0, 0, "ENEMY");
		attacks.apply(state, boss, List.of(warrior, stone, cat), quake, true, 1, Set.of(), protections);
		assertEquals(0, warrior.getHp());
		assertEquals(123, stone.getHp());
		assertEquals(200, cat.getHp(), "保护者倒下不返还同一次攻击的代接伤害");
		assertEquals(1, protections.get(cat.getId()).size(), "不适用的掩护不应被消耗");
	}

	private BattleStateUnit unit(String id, int hp) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setLevel(6);
		unit.setHp(hp);
		unit.setStats(new BattleStateStats(500, 0, 50, 50, 50, 50, 20));
		return unit;
	}

	@Test
	void setShieldSelectsOwnLowestRatioAndAbsorbsBeforeManaWithoutBlockingSlow() {
		var state = new BattleState();
		var hero = unit("hero", 500);
		hero.setHero(true);
		hero.setDefendShield(30);
		var pet = unit("pet", 200);
		pet.setOwnerHero("hero");
		pet.setPosition(1);
		pet.setMp(20);
		pet.setShield(100);
		var first = unit("first", 100);
		first.setStats(new BattleStateStats(250, 0, 50, 50, 50, 50, 20));
		first.setOwnerHero("hero");
		var other = unit("other", 1);
		other.setOwnerHero("other-hero");
		var reserve = unit("reserve", 1);
		reserve.setOwnerHero("hero");
		reserve.setPosition(2);
		var boss = unit("boss", 500);
		boss.setEnemy(true);
		state.setUnits(List.of(hero, pet, first, other, reserve, boss));
		EquipmentSetPolicy.defend(state, hero);
		assertEquals(30, first.getSetShield(), "同生命比例按出战位置选择");
		assertEquals(0, other.getSetShield());
		assertEquals(0, reserve.getSetShield());
		first.setHp(250);
		EquipmentSetPolicy.defend(state, hero);
		EquipmentSetPolicy.defend(state, hero);
		assertEquals(30, pet.getSetShield(), "重复触发不会叠加容量");
		var slow = new BattleSkill("减速攻击", "SLOW", "无", "MA", "MD", 70, 0, 0, 0, "ENEMY");
		attacks.apply(state, boss, List.of(pet), slow, false, 1, Set.of(), new HashMap<>());
		assertEquals(200, pet.getHp());
		assertEquals(0, pet.getSetShield());
		assertEquals(53, pet.getShield());
		assertEquals(10, pet.getMp());
		assertEquals(state.getRound() + 2, pet.getSlowUntil());
	}

	@Test
	void protectionConsumesOnlyActualRecipientsWardAndDeathClearsIt() {
		var state = new BattleState();
		var boss = unit("boss", 500);
		boss.setEnemy(true);
		var pet = unit("pet", 200);
		pet.setSetShield(30);
		var protector = unit("protector", 200);
		protector.setSetShield(10);
		state.setUnits(List.of(boss, pet, protector));
		var attack = new BattleSkill("攻击", "DAMAGE", "无", "MA", "MD", 70, 0, 0, 0, "ENEMY");
		var protections = new HashMap<String, List<DirectAttackResolverProtection>>();
		protections.put("pet", new ArrayList<>(List.of(new DirectAttackResolverProtection("protector", false))));
		attacks.apply(state, boss, List.of(pet), attack, false, 1, Set.of(), protections);
		assertEquals(200, pet.getHp());
		assertEquals(30, pet.getSetShield(), "原目标护盾不随援护转移");
		assertEquals(0, protector.getSetShield());
		assertTrue(protector.getHp() < 200);
		pet.setHp(1);
		attacks.apply(state, boss, List.of(pet), attack, false, 1, Set.of(), new HashMap<>());
		assertEquals(0, pet.getHp());
		assertEquals(0, pet.getSetShield());
	}

}
