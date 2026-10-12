package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** 验证多人物接待与移动资格，不需要数据库，也不把任意显示名当作身份。 */
class NpcReceptionTest {

	private static final List<WorldCatalog.Npc> NPCS = List.of(new WorldCatalog.Npc("FEYN", "费恩"),
			new WorldCatalog.Npc("MILA", "米菈"));

	private WorldCatalog.NpcBinding binding(String npc, String required, String closed) {
		return new WorldCatalog.NpcBinding(npc, required, closed);
	}

	private WorldCatalog.Definition world(List<WorldCatalog.Npc> npcs, List<WorldCatalog.NpcBinding> first,
			List<WorldCatalog.NpcBinding> second) {
		return new WorldCatalog.Definition("A", List.of(new WorldCatalog.Profession("WARRIOR", "战士", "")),
				List.of(new WorldCatalog.Place("A", "学院", "学院", 0, 0, List.of("B"), null, List.of(), null, first),
						new WorldCatalog.Place("B", "营地", "营地", 1, 0, List.of("A"), null, List.of(), null, second)),
				List.of(), npcs);
	}

	private QuestContentStep step(String id, String place, String npcId) {
		return new QuestContentStep(id, "TEST", "任务", place, "交谈", "你好", List.of(), false, List.of(), null, List.of(),
				0, false, npcId, false, List.of(), null);
	}

	private void validateQuests(WorldCatalog.Definition world, QuestContentStep... steps) {
		QuestCatalog.validate(new QuestContentDefinition(List.of(steps)), world, new ItemCatalog(new WorldCatalog()),
				new CreatureCatalog());
	}

	@Test
	void twoNpcsShareLocationAndMoveWithoutDuplicatePresence() {
		var world = world(NPCS, List.of(binding("FEYN", null, null), binding("MILA", null, "QUEST:MOVE")),
				List.of(binding("MILA", "QUEST:MOVE", null)));
		assertDoesNotThrow(() -> WorldCatalog.validate(world));
		var first = step("MOVE", "A", "MILA");
		var second = step("VISIT", "B", "MILA");
		assertDoesNotThrow(() -> validateQuests(world, first, second));
		assertTrue(QuestCatalog.receptionAvailable(first, world, Set.<String>of()::contains));
		assertFalse(QuestCatalog.receptionAvailable(second, world, Set.<String>of()::contains));
		assertFalse(QuestCatalog.receptionAvailable(first, world, Set.of("QUEST:MOVE")::contains));
		assertTrue(QuestCatalog.receptionAvailable(second, world, Set.of("QUEST:MOVE")::contains));
		assertTrue(QuestCatalog.receptionAvailable(step("OTHER", "A", "FEYN"), world, Set.of("QUEST:MOVE")::contains));
	}

	@Test
	void invalidIdentityBindingsAndAmbiguousPresenceFailAtStartup() {
		assertThrows(IllegalArgumentException.class,
				() -> WorldCatalog.validate(world(NPCS, List.of(binding("UNKNOWN", null, null)), List.of())));
		assertThrows(IllegalArgumentException.class, () -> WorldCatalog
			.validate(world(NPCS, List.of(binding("FEYN", null, null), binding("FEYN", null, null)), List.of())));
		assertThrows(IllegalArgumentException.class, () -> WorldCatalog
			.validate(world(NPCS, List.of(binding("MILA", null, null)), List.of(binding("MILA", null, null)))));
		assertThrows(IllegalArgumentException.class,
				() -> WorldCatalog.validate(world(List.of(NPCS.getFirst(), NPCS.getFirst()), List.of(), List.of())));
		var world = world(NPCS, List.of(binding("FEYN", "QUEST:TYPO", null)), List.of());
		assertThrows(IllegalArgumentException.class, () -> validateQuests(world, step("START", "A", "FEYN")));
		assertThrows(IllegalArgumentException.class,
				() -> validateQuests(world(NPCS, List.of(), List.of()), step("START", "A", "FEYN")));
	}

	@Test
	void displayNamesCannotReplaceStableIdentityAndFormalContentLoads() {
		var catalog = new WorldCatalog();
		assertDoesNotThrow(() -> new QuestCatalog(catalog, new ItemCatalog(catalog), new CreatureCatalog()));
		var modern = world(NPCS, List.of(binding("FEYN", null, null)), List.of());
		var spoofed = step("START", "A", "费恩");
		assertThrows(IllegalArgumentException.class, () -> validateQuests(modern, spoofed));
		assertFalse(QuestCatalog.receptionAvailable(spoofed, modern, value -> true));
		assertThrows(IllegalArgumentException.class, () -> validateQuests(modern, step("START", "A", null)));
	}

	@Test
	void schemasAcceptStableIdentityAndRejectMixedStepModes() {
		var mapper = new ObjectMapper();
		var document = mapper.readTree("""
				{"$schema":"./schemas/quests.schema.json","steps":[
				{"id":"START","quest":"BQ","title":"任务","location":"A","npcId":"FEYN",
				"label":"交谈","dialogue":"你好"}]}
				""");
		assertDoesNotThrow(() -> ContentLoader.validate("/content/quests.json", document));
		((ObjectNode) document.path("steps").get(0)).put("npc", "费恩");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
		var entry = (ObjectNode) document.path("steps").get(0);
		entry.remove("npcId");
		// 仅提供显示名也必须失败，不能退回按姓名匹配的接待方式。
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
		entry.remove("npc");
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/quests.json", document));
	}

	@Test
	void mapSchemaRejectsIndependentDisplayNameField() {
		var mapper = new ObjectMapper();
		// 从正式文件验证：地点只绑定稳定身份，不能另存一套接待姓名。
		try (var input = getClass().getResourceAsStream("/content/opening.json")) {
			var formal = mapper.readTree(input);
			assertDoesNotThrow(() -> ContentLoader.validate("/content/opening.json", formal));
			((ObjectNode) formal.path("places").get(0)).put("npc", "村长");
			assertThrows(IllegalArgumentException.class, () -> ContentLoader.validate("/content/opening.json", formal));
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
	}

}
