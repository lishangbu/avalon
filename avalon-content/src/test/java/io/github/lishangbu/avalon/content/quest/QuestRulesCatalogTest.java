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
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

/** 正式任务分组、并行课程完成判定及规则目录的跨文件约束。 */
class QuestRulesCatalogTest {

	@Test
	void explicitPolicyAndAbandonmentAreRequired() {
		var json = JsonMapper.builder().build();
		String missing = "{\"id\":\"TEST\",\"title\":\"测试\",\"type\":\"SIDE\",\"tags\":[],\"abandonable\":true}";
		assertThrows(RuntimeException.class, () -> json.readValue(missing, QuestRule.class));
		var policy = new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of());
		assertThrows(NullPointerException.class,
				() -> new QuestRule("TEST", "测试", QuestType.SIDE, List.of(), null, policy));
	}

	private QuestCatalog quests() {
		var world = new WorldCatalog();
		return new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
	}

	@Test
	void everyFormalQuestHasExplicitRulesAndLabels() {
		var quests = quests();
		var rules = new QuestRulesCatalog(quests);
		// 新故事替换旧分组，支线不得误变成主线门槛。
		assertEquals(300, rules.definition().quests().size());
		for (int number = 1; number <= 25; number++)
			assertEquals(QuestType.MAIN, rules.rule("N%03d".formatted(number)).type());
		for (String id : List.of("N026", "N027", "N028", "N029", "N037", "N038", "N039", "N045")) {
			assertEquals(QuestType.SIDE, rules.rule(id).type());
			assertTrue(rules.rule(id).abandonable());
		}
		assertEquals(QuestRepeatPolicy.ONCE, rules.rule("O013").policy().repeatPolicy());
		assertFalse(rules.rule("O013").abandonable());
		assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
			.validate(new QuestRulesDefinition(rules.definition().tags(), List.of()), quests.definition()));
	}

	@Test
	void labelsMustExistAndMainQuestsCannotBeAbandoned() {
		var quests = quests();
		var definition = new QuestRulesCatalog(quests).definition();
		for (var tags : List.of(List.of("UNKNOWN"), List.of("GUIDE", "GUIDE"))) {
			var changed = new ArrayList<>(definition.quests());
			changed.set(0, new QuestRule("O001", "今天要做的事", QuestType.MAIN, tags, false,
					new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())));
			assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
				.validate(new QuestRulesDefinition(definition.tags(), changed), quests.definition()));
		}
		var changed = new ArrayList<>(definition.quests());
		changed.set(0, new QuestRule("O001", "今天要做的事", QuestType.MAIN, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())));
		assertThrows(IllegalArgumentException.class, () -> QuestRulesCatalog
			.validate(new QuestRulesDefinition(definition.tags(), changed), quests.definition()));
	}

	@Test
	void completionRequiresEveryStepIncludingAllParallelCourses() {
		var quests = quests();
		var completed = new HashSet<String>();
		completed.add("O001");
		assertEquals(List.of("O001"), quests.completedQuests(completed));
		quests.definition()
			.steps()
			.stream()
			.filter(step -> step.quest().equals("O002"))
			.forEach(step -> completed.add(step.id()));
		assertTrue(quests.completedQuests(completed).contains("O002"));
		for (String step : List.of("O002_RECEIVE", "O002")) {
			completed.remove(step);
			assertFalse(quests.completedQuests(completed).contains("O002"));
			completed.add(step);
		}
		completed.add("O003_RECEIVE");
		assertFalse(quests.completedQuests(completed).contains("O003"));
	}

}
