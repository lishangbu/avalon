package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 验证累计升级边界、开篇预算与满级截断，避免奖励内容更换后仍使用快速试玩公式。 */
class GrowthRulesTest {

	private final GrowthRules growth = new GrowthRules();

	@Test
	void everyThresholdLevelsUpExactlyAndRetainsOverflow() {
		for (int level = 2; level <= GrowthRules.LEVEL_CAP; level++) {
			long threshold = growth.threshold(level);
			assertEquals(level - 1, growth.level(threshold - 1));
			assertEquals(level, growth.level(threshold));
			assertEquals(threshold, growth.addExperience(threshold - 1, 1));
		}
		assertEquals(707, growth.addExperience(699, 8));
		assertEquals(3, growth.level(707));
	}

	@Test
	void openingBudgetAndLevelCapMatchThePublishedCurve() {
		assertEquals(700, growth.threshold(3));
		assertEquals(37800, growth.threshold(10));
		assertEquals(8, growth.level(22680));
		assertEquals(264919, growth.addExperience(264819, 100));
		assertEquals(23935620, growth.addExperience(23935619, 100));
		assertEquals(50, growth.level(23935620));
		assertEquals(10, growth.carryingCapacity(50));
		assertEquals(5, growth.carryingCapacity(9));
		assertEquals(6, growth.carryingCapacity(10));
		assertThrows(IllegalArgumentException.class, () -> growth.threshold(0));
		assertThrows(IllegalArgumentException.class, () -> growth.addExperience(0, -1));
	}

	@Test
	void taskExperienceMatchesOpeningBudgetAndLevelsBeforeFirstCompanionJoins() {
		var world = new WorldCatalog();
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		long total = quests.definition()
			.steps()
			.stream()
			.filter(step -> Integer.parseInt(step.id().substring(2)) <= 30)
			.flatMap(step -> step.effects().stream())
			.filter(effect -> effect.kind() == QuestCatalog.Kind.EXPERIENCE)
			.mapToLong(QuestCatalog.Effect::amount)
			.sum();
		assertEquals(22680, total);
		long earned = 0;
		for (String id : List.of("BQ001", "BQ002", "BQ003", "BQ004"))
			for (var effect : quests.step(id).effects()) {
				if (effect.kind() == QuestCatalog.Kind.EXPERIENCE)
					earned += effect.amount();
				if (effect.kind() == QuestCatalog.Kind.GIVE_PET)
					assertEquals(3, growth.level(earned), "先到3级再结契，不给新伙伴追领此前经验");
			}
		assertEquals(700, earned);
	}

}
