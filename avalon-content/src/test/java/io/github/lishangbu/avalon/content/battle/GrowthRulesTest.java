package io.github.lishangbu.avalon.content.battle;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
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
			.filter(step -> step.quest().matches("O\\d{3}"))
			.flatMap(step -> step.effects().stream())
			.filter(effect -> effect.kind().equals("EXPERIENCE"))
			.mapToLong(QuestEffect::amount)
			.sum();
		assertTrue(total >= growth.threshold(5), "前期任务应支撑开篇成长");
		long earned = 0;
		for (String id : List.of("O001", "O002_RECEIVE"))
			for (var effect : quests.step(id).effects()) {
				if (effect.kind().equals("EXPERIENCE"))
					earned += effect.amount();
				if (effect.kind().equals("GIVE_PET"))
					assertEquals(3, growth.level(earned), "领取伙伴前先升到3级，保证初始伙伴等级可用");
			}
		assertEquals(700, earned);
	}

	@Test
	void mineStoryRewardsBridgeMandatoryEncounterLevelGates() {
		var world = new WorldCatalog();
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		assertEquals(3000,
				quests.step("N003")
					.effects()
					.stream()
					.filter(effect -> effect.kind().equals("EXPERIENCE"))
					.mapToLong(QuestEffect::amount)
					.sum(),
				"材料交付生成逻辑不能覆盖矿道支撑的经验奖励");
		// 四职业自然旅程已验证的N007最低累计经验；后续仅累计任务奖励，忽略额外战斗经验。
		long earned = 39270;
		for (String id : List.of("N008", "N009", "N010", "N011", "N012", "N013", "N014", "N015", "N016", "N017", "N018",
				"N019", "N020")) {
			earned += quests.step(id)
				.effects()
				.stream()
				.filter(effect -> effect.kind().equals("EXPERIENCE"))
				.mapToLong(QuestEffect::amount)
				.sum();
			// 即使忽略战斗经验，必需任务也应支撑下一个遭遇的准入等级。
			int required = switch (id) {
				case "N010" -> 11;
				case "N012" -> 12;
				case "N013" -> 14;
				case "N015" -> 15;
				case "N016" -> 16;
				case "N017" -> 17;
				case "N018" -> 18;
				case "N019" -> 19;
				case "N020" -> 20;
				default -> 0;
			};
			if (required > 0)
				assertTrue(growth.level(earned) >= required, id + "奖励不足以衔接下一场必需遭遇");
		}
	}

}
