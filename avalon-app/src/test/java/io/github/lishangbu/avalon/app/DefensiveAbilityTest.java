package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.model.BattleSkillEffect;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DirectAttackResolver;
import io.github.lishangbu.avalon.content.battle.model.BattleSkill;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("rules")
class DefensiveAbilityTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final DirectAttackResolver attacks = fixture.attacks;

	@ParameterizedTest
	@CsvSource({ "储水,水,175", "蓄电,雷,175", "引火,火,100", "食草,木,100", "飘浮,地,100" })
	void immunityPrecedesShieldsSlowAndConsumableProtection(String ability, String element, int health) {
		var state = new BattleState();
		var enemy = unit("enemy", true, 300);
		var pet = unit("pet", false, 100);
		pet.setAbility(ability);
		pet.setShield(100);
		pet.setMp(20);
		pet.setRelayGuardUntil(2);
		state.setUnits(List.of(enemy, pet));
		var skill = new BattleSkill("免疫检查", List.of(new BattleSkillEffect("SLOW", 100)), element, "MA", "MD", 0, 0, 0,
				"ENEMY", null, null);
		attacks.apply(state, enemy, List.of(pet), skill, false, 1, Set.of(), new HashMap<>());
		assertEquals(health, pet.getHp());
		assertEquals(100, pet.getShield());
		assertEquals(20, pet.getMp());
		assertEquals(2, pet.getRelayGuardUntil());
		assertEquals(0, pet.getSlowUntil());
		assertEquals("引火".equals(ability), pet.isFireAbsorbed());
		assertEquals("食草".equals(ability), pet.isWoodAbsorbed());
	}

	@Test
	void normalHitClearsPreviouslyObservedTemporaryImmunity() {
		var state = new BattleState();
		var enemy = unit("enemy", true, 300);
		var pet = unit("pet", false, 300);
		pet.setAbility("复制");
		pet.setCopiedAbility("储水");
		state.setUnits(List.of(enemy, pet));
		var skill = new BattleSkill("水弹", List.of(new BattleSkillEffect("DAMAGE", 50)), "水", "MA", "MD", 0, 0, 0,
				"ENEMY", null, null);
		attacks.apply(state, enemy, List.of(pet), skill, false, 1, Set.of(), new HashMap<>());
		assertEquals(List.of("水"), pet.getObservedImmunities());
		pet.setCopiedAbility("节制");
		attacks.apply(state, enemy, List.of(pet), skill, false, 1, Set.of(), new HashMap<>());
		assertEquals(List.of(), pet.getObservedImmunities());
		assertEquals(250, pet.getHp());
	}

	@Test
	void sturdyStopsLethalDamageAtOneButNotTheFollowingHit() {
		var state = new BattleState();
		var enemy = unit("enemy", true, 300);
		var pet = unit("pet", false, 300);
		pet.setAbility("结实");
		state.setUnits(List.of(enemy, pet));
		var skill = new BattleSkill("重击", List.of(new BattleSkillEffect("DAMAGE", 1000)), "无", "PA", "PD", 0, 0, 0,
				"ENEMY", null, null);
		attacks.apply(state, enemy, List.of(pet), skill, false, 1, Set.of(), new HashMap<>());
		assertEquals(1, pet.getHp());
		attacks.apply(state, enemy, List.of(pet), skill, false, 1, Set.of(), new HashMap<>());
		assertEquals(0, pet.getHp());
	}

	@Test
	void friendGuardSnapshotSurvivesSourceDeathButNeverProtectsAnotherParty() {
		var state = new BattleState();
		var enemy = unit("enemy", true, 300);
		var source = unit("source", false, 1);
		source.setAbility("友情防守");
		var pet = unit("pet", false, 300);
		var other = unit("other", false, 300);
		other.setOwnerHero("other-owner");
		state.setUnits(List.of(enemy, source, pet, other));
		var skill = new BattleSkill("群攻", List.of(new BattleSkillEffect("AREA", 100)), "无", "PA", "PD", 0, 0, 0,
				"ENEMY", null, null);
		attacks.apply(state, enemy, List.of(source, pet, other), skill, true, 1, Set.of(), new HashMap<>());
		assertEquals(0, source.getHp());
		assertEquals(210, pet.getHp());
		assertEquals(200, other.getHp());
	}

	private BattleStateUnit unit(String id, boolean enemy, int health) {
		var unit = new BattleStateUnit();
		unit.setId(id);
		unit.setName(id);
		unit.setOwnerHero("owner");
		unit.setEnemy(enemy);
		unit.setLevel(5);
		unit.setStats(new BattleStateStats(300, 40, 100, 100, 100, 100, 20));
		unit.setHp(health);
		return unit;
	}

}
