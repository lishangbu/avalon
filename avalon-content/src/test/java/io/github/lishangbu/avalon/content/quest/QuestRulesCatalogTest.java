package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

/** 正式任务分组、并行课程完成判定及规则目录的跨文件约束。 */
class QuestRulesCatalogTest {

	@Test
	void omittedAbandonmentUsesQuestTypeAndExplicitSideOverrideRemainsSupported() {
		var json = JsonMapper.builder().build();
		String template = """
				{"id":"TEST","title":"测试委托","type":"%s","tags":[],"repeatPolicy":"ONCE"%s}
				""";
		assertTrue(json.readValue(template.formatted("SIDE", ""), QuestRule.class).abandonable());
		assertFalse(json.readValue(template.formatted("MAIN", ""), QuestRule.class).abandonable());
		assertFalse(
				json.readValue(template.formatted("SIDE", ",\"abandonable\":false"), QuestRule.class).abandonable());
	}

	private QuestCatalog quests() {
		var world = new WorldCatalog();
		return new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
	}

	@Test
	void everyFormalQuestHasExplicitRulesAndLabels() {
		var quests = quests();
		var rules = new QuestRulesCatalog(quests);
		assertEquals(14, rules.definition().quests().size());
		assertEquals(QuestRepeatPolicy.ONCE, rules.rule("BS03").repeatPolicy());
		assertTrue(rules.rule("BS03").tags().contains("GUIDE"));
		assertFalse(rules.rule("BS03").abandonable());
		assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
			.validate(new QuestRulesDefinition(rules.definition().tags(), List.of()), quests.definition()));
	}

	@Test
	void labelsMustExistAndMainQuestsCannotBeAbandoned() {
		var quests = quests();
		var definition = new QuestRulesCatalog(quests).definition();
		for (var tags : List.of(List.of("UNKNOWN"), List.of("GUIDE", "GUIDE"))) {
			var changed = new ArrayList<>(definition.quests());
			changed.set(0, new QuestRule("BQ001", "今天要做的事", QuestType.MAIN, tags, QuestRepeatPolicy.ONCE, false));
			assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
				.validate(new QuestRulesDefinition(definition.tags(), changed), quests.definition()));
		}
		var changed = new ArrayList<>(definition.quests());
		changed.set(0, new QuestRule("BQ001", "今天要做的事", QuestType.MAIN, List.of(), QuestRepeatPolicy.ONCE, true));
		assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
			.validate(new QuestRulesDefinition(definition.tags(), changed), quests.definition()));
	}

	@Test
	void completionRequiresEveryStepIncludingAllParallelCourses() {
		var quests = quests();
		var completed = new HashSet<String>();
		completed.add("BQ005");
		assertEquals(List.of("BQ005"), quests.completedQuests(completed));
		quests.definition()
			.steps()
			.stream()
			.filter(step -> step.quest().equals("BS03"))
			.forEach(step -> completed.add(step.id()));
		assertTrue(quests.completedQuests(completed).contains("BS03"));
		for (int lesson = 19; lesson <= 23; lesson++) {
			String step = "BQ0" + lesson;
			completed.remove(step);
			assertFalse(quests.completedQuests(completed).contains("BS03"));
			completed.add(step);
		}
		completed.add("BQ014");
		assertFalse(quests.completedQuests(completed).contains("BS02"));
	}

}
