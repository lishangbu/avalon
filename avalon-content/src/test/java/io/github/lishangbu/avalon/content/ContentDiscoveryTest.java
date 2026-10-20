package io.github.lishangbu.avalon.content;

import io.github.lishangbu.avalon.content.quest.QuestDocuments;
import io.github.lishangbu.avalon.content.battle.DungeonDocuments;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import static org.junit.jupiter.api.Assertions.*;

class ContentDiscoveryTest {

	@Test
	void discoveredPathsAreSortedAndIncludeNestedChains() {
		var paths = ContentLoader.discover("quests");
		assertEquals(paths.stream().sorted().toList(), paths);
		assertTrue(paths.contains("/content/quests/nom/main.json"));
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.discover("../schemas"));
		assertThrows(IllegalArgumentException.class, () -> ContentLoader.discover("missing-domain"));
	}

	@Test
	void newChainFilenameNeedsNoRegistrationAndDuplicateIdsShowBothSources() {
		var original = ContentLoader.documents("quests").get("/content/quests/nom/main.json");
		var moved = QuestDocuments.fromDocuments(Map.of("/content/quests/nom/new-chain.json", original));
		assertFalse(moved.content().steps().isEmpty());
		var error = assertThrows(IllegalArgumentException.class, () -> QuestDocuments.fromDocuments(
				Map.of("/content/quests/nom/main.json", original, "/content/quests/nom/duplicate.json", original)));
		assertTrue(error.getMessage().contains("main.json"));
		assertTrue(error.getMessage().contains("duplicate.json"));
	}

	@Test
	void stepOwnershipAndConversationTyposReportFileAndField() {
		ObjectNode document = (ObjectNode) ContentLoader.documents("quests")
			.get("/content/quests/nom/main.json")
			.deepCopy();
		var step = (ObjectNode) document.path("quests").get(0).path("steps").get(0);
		step.put("quest", "UNKNOWN_QUEST");
		var error = assertThrows(IllegalArgumentException.class,
				() -> QuestDocuments.fromDocuments(Map.of("/content/quests/nom/main.json", document)));
		assertTrue(error.getMessage().contains("main.json"));
		assertTrue(error.getMessage().contains(".quest"));
		step.put("quest", document.path("quests").get(0).path("id").asString());
		((ObjectNode) step.path("conversations")).put("offre", "typo");
		error = assertThrows(IllegalArgumentException.class,
				() -> QuestDocuments.fromDocuments(Map.of("/content/quests/nom/main.json", document)));
		assertTrue(error.getMessage().contains("conversations"));
	}

	@Test
	void dungeonBundleRejectsCrossDungeonChestAndDuplicateIds() {
		var documents = ContentLoader.documents("dungeons");
		String path = documents.keySet().iterator().next();
		ObjectNode document = (ObjectNode) documents.get(path).deepCopy();
		((ObjectNode) document.path("chests")).put("dungeon", "OTHER_DUNGEON");
		var error = assertThrows(IllegalArgumentException.class,
				() -> DungeonDocuments.fromDocuments(Map.of(path, document)));
		assertTrue(error.getMessage().contains("chests.dungeon"));
		assertTrue(error.getMessage().contains(path));
		error = assertThrows(IllegalArgumentException.class, () -> DungeonDocuments
			.fromDocuments(Map.of(path, documents.get(path), "/content/dungeons/duplicate.json", documents.get(path))));
		assertTrue(error.getMessage().contains("duplicate.json"));
		assertTrue(error.getMessage().contains(path));
	}

}
