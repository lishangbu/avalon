package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 公式及行动边界使用固定输入，随机只作为注入倍率或固定种子，不用重复抽样掩盖误差。
 */
@Tag("rules")
class BattleRulesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	@Test
	void highTierBallsKeepCaptureViableAndUseActualCaptureActions() {
		for (String code : List.of("SEAL_III", "STRONG_SEAL_III", "SEAL_IV", "STRONG_SEAL_IV", "SEAL_V",
				"STRONG_SEAL_V")) {
			var ball = balls.find(code);
			int caught = 0;
			for (int sample = 0; sample < 100; sample++) {
				var hero = unit("hero", "WARRIOR", true, false);
				hero.setLevel(ball.level());
				var enemy = unit("enemy", "STONE_GOLEM", false, true);
				enemy.setLevel(ball.level());
				enemy.setStats(rules.stats("STONE_GOLEM", ball.level(), false, Map.of()));
				enemy.setHp(enemy.getStats().health() / 5);
				assertTrue(rules.capture(enemy, ball.level(), false, ball, 1) >= .5);
				assertEquals(.01, rules.capture(enemy, ball.level(), false, balls.find("SEAL_II"), 1), 1e-12);
				var state = new BattleState();
				state.setEncounter("HIGH_GOLD_DEEP_WILD");
				state.setCaptureAllowed(true);
				state.setSeed(70000L + sample);
				state.getUnits().addAll(List.of(hero, enemy));
				state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "DEFEND", "enemy")));
				var resources = new FakeResources();
				BattleTestRounds.resolve(engine, state, List.of(new BattleStateOrder("hero", code, "enemy")), false,
						resources);
				assertEquals(1, resources.spent);
				if (enemy.isCaptured())
					caught++;
			}
			assertTrue(caught >= 50 && caught < 100, code + " captured=" + caught);
		}
	}

	@Test
	void craftedPotionsRestoreResourcesAndConsumeExactlyOneItemInBattle() {
		for (int tier = 1; tier <= 4; tier++) {
			for (String kind : List.of("HEALTH", "MANA")) {
				var state = new BattleState();
				state.setEncounter("CITY_WILDS");
				var hero = unit("hero", "WARRIOR", true, false);
				hero.setStats(new BattleStateStats(2000, 500, 20, 20, 20, 20, 20));
				hero.setHp(1);
				hero.setMp(0);
				var enemy = unit("enemy", "STONE_GOLEM", false, true);
				state.getUnits().addAll(List.of(hero, enemy));
				state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "DEFEND", "enemy")));
				var resources = new FakeResources();
				BattleTestRounds.resolve(engine, state,
						List.of(new BattleStateOrder("hero", "LIFE_" + kind + "_POTION_" + tier, "hero")), false,
						resources);
				int health = new int[] { 0, 240, 480, 800, 1200 }[tier];
				int mana = new int[] { 0, 48, 96, 160, 240 }[tier];
				assertEquals(kind.equals("HEALTH") ? health + 1 : 1, hero.getHp());
				assertEquals(kind.equals("MANA") ? mana : 0, hero.getMp());
				assertEquals(1, resources.spent);
			}
		}
	}

	@Test
	void minorHealRestoresOneMagicAttackAndPaysItsRealCost() {
		var state = new BattleState();
		state.setEncounter("CITY_WILDS");
		var healer = new BattleStateUnit();
		healer.setId("healer");
		healer.setName("治疗者");
		healer.setHero(true);
		healer.setStats(new BattleStateStats(300, 40, 20, 80, 20, 20, 20));
		healer.setHp(100);
		healer.setMp(40);
		healer.setSkills(List.of("微疗"));
		var enemy = new BattleStateUnit();
		enemy.setId("enemy");
		enemy.setEnemy(true);
		enemy.setStats(new BattleStateStats(100, 0, 10, 10, 10, 10, 10));
		enemy.setHp(100);
		state.getUnits().addAll(List.of(healer, enemy));
		BattleTestRounds.resolve(engine, state, List.of(new BattleStateOrder("healer", "微疗", "healer")), false,
				new FakeResources());
		assertEquals(180, healer.getHp());
		assertEquals(32, healer.getMp());
		assertEquals(3, healer.getReady().get("微疗"));
	}

	@Test
	void supportAbilitiesUseExplicitStateAndSharedCostRules() {
		var caster = unit("caster", "CLAW_CAT", false, false);
		caster.setAbility("节制");
		var heal = catalog.skill("微疗");
		assertEquals(Math.max(1, heal.mana() - 1), engine.manaCost(caster, heal));

		var attacker = unit("attacker", "STONE_GOLEM", false, true);
		attacker.setStats(new BattleStateStats(300, 0, 100, 20, 20, 20, 20));
		var receiver = unit("receiver", "CLAW_CAT", false, false);
		receiver.setStats(new BattleStateStats(300, 40, 20, 20, 20, 20, 20));
		receiver.setHp(300);
		receiver.setRelayGuardUntil(2);
		int guarded = rules.damage(attacker, receiver, catalog.skill("碎岩击"), false, false, 1, 1);
		receiver.setRelayGuardUntil(0);
		int ordinary = rules.damage(attacker, receiver, catalog.skill("碎岩击"), false, false, 1, 1);
		assertEquals((int) Math.floor(ordinary * .75), guarded);
	}

	private final BattleRules rules = fixture.rules;

	private final BattleEngine engine = fixture.engine;

	private final BattleCatalog catalog = fixture.battles;

	private final CaptureBallCatalog balls = fixture.balls;

	@Test
	void spiritClawUsesMagicAttackOnlyForBasicPhysicalAttack() {
		var fox = unit("fox", "SHADOWTAIL_FOX", false, false);
		fox.setLevel(20);
		fox.setElement("暗");
		fox.setStats(new BattleStateStats(1000, 100, 40, 200, 100, 100, 50));
		fox.setHp(1000);
		var target = unit("target", "STONE_GOLEM", false, true);
		target.setElement("地");
		target.setStats(new BattleStateStats(1000, 100, 100, 100, 100, 400, 30));
		target.setHp(1000);
		var attack = engine.skill("ATTACK");
		int physicalSkill = rules.damage(fox, target, catalog.skill("英勇打击"), false, false, 1, 1);
		int magicSkill = rules.damage(fox, target, catalog.skill("水弹"), false, false, 1, 1);
		assertEquals(50, rules.damage(fox, target, attack, false, false, 1, 1));
		fox.setAbility("灵爪");
		assertEquals(250, rules.damage(fox, target, attack, false, false, 1, 1));
		target.setMagicGuard(true);
		assertEquals(250, rules.damage(fox, target, attack, false, false, 1, 1), "仍是物理伤害，不受魔法减伤影响");
		target.setMagicGuard(false);
		assertEquals(physicalSkill, rules.damage(fox, target, catalog.skill("英勇打击"), false, false, 1, 1));
		assertEquals(magicSkill, rules.damage(fox, target, catalog.skill("水弹"), false, false, 1, 1));
		target.setAbility("坚岩");
		assertEquals(225, rules.damage(fox, target, attack, false, false, 1, 1));
		assertEquals("PA", attack.attack());
		assertEquals("PD", attack.defense());
		assertEquals("无", attack.element());
		assertEquals(0, attack.mana());
		assertEquals(50, attack.power());
	}

	@Test
	void petStatsUseTargetSpeciesAndCannotInheritHeroEquipment() {
		assertEquals(new BattleStateStats(433, 114, 64, 80, 68, 80, 48), rules.stats("CLAW_CAT", 12, false, Map.of()));
		assertEquals(new BattleStateStats(555, 127, 64, 98, 76, 90, 49),
				rules.stats("STREAM_CLAW_CAT", 12, false, Map.of("health", 500, "mana", 500)));
		assertEquals(new BattleStateStats(480, 160, 57, 111, 71, 95, 60),
				rules.stats("SHADOWTAIL_FOX", 15, false, Map.of()));
	}

	@Test
	void secondTierUsesConfiguredMultiplierAndOnlyLiveSlowHelpsCapture() {
		var stone = unit("stone", "STONE_GOLEM", false, true);
		stone.setLevel(15);
		stone.setHp(150);
		assertEquals(.575, rules.capture(stone, 15, false, balls.find("SEAL_II"), 1), 1e-12);
		assertEquals(.75, rules.capture(stone, 15, false, balls.find("STRONG_SEAL_II"), 1), 1e-12);
		assertEquals(.01, rules.capture(stone, 15, false, balls.find("SEAL_I"), 1), 1e-12);
		stone.setSlowUntil(2);
		assertEquals(.6325, rules.capture(stone, 15, false, balls.find("SEAL_II"), 2), 1e-12);
		assertEquals(.575, rules.capture(stone, 15, false, balls.find("SEAL_II"), 3), 1e-12);
		assertEquals(.2875, rules.capture(stone, 10, false, balls.find("SEAL_II"), 3), 1e-12);
		assertEquals(.575, rules.capture(stone, 15, true, balls.find("SEAL_II"), 3), 1e-12, "教学与野外使用相同概率");

		var state = new BattleState();
		state.setEncounter("VILLAGE");
		state.setCaptureAllowed(true);
		state.setSeed(10);
		state.getUnits().addAll(List.of(unit("hero", "WARRIOR", true, false), stone));
		state.setEnemyOrders(List.of(new BattleStateOrder("stone", "DEFEND", "stone")));
		var resources = new FakeResources();
		BattleTestRounds.resolve(engine, state, List.of(new BattleStateOrder("hero", "SEAL_II", "stone")), false,
				resources);
		assertEquals(1, resources.spent, "II 档必须走真实捕捉行动并扣球");
	}

	BattleStateUnit unit(String id, String species, boolean hero, boolean enemy) {
		var u = new BattleStateUnit();
		u.setId(id);
		u.setName(id);
		u.setSpecies(species);
		u.setHero(hero);
		u.setEnemy(enemy);
		u.setLevel(3);
		u.setStats(rules.stats(species, 3, hero, Map.of()));
		u.setHp(u.getStats().health());
		u.setMp(u.getStats().mana());
		u.setSkills(catalog.growth(species).skills());
		return u;
	}

	@Test
	void captureUsesExactHealthBoundariesWithoutTutorialGuarantee() {
		var stone = unit("stone", "STONE_GOLEM", false, true);
		// 本例专测75%、50%、25%的离散边界，用固定300生命排除物种成长调整影响。
		stone.setStats(new BattleStateStats(300, 40, 35, 35, 35, 35, 26));
		stone.setHp(225);
		assertEquals(.375, rules.capture(stone, 3, false, false));
		stone.setHp(226);
		assertEquals(.25, rules.capture(stone, 3, false, false));
		stone.setHp(150);
		assertEquals(.5, rules.capture(stone, 3, false, false));
		assertEquals(.5, rules.capture(stone, 3, true, false));
		assertEquals(.625, rules.capture(stone, 3, true, true));
		stone.setHp(75);
		assertEquals(.5, rules.capture(stone, 3, false, false));
		stone.setHp(74);
		assertEquals(.75, rules.capture(stone, 3, false, false));
		assertEquals(.85, rules.capture(stone, 3, false, true));
		stone.setLevel(15);
		assertEquals(.025, rules.capture(stone, 3, false, false), 1e-12);
	}

	@Test
	void neutralDamageRandomAndEquipmentBudgetMatchDesign() {
		var a = unit("hero", "WARRIOR", true, false);
		var b = unit("enemy", "STONE_GOLEM", false, true);
		a.setStats(new BattleStateStats(240, 39, 35, 35, 35, 35, 26));
		b.setStats(a.getStats());
		assertEquals(60, rules.damage(a, b, catalog.skill("英勇打击"), false, false, .9375, 1));
		assertEquals(68, rules.damage(a, b, catalog.skill("英勇打击"), false, false, 1.0625, 1));
		assertEquals(44, rules.damage(a, b, catalog.skill("英勇打击"), true, false, 1, 1));
		b.setAbility("坚岩");
		assertEquals(57, rules.damage(a, b, catalog.skill("英勇打击"), false, false, 1, 1));
		var naked = rules.stats("WARRIOR", 3, true, Map.of());
		var equipped = rules.stats("WARRIOR", 3, true,
				Map.of("health", 40, "physicalAttack", 8, "physicalDefense", 6, "magicDefense", 4, "speed", 2));
		assertEquals(288, equipped.health());
		assertEquals(40, equipped.health() - naked.health());
		assertEquals(38, equipped.physicalAttack());
		assertEquals(42, equipped.physicalDefense());
	}

	@Test
	void everyElementHasNeutralAverageAttackAndDefense() {
		var elements = List.of("水", "火", "风", "地", "雷", "木", "光", "暗");
		for (var element : elements) {
			assertEquals(8, elements.stream().mapToDouble(target -> catalog.element(element, target)).sum());
			assertEquals(8, elements.stream().mapToDouble(attack -> catalog.element(attack, element)).sum());
		}
	}

	@Test
	void deadTargetConsumesNoBallAndReservesCanReplaceWithoutLosingTheirAction() {
		var state = new BattleState();
		state.setEncounter("TUTORIAL");
		state.setCaptureAllowed(true);
		state.setSeed(10);
		var hero = unit("hero", "WARRIOR", true, false);
		var cat = unit("cat", "CLAW_CAT", false, false);
		var enemy = unit("enemy", "STONE_GOLEM", false, true);
		enemy.setHp(1);
		state.getUnits().addAll(List.of(hero, cat, enemy));
		engine.planEnemies(state);
		var resources = new FakeResources();
		BattleTestRounds.resolve(engine, state, List.of(new BattleStateOrder("hero", "SEAL_I", "enemy"),
				new BattleStateOrder("cat", "ATTACK", "enemy")), false, resources);
		assertEquals(0, resources.spent);
		assertEquals("WON", state.getStatus());

		var replacementState = new BattleState();
		replacementState.setEncounter("VILLAGE");
		hero.setHp(0);
		cat.setHp(0);
		enemy.setHp(200);
		var reserve = unit("reserve", "CLAW_CAT", false, false);
		reserve.setPosition(2);
		replacementState.getUnits().addAll(List.of(hero, cat, reserve, enemy));
		engine.planEnemies(replacementState);
		BattleTestRounds.resolve(engine, replacementState, List.of(new BattleStateOrder("reserve", "水弹", "enemy")),
				false, resources, Map.of("reserve", 0));
		assertEquals(0, reserve.getPosition());
		assertEquals(2, cat.getPosition());
		assertEquals(reserve.getStats().mana() - 12, reserve.getMp());
		assertTrue(enemy.getHp() < 200);
	}

	@Test
	void lightScreenProtectsOnlyMagicAndExpiresAtRoundEnd() {
		var caster = unit("caster", "CLAW_CAT", false, false);
		var enemy = unit("enemy", "THUNDER_LIZARD", false, true);
		var magic = catalog.skill("火球术");
		var physical = catalog.skill("碎岩击");
		int plainMagic = rules.damage(enemy, caster, magic, false, false, 1, 1);
		int plainPhysical = rules.damage(enemy, caster, physical, false, false, 1, 1);
		caster.setMagicGuard(true);
		assertTrue(rules.damage(enemy, caster, magic, false, false, 1, 1) < plainMagic);
		assertEquals(plainPhysical, rules.damage(enemy, caster, physical, false, false, 1, 1));
		caster.setMagicGuard(false);
		caster.setSkills(List.of("光幕"));
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		state.getUnits().addAll(List.of(caster, enemy));
		state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "DEFEND", "enemy")));
		BattleTestRounds.resolve(engine, state, List.of(new BattleStateOrder("caster", "光幕", "caster")), false,
				new FakeResources());
		assertEquals(caster.getStats().mana() - 6, caster.getMp());
		assertTrue(!caster.isMagicGuard(), "回合结束必须移除光幕，不能带入下一回合");
		assertTrue(!engine.available(caster, catalog.skill("光幕"), state.getRound()), "冷却一完整回合");
	}

	@Test
	void crystalDefendTriggersInRealRoundAndExpiresBeforeNextRound() {
		var state = new BattleState();
		state.setEncounter("VILLAGE");
		var hero = unit("hero", "WARRIOR", true, false);
		hero.setDefendShield(30);
		var pet = unit("pet", "CLAW_CAT", false, false);
		pet.setOwnerHero("hero");
		var enemy = unit("enemy", "STONE_GOLEM", false, true);
		state.getUnits().addAll(List.of(hero, pet, enemy));
		state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "DEFEND", "enemy")));
		BattleTestRounds.resolve(engine, state,
				List.of(new BattleStateOrder("hero", "DEFEND", "hero"), new BattleStateOrder("pet", "DEFEND", "pet")),
				false, new FakeResources());
		assertTrue(state.getLog().stream().anyMatch(line -> line.contains("获得套装护盾")));
		assertEquals(0, pet.getSetShield(), "未消耗的护盾也在轮末到期");
		var reserve = unit("reserve", "CLAW_CAT", false, false);
		reserve.setOwnerHero("hero");
		reserve.setPosition(2);
		state.getUnits().add(reserve);
		pet.setSetShield(30);
		state.setEnemyOrders(List.of(new BattleStateOrder("enemy", "DEFEND", "enemy")));
		BattleTestRounds.resolve(engine, state,
				List.of(new BattleStateOrder("hero", "DEFEND", "hero"), new BattleStateOrder("pet", "SWAP", "reserve")),
				false, new FakeResources());
		assertEquals(2, pet.getPosition());
		assertEquals(0, pet.getSetShield());
		assertEquals(0, reserve.getSetShield());
	}

	static class FakeResources implements BattleEngineResources {

		int spent;

		public int count(BattleStateUnit actor, String code) {
			return 3;
		}

		public boolean room(BattleStateUnit target) {
			return true;
		}

		public void take(BattleStateUnit actor, String code) {
			spent++;
		}

		public void capture(BattleStateUnit target) {
		}

	}

}
