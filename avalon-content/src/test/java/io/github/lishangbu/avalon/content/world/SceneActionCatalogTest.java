package io.github.lishangbu.avalon.content.world;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.model.SceneAction;
import io.github.lishangbu.avalon.content.world.model.SceneActionDefinition;
import io.github.lishangbu.avalon.content.world.model.SceneActionType;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 场景配置与正式任务目录联合校验，不为测试另写执行引擎。 */
class SceneActionCatalogTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private SceneAction action(String id, SceneActionType type, List<String> requires, String item) {
		return new SceneAction(id, type, "VILLAGE_HOME", "动作", "观察这里", requires, null, null, item, item == null ? 0 : 1,
				List.of(), null, List.of(), "完成", null, 0);
	}

	@Test
	void validatesTaskItemsAndRejectsUnknownReferencesAndCycles() {
		var observe = action("LOOK", SceneActionType.CARE, List.of(), null);
		var collect = action("GET", SceneActionType.COLLECT_TASK, List.of("SCENE:LOOK"), "OPENING_MINE_REPORT");
		var empty = new QuestContentDefinition(List.of());
		assertDoesNotThrow(() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(observe, collect)),
				world.definition(), empty, items));
		for (var invalid : List.of(action("BAD", SceneActionType.COLLECT_TASK, List.of(), "HEALTH_POTION"),
				action("BAD", SceneActionType.CARE, List.of("SCENE:MISSING"), null),
				action("BAD", SceneActionType.CARE, List.of("SCENE:BAD"), null)))
			assertThrows(IllegalArgumentException.class, () -> SceneActionCatalog
				.validate(new SceneActionDefinition(List.of(invalid)), world.definition(), empty, items));
	}

	@Test
	void acceptsTaskDeliveryAndRejectsNonTaskDelivery() {
		var delivery = new SceneAction("DELIVER", SceneActionType.DELIVER_TASK, "VILLAGE_HOME", "交接", "提交任务物品",
				List.of(), null, null, "OPENING_MINE_REPORT", 1, List.of(), null, List.of(), "交接完成", null, 0);
		assertDoesNotThrow(() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(delivery)),
				world.definition(), new QuestContentDefinition(List.of()), items));
		var bad = new SceneAction("BAD_DELIVER", SceneActionType.DELIVER_TASK, "VILLAGE_HOME", "交接", "提交普通物品",
				List.of(), null, null, "HEALTH_POTION", 1, List.of(), null, List.of(), "完成", null, 0);
		assertThrows(IllegalArgumentException.class,
				() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(bad)), world.definition(),
						new QuestContentDefinition(List.of()), items));
	}

	@Test
	void questActionMutualDependencyCannotDeadlockContent() {
		var action = action("LOOK", SceneActionType.CARE, List.of("QUEST:REPORT"), null);
		var quest = new QuestContentStep("REPORT", "TEST_QUEST", "任务", "VILLAGE_HOME", "回报", "完成", List.of(), false,
				List.of(),
				List.<String>copyOf(List.of("SCENE:LOOK"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception("VILLAGE_HOME", null));
		assertThrows(IllegalArgumentException.class,
				() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(action)), world.definition(),
						new QuestContentDefinition(List.of(quest)), items));
	}

	@Test
	void consumptiveDeliveryCannotCreditTwoObjectivesButSharedCareCan() {
		var steps = List.of("FIRST", "SECOND")
			.stream()
			.map(id -> new QuestContentStep(id, id, "任务", "VILLAGE_HOME", "回报", "完成", List.of(), false, List.of(),
					List.<String>copyOf(List.of("SCENE:SHARED"))
						.stream()
						.map(code -> new QuestObjective(code,
								code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
										: QuestMeasure.EVENT,
								code, 1))
						.toList(),
					List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
					new QuestReception("VILLAGE_HOME", null)))
			.toList();
		var quests = new QuestContentDefinition(steps);
		var delivery = action("SHARED", SceneActionType.DELIVER_TASK, List.of(), "OPENING_MINE_REPORT");
		var rejected = assertThrows(IllegalArgumentException.class, () -> SceneActionCatalog
			.validate(new SceneActionDefinition(List.of(delivery)), world.definition(), quests, items));
		assertTrue(rejected.getMessage().contains("不能同时作为多个任务目标"));
		assertDoesNotThrow(() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(delivery)),
				world.definition(), new QuestContentDefinition(List.of(steps.getFirst())), items));
		var observation = action("SHARED", SceneActionType.CARE, List.of(), null);
		assertDoesNotThrow(() -> SceneActionCatalog.validate(new SceneActionDefinition(List.of(observation)),
				world.definition(), quests, items));
	}

	@Test
	void formalCatalogLoadsWithSceneActions() {
		var quests = new QuestCatalog(world, items, new CreatureCatalog());
		var catalog = new SceneActionCatalog(world, quests, items);
		var recovery = catalog.action("N011_RECOVER");
		assertNotNull(recovery);
		assertEquals(SceneActionType.COLLECT_TASK, recovery.type());
		assertEquals("NOM_OLD_YARD", recovery.location());
		assertTrue(recovery.requires().contains("QUEST_ACCEPT:N011"));
		assertTrue(recovery.requires().contains("VICTORY:NOM_YARD_PATROL"));
	}

	@Test
	void receivingLocationCannotRequireItsOwnQuestCompletion() {
		var step = new QuestContentStep("REPORT", "TEST_QUEST", "任务", "HOME", "回报", "完成", List.of(), false, List.of(),
				List.<String>copyOf(List.of())
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(), new QuestReception("HOME", null));
		for (String gate : List.of("QUEST:REPORT", "")) {
			var place = new WorldPlace("HOME", "家园", "接待地点", 0, 0, List.of(), gate.isEmpty() ? null : gate);
			var map = new WorldDefinition("HOME", List.of(), List.of(place));
			var quests = new QuestContentDefinition(List.of(step));
			var actions = new SceneActionDefinition(List.of());
			if (gate.isEmpty())
				assertDoesNotThrow(() -> SceneActionCatalog.validate(actions, map, quests, items));
			else
				assertThrows(IllegalArgumentException.class,
						() -> SceneActionCatalog.validate(actions, map, quests, items));
		}
	}

	@Test
	void sceneGateMustReferenceRegisteredAction() {
		var ready = action("READY", SceneActionType.CARE, List.of(), null);
		var actions = new SceneActionDefinition(List.of(ready));
		var quests = new QuestContentDefinition(List.of());
		for (String reference : List.of("SCENE:READY", "SCENE:MISSING")) {
			var place = new WorldPlace("DOCK", "码头", "准备出航", 0, 0, List.of(), reference);
			var map = new WorldDefinition("DOCK", List.of(), List.of(place));
			if (reference.equals("SCENE:READY"))
				assertDoesNotThrow(() -> SceneActionCatalog.validateWorldReferences(actions, map, quests));
			else
				assertThrows(IllegalArgumentException.class,
						() -> SceneActionCatalog.validateWorldReferences(actions, map, quests));
		}
	}

	@Test
	void formalAcceptanceCanOpenInvestigationAreaButCannotUnlockItselfFromInside() {
		var home = new WorldPlace("HOME", "入口", "锁区外接取", 0, 0, List.of("FIELD"));
		var field = new WorldPlace("FIELD", "调查区", "接取后开放", 1, 0, List.of("HOME"), "QUEST_ACCEPT:REPORT");
		var map = new WorldDefinition("HOME", List.of(), List.of(home, field));
		var survey = new SceneAction("SURVEY", SceneActionType.CARE, "FIELD", "调查", "检查现场",
				List.of("QUEST_ACCEPT:REPORT"), null, null, null, 0, List.of(), null, List.of(), "完成", null, 0);
		var actions = new SceneActionDefinition(List.of(survey));
		for (String reception : List.of("HOME", "FIELD")) {
			var step = new QuestContentStep("REPORT", "TEST_QUEST", "调查", "FIELD", "回报", "完成", List.of(), false,
					List.of(), List.of(new QuestObjective("SURVEY", QuestMeasure.EVENT, "SCENE:SURVEY", 1)), List.of(),
					QuestDelivery.MANUAL, null, null, null, true, List.of(), new QuestReception(reception, null));
			var quests = new QuestContentDefinition(List.of(step));
			// 已知事实引用合法；可达性另外判断接取者究竟在锁区外还是锁区内。
			assertDoesNotThrow(() -> SceneActionCatalog.validateWorldReferences(actions, map, quests));
			if (reception.equals("HOME"))
				assertDoesNotThrow(() -> SceneActionCatalog.validate(actions, map, quests, items));
			else
				assertThrows(IllegalArgumentException.class,
						() -> SceneActionCatalog.validate(actions, map, quests, items));
		}
		// 支持接取事实不等于接受任意字符串，未注册任务仍必须在加载时拒绝。
		assertThrows(IllegalArgumentException.class,
				() -> SceneActionCatalog.validateWorldReferences(actions, map, new QuestContentDefinition(List.of())));
	}

	@Test
	void receivingLocationCannotBeHiddenBehindItsOwnRoadGate() {
		var step = new QuestContentStep("REPORT", "TEST_QUEST", "任务", "DESTINATION", "回报", "完成", List.of(), false,
				List.of(),
				List.<String>copyOf(List.of())
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception("DESTINATION", null));
		var home = new WorldPlace("HOME", "家园", "起点", 0, 0, List.of("ROAD"));
		var destination = new WorldPlace("DESTINATION", "终点", "接待地点", 2, 0, List.of("ROAD"));
		for (String gate : List.of("QUEST:REPORT", "")) {
			var road = new WorldPlace("ROAD", "道路", "必经道路", 1, 0, List.of("HOME", "DESTINATION"),
					gate.isEmpty() ? null : gate);
			var map = new WorldDefinition("HOME", List.of(), List.of(home, road, destination));
			var quests = new QuestContentDefinition(List.of(step));
			var actions = new SceneActionDefinition(List.of());
			if (gate.isEmpty())
				assertDoesNotThrow(() -> SceneActionCatalog.validate(actions, map, quests, items));
			else
				assertThrows(IllegalArgumentException.class,
						() -> SceneActionCatalog.validate(actions, map, quests, items));
		}
	}

}
