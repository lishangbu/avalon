package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.battle.domain.BattleState.Unit;
import io.github.lishangbu.avalon.battle.domain.TrainingPolicy;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/** 借用课程走正式回合引擎，验证真实通过动作、失败重试及第五只不能越名单。 */
@Tag("rules")
class AcademyTrainingRulesTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final TrainingPolicy policy = fixture.training;

	private final BattleEngine engine = fixture.engine;

	private static final BattleEngine.Resources NO_ITEMS = new BattleEngine.Resources() {
		public int count(String code) {
			return 0;
		}

		public boolean room(Unit target) {
			return false;
		}

		public void take(String code) {
			fail("训练不应访问真实道具");
		}

		public void capture(Unit target) {
			fail("本课不允许捕捉");
		}
	};

	@Test
	void allProfessionsDefendRealTelegraphedHitAndFinish() {
		for (String profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			var state = policy.createAcademy("BQ018", profession, List.of(), 71L);
			assertEquals(3, state.getUnits().stream().filter(u -> !u.isEnemy()).count());
			for (int i = 0; i < 20 && state.getStatus().equals("ACTIVE"); i++) {
				boolean defend = state.getRound() == 2;
				var orders = state.getUnits()
					.stream()
					.filter(u -> !u.isEnemy() && u.active() && u.alive())
					.map(u -> new Order(u.getId(), defend && u.isHero() ? "DEFEND" : "ATTACK",
							defend && u.isHero() ? u.getId() : "academy-target"))
					.toList();
				engine.resolve(state, orders, false, NO_ITEMS);
				if (state.getStatus().equals("ACTIVE"))
					engine.planEnemies(state);
			}
			assertEquals("WON", state.getStatus(), profession);
			assertTrue(state.getTraining().isDefendedHit());
			assertTrue(state.getTraining().getActedUnits().containsAll(List.of("trainee", "T_CAT", "T_STONE")));
		}
	}

	@Test
	void earlyKillDoesNotInventDefenseOrReviveTarget() {
		var state = policy.createAcademy("BQ018", "WARRIOR", List.of(), 3L);
		state.getUnits().stream().filter(Unit::isEnemy).forEach(u -> u.setHp(1));
		var orders = state.getUnits()
			.stream()
			.filter(u -> !u.isEnemy())
			.map(u -> new Order(u.getId(), "ATTACK", "academy-target"))
			.toList();
		engine.resolve(state, orders, false, NO_ITEMS);
		assertEquals("LOST", state.getStatus());
		assertFalse(state.getTraining().isDefendedHit());
		assertTrue(state.getUnits().stream().filter(Unit::isEnemy).noneMatch(Unit::alive));
		assertTrue(state.getLog().stream().anyMatch(s -> s.contains("战斗胜利")));
	}

	@Test
	void borrowedFiveRequireFourDistinctChoicesAndFifthCannotSwap() {
		assertThrows(BusinessException.class,
				() -> policy.createAcademy("BQ019", "MAGE", List.of("T_CAT", "T_CAT", "T_STONE", "T_GALE"), 1L));
		var state = policy.createAcademy("BQ019", "MAGE", List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"), 1L);
		assertEquals(4,
				state.getUnits()
					.stream()
					.filter(u -> u.getId().equals("T_RABBIT"))
					.findFirst()
					.orElseThrow()
					.getPosition());
		assertThrows(BusinessException.class,
				() -> engine.resolve(
						state, List.of(new Order("trainee", "DEFEND", "trainee"),
								new Order("T_CAT", "SWAP", "T_RABBIT"), new Order("T_STONE", "DEFEND", "T_STONE")),
						false, NO_ITEMS));
		engine.resolve(state, List.of(new Order("trainee", "DEFEND", "trainee"), new Order("T_CAT", "SWAP", "T_SPROUT"),
				new Order("T_STONE", "DEFEND", "T_STONE")), false, NO_ITEMS);
		assertEquals("WON", state.getStatus());
		assertTrue(state.getTraining().isSwapped());
		assertTrue(state.getUnits().stream().filter(Unit::isEnemy).allMatch(Unit::alive));
		assertFalse(state.getLog().stream().anyMatch(s -> s.contains("借用芽灵防御") || s.contains("借用芽灵使用")));
	}

}
