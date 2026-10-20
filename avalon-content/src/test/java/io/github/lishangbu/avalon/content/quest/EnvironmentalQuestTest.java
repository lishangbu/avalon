package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** 环境任务明确声明交付模式，目标必须来自真实行动。 */
class EnvironmentalQuestTest {

	private QuestContentStep step(List<String> facts, String npc, boolean arrival) {
		return new QuestContentStep("N026", "N026", "郊野调查", "CITY_OUTSKIRTS", "整理现场记录", "记录完成", List.of(), true,
				List.of(),
				List.<String>copyOf(facts)
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), (arrival ? QuestDelivery.AUTO : QuestDelivery.MANUAL), null, null, npc, true, List.of(),
				new QuestReception("CITY_OUTSKIRTS", npc));
	}

	private void validate(QuestContentStep step) {
		var world = new WorldCatalog();
		// 此测试只验证环境任务约束，不让其他地点的接待前置污染单步骤夹具。
		var map = new WorldDefinition("CITY_OUTSKIRTS", world.definition().professions(),
				List.of(new WorldPlace("CITY_OUTSKIRTS", "城郊", "现场", 0, 0, List.of())));
		QuestCatalog.validate(new QuestContentDefinition(List.of(step)), map, new ItemCatalog(world),
				new CreatureCatalog());
	}

	@Test
	void environmentalTaskHasNoNpcButRequiresActionEvidence() {
		var step = step(List.of("SCENE:N026_HABITAT", "VICTORY:OUTSKIRTS"), null, false);
		assertDoesNotThrow(() -> validate(step));
		assertTrue(QuestCatalog.receptionAvailable(step, new WorldCatalog().definition(), fact -> false));
		// 接待许可不等于任务事实成立；事实仍由查询及命令服务检查。
		assertEquals(2, step.objectives().stream().map(objective -> objective.code()).toList().size());
	}

	@Test
	void facilityInformationAndAutomaticDeliveryUseExplicitModeWithoutNpc() {
		assertDoesNotThrow(() -> validate(step(List.of(), null, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(List.of("QUEST:O013"), null, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(List.of("SCENE:TRACE"), "路人", false)));
		assertDoesNotThrow(() -> validate(step(List.of("SCENE:TRACE"), null, true)));
	}

	@Test
	void schemaEnforcesExplicitEnvironmentModeAndRealEvidence() {
		var document = new ObjectMapper().readTree(
				"""
						{"$schema":"./schemas/quests.schema.json","steps":[
						{"id":"N026","quest":"N026","title":"郊野调查","location":"CITY_OUTSKIRTS",
						"label":"整理记录","dialogue":"记录完成","environmental":true,"effects":[],"objectives":[{"id":"trace","measure":"EVENT","code":"SCENE:TRACE","amount":1}],"conditions":[],"delivery":"MANUAL","requires":[],"acceptEffects":[],"acceptance":{"location":"CITY_OUTSKIRTS"}}]}
						""");
		assertDoesNotThrow(() -> ContentLoader.validateSchema("quests", document));
		var step = (ObjectNode) document.path("steps").get(0);
		step.put("npcId", "FEYN");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validateSchema("quests", document));
		step.remove("npcId");
		step.put("onArrival", true);
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validateSchema("quests", document));
		step.remove("onArrival");
		for (String legacy : List.of("facts", "minimumActive", "placement", "runtime")) {
			step.putNull(legacy);
			assertThrows(IllegalArgumentException.class, () -> ContentLoader.validateSchema("quests", document),
					legacy);
			step.remove(legacy);
		}
		step.putArray("objectives")
			.addObject()
			.put("id", "invalid")
			.put("measure", "EVENT")
			.put("code", "QUEST:O013")
			.put("amount", 1);
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validateSchema("quests", document));
	}

}
