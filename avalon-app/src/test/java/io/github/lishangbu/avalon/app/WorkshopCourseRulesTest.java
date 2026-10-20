package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.WorkshopCourseRules;
import io.github.lishangbu.avalon.creature.domain.CreatureStorageRules;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 用可控随机票验证学习的两种真实分支，且验证四职业的资源输入，避免只靠一轮随机集成碰运气。 */
class WorkshopCourseRulesTest extends SpringIntegrationTest {

	@Autowired
	WorkshopCourseRules rules;

	@Test
	void bothLearningBranchesPreserveNewSkillAndRejectInvalidBooksBeforeSampling() {
		for (int ticket : new int[] { 30, 90 }) {
			var state = rules.create("WORKSHOP_SKILLS", "MAGE");
			var random = new Random() {
				int calls;

				@Override
				public int nextInt(int bound) {
					return calls++ == 0 ? ticket : 1;
				}
			};
			rules.apply(state, "VIEW_POOL", null, null, random);
			assertThrows(BusinessException.class, () -> rules.apply(state, "LEARN", "BOOK_ROCK", null, random));
			assertThrows(BusinessException.class, () -> rules.apply(state, "LEARN", "BOOK_WATER", null, random));
			assertEquals(0, random.calls);
			rules.apply(state, "LEARN", "BOOK_LIGHT_SCREEN", null, random);
			assertEquals(ticket == 30 ? 0 : 1, state.getLearningResult().forgotten().size());
			assertTrue(state.getSkills().contains("光幕"));
			if (ticket == 90)
				assertEquals(List.of("微疗"), state.getLearningResult().forgotten());
			assertEquals(0, state.getBooks().get("BOOK_LIGHT_SCREEN"));
		}
	}

	@Test
	void everyProfessionGetsFixedResourcesAndSwapNeverHeals() {
		String[] professions = { "WARRIOR", "MAGE", "PRIEST", "RANGER" };
		int[] health = { 432, 306, 360, 342 };
		int[] mana = { 51, 85, 85, 68 };
		for (int i = 0; i < professions.length; i++) {
			var state = rules.create("WORKSHOP_GEAR", professions[i]);
			assertEquals(health[i], state.getBaseHp());
			assertEquals(mana[i], state.getBaseMp());
			rules.apply(state, "INSPECT_EQUIPMENT", null, null, new Random());
			rules.apply(state, "EQUIP", "B", null, new Random());
			assertEquals(health[i] - 40, state.getHp());
			assertEquals(mana[i] - 12, state.getMp());
			rules.apply(state, "UNEQUIP", null, null, new Random());
			assertEquals(health[i] - 40, state.getHp());
			state.setHp(0);
			rules.apply(state, "EQUIP", "B", null, new Random());
			assertEquals(0, state.getHp(), "换装不能让倒下单位复活");
		}
	}

	@Test
	void formalStorageCapacityAndCarryLevelChecksAlsoApplyToBorrowedPets() {
		Set<Integer> full = IntStream.range(100, 130).boxed().collect(Collectors.toSet());
		assertThrows(BusinessException.class, () -> CreatureStorageRules.deposit(2, full));
		assertThrows(BusinessException.class, () -> CreatureStorageRules.deposit(1, Set.of()));
		assertEquals(100, CreatureStorageRules.deposit(2, Set.of()));
		assertThrows(BusinessException.class, () -> CreatureStorageRules.requirePosition(100, 5, 5, 7, 7, true));
		assertThrows(BusinessException.class, () -> CreatureStorageRules.requirePosition(100, 2, 5, 8, 7, true));
		assertThrows(BusinessException.class, () -> CreatureStorageRules.requirePosition(100, 2, 5, 7, 7, false));
	}

}
