package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.TrainingPolicy;
import io.github.lishangbu.avalon.content.battle.CaptureBallCatalog;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 捕捉课程直接调用正式概率与回合引擎，不用保证成功的教学球分支。 */
@Tag("rules")
class AcademyCaptureRulesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final TrainingPolicy policy = fixture.training;

	private final BattleEngine engine = fixture.engine;

	private final BattleRules rules = fixture.rules;

	private final CaptureBallCatalog balls = fixture.balls;

	private BattleEngineResources resources(BattleState state) {
		return new BattleEngineResources() {
			public int count(BattleStateUnit actor, String code) {
				return state.getTraining().getSupplies().getOrDefault(code, 0);
			}

			public boolean room(BattleStateUnit target) {
				return state.getUnits().stream().noneMatch(BattleStateUnit::isCaptured);
			}

			public void take(BattleStateUnit actor, String code) {
				assertTrue(count(actor, code) > 0);
				state.getTraining().getSupplies().put(code, count(actor, code) - 1);
			}

			public void capture(BattleStateUnit target) {
				assertTrue(target.getHp() > 0);
			}
		};
	}

	@Test
	void generatesOnceAndUsesRealTierPenaltyWithoutGuarantee() {
		var state = policy.createAcademy("ACADEMY_CAPTURE", "MAGE", List.of(), 71L);
		var target = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
		assertEquals(11, target.getLevel());
		String slot = target.getAbilitySlot();
		target.setHp(target.getStats().health() / 2);
		assertEquals(.4, rules.capture(target, 11, false, balls.find("SEAL_I"), 1), .00001);
		assertEquals(.575, rules.capture(target, 11, false, balls.find("SEAL_II"), 1), .00001);
		engine.planEnemies(state);
		assertEquals(slot, target.getAbilitySlot());
		assertEquals(slot,
				policy.createAcademy("ACADEMY_CAPTURE", "MAGE", List.of(), 71L)
					.getUnits()
					.stream()
					.filter(BattleStateUnit::isEnemy)
					.findFirst()
					.orElseThrow()
					.getAbilitySlot());
	}

	@Test
	void eitherBallCanPassAfterRealDamageAndNeverActsAfterCaptured() {
		for (String ball : List.of("SEAL_I", "SEAL_II")) {
			boolean passed = false;
			// 穷举有限种子验证合法成功分支；每次都走正式随机概率，不改捕捉随机数或锁血。
			for (long seed = 0; seed < 40 && !passed; seed++) {
				var state = policy.createAcademy("ACADEMY_CAPTURE", "MAGE", List.of(), seed);
				state.getTraining().setCaptureInfoViewed(true);
				var resources = resources(state);
				BattleTestRounds.resolve(engine, state,
						List.of(new BattleStateOrder("trainee", "DEFEND", "trainee"),
								new BattleStateOrder("T_CAT", "水弹", "academy-target"),
								new BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
						false, resources);
				var target = state.getUnits().stream().filter(BattleStateUnit::isEnemy).findFirst().orElseThrow();
				assertTrue(target.getHp() > 0 && target.getHp() < target.getStats().health());
				int enemyMp = target.getMp();
				BattleTestRounds.resolve(engine, state,
						List.of(new BattleStateOrder("trainee", ball, "academy-target"),
								new BattleStateOrder("T_CAT", "DEFEND", "T_CAT"),
								new BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
						false, resources);
				assertEquals(7, resources
					.count(state.getUnits().stream().filter(BattleStateUnit::isHero).findFirst().orElseThrow(), ball));
				if (target.isCaptured()) {
					assertEquals("WON", state.getStatus());
					assertEquals(enemyMp, target.getMp());
					assertTrue(state.getTraining().isCapturedAfterDamage());
					passed = true;
				}
			}
			assertTrue(passed, ball);
		}
	}

	@Test
	void killedProjectionIsNotCapturedAndFreshSessionRestoresTemporaryInventory() {
		var state = policy.createAcademy("ACADEMY_CAPTURE", "WARRIOR", List.of(), 3L);
		state.getUnits().stream().filter(BattleStateUnit::isEnemy).forEach(u -> u.setHp(1));
		BattleTestRounds.resolve(engine, state,
				List.of(new BattleStateOrder("trainee", "ATTACK", "academy-target"),
						new BattleStateOrder("T_CAT", "ATTACK", "academy-target"),
						new BattleStateOrder("T_STONE", "ATTACK", "academy-target")),
				false, resources(state));
		assertEquals("LOST", state.getStatus());
		assertFalse(state.getUnits().stream().anyMatch(BattleStateUnit::isCaptured));
		state.getTraining().getSupplies().put("SEAL_I", 0);
		var fresh = policy.createAcademy("ACADEMY_CAPTURE", "WARRIOR", List.of(), 4L);
		assertEquals(8, fresh.getTraining().getSupplies().get("SEAL_I"));
		assertFalse(fresh.getTraining().isCaptureInfoViewed());
	}

	@Test
	void legalFullHealthCaptureDoesNotPretendWeakeningWasPracticed() {
		boolean observed = false;
		for (long seed = 0; seed < 80 && !observed; seed++) {
			var state = policy.createAcademy("ACADEMY_CAPTURE", "MAGE", List.of(), seed);
			state.getTraining().setCaptureInfoViewed(true);
			BattleTestRounds.resolve(engine, state,
					List.of(new BattleStateOrder("trainee", "SEAL_II", "academy-target"),
							new BattleStateOrder("T_CAT", "DEFEND", "T_CAT"),
							new BattleStateOrder("T_STONE", "DEFEND", "T_STONE")),
					false, resources(state));
			if (state.getUnits().stream().anyMatch(BattleStateUnit::isCaptured)) {
				assertEquals("LOST", state.getStatus());
				assertFalse(state.getTraining().isCapturedAfterDamage());
				assertTrue(state.getLog().stream().anyMatch(s -> s.contains("捕获成功")));
				observed = true;
			}
		}
		assertTrue(observed);
	}

}
