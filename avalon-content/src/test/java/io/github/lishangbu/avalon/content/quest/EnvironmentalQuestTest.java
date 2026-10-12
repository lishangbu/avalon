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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** 设施资料直接确认，实际行动仍检查条件，且不能混入到达自动领奖路径。 */
class EnvironmentalQuestTest {

	private QuestContentStep step(List<String> facts, String npc, boolean arrival) {
		return new QuestContentStep("BQ026", "BS04", "郊野调查", "CITY_OUTSKIRTS", "整理现场记录", "记录完成", List.of(), true,
				List.of(), null, facts, 0, arrival, npc, true, List.of(), null);
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
		var step = step(List.of("SCENE:BQ026_HABITAT", "VICTORY:OUTSKIRTS"), null, false);
		assertDoesNotThrow(() -> validate(step));
		assertTrue(QuestCatalog.receptionAvailable(step, new WorldCatalog().definition(), fact -> false));
		// 接待许可不等于任务事实成立；事实仍由查询及命令服务检查。
		assertEquals(2, step.facts().size());
	}

	@Test
	void facilityInformationMayBeConfirmedButMixedNpcAndAutomaticArrivalAreRejected() {
		assertDoesNotThrow(() -> validate(step(List.of(), null, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(List.of("QUEST:BQ025"), null, false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(List.of("SCENE:TRACE"), "路人", false)));
		assertThrows(IllegalArgumentException.class, () -> validate(step(List.of("SCENE:TRACE"), null, true)));
	}

	@Test
	void schemaEnforcesExplicitEnvironmentModeAndRealEvidence() {
		var document = new ObjectMapper().readTree("""
				{"$schema":"./schemas/quests.schema.json","steps":[
				{"id":"BQ026","quest":"BS04","title":"郊野调查","location":"CITY_OUTSKIRTS",
				"label":"整理记录","dialogue":"记录完成","environmental":true,"facts":["SCENE:TRACE"]}]}
				""");
		assertDoesNotThrow(() -> ContentLoader.validate("/content/quests.json", document));
		var step = (ObjectNode) document.path("steps").get(0);
		step.put("npcId", "FEYN");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
		step.remove("npcId");
		step.put("onArrival", true);
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
		step.remove("onArrival");
		step.putArray("facts").add("QUEST:BQ025");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
	}

}
