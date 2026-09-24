package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LifeSkillRulesTest {

	@Test
	void thresholdsDoubleAndKeepHalfPointBeforePromotion() {
		long[] thresholds = { 0, 128, 384, 896, 1920, 3968, 8064, 16256, 32640, 65408 };
		for (int level = 1; level <= 10; level++) {
			assertEquals(thresholds[level - 1], LifeSkillRules.threshold(level));
			assertEquals(level, LifeSkillRules.level(thresholds[level - 1] * 2));
			assertEquals((level - 1) / 2, LifeSkillRules.toolTier(level));
			if (level > 1)
				assertEquals(level - 1, LifeSkillRules.level(thresholds[level - 1] * 2 - 1));
		}
	}

	@Test
	void lowerContentDecaysWithoutDiscardingHalfPoints() {
		assertEquals(2, LifeSkillRules.gain(0, 1, 1));
		assertEquals(257, LifeSkillRules.gain(256, 1, 1));
		assertEquals(768, LifeSkillRules.gain(768, 1, 1));
		assertEquals(130816, LifeSkillRules.gain(130814, 9, 20));
		assertThrows(IllegalArgumentException.class, () -> LifeSkillRules.gain(0, 2, 1));
		assertThrows(IllegalArgumentException.class, () -> LifeSkillRules.level(-1));
	}

}
