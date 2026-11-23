package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LifeSkillRulesTest {

	/** 复核离线单人包覆计划的全部升级边界；不证明原料、工具及地图已取得。 */
	@Test
	void soloWrapPlanReachesNineUsingActualRecipeRewardsWithoutInjectedProficiency() {
		var world = new WorldCatalog();
		var production = new LifeProductionCatalog(new ItemCatalog(world), world);
		int[] counts = { 43, 43, 170, 171, 682, 683, 2730, 2731 };
		long halfPoints = 0;
		int crafts = 0;
		for (int level = 1; level <= 8; level++) {
			String id = "LIFE_WRAP_" + (level - 1) / 2 + (level % 2 == 0 ? "_FINE" : "");
			var recipe = production.recipe(id);
			assertEquals(level, LifeSkillRules.level(halfPoints));
			for (int count = 0; count < counts[level - 1]; count++) {
				assertEquals(level, LifeSkillRules.level(halfPoints), "本阶段不能在升级后继续领取同级收益");
				halfPoints = LifeSkillRules.gain(halfPoints, recipe.level(), recipe.points());
				crafts++;
			}
			assertEquals(level + 1, LifeSkillRules.level(halfPoints));
		}
		assertEquals(7253, crafts);
		assertEquals(65286, halfPoints);
		var wrap = production.recipe("LIFE_WRAP_4");
		for (int count = 0; count < 3; count++)
			halfPoints = LifeSkillRules.gain(halfPoints, wrap.level(), wrap.points());
		var armor = production.recipe("E_FORGED_BODY");
		halfPoints = LifeSkillRules.gain(halfPoints, armor.level(), armor.points());
		assertEquals(65336, halfPoints);
		assertEquals(9, LifeSkillRules.level(halfPoints));
	}

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
