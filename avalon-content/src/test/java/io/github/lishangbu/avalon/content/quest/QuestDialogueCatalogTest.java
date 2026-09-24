package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;

import java.util.HashMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 对白是完整的正式内容；遗漏步骤、夹带废弃步骤和空白文案都必须在启动前拒绝。 */
class QuestDialogueCatalogTest {

	@Test
	void everyFormalStepHasValidatedDialogue() {
		var world = new WorldCatalog();
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var catalog = new QuestDialogueCatalog(quests);
		var texts = quests.definition()
			.steps()
			.stream()
			.collect(Collectors.toMap(QuestCatalog.Step::id, step -> catalog.step(step.id())));
		assertEquals(66, texts.size());
		assertDoesNotThrow(() -> QuestDialogueCatalog.validate(new QuestDialogueCatalog.Definition(texts), quests));
		var missing = new HashMap<>(texts);
		missing.remove("BQ001");
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialogueCatalog.Definition(missing), quests));
		var extra = new HashMap<>(texts);
		extra.put("OBSOLETE", texts.get("BQ001"));
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialogueCatalog.Definition(extra), quests));
		var empty = new HashMap<>(texts);
		empty.put("BQ001", new QuestDialogueCatalog.Dialogue(" ", "接取", "进行", "待交", "再访", "重接"));
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialogueCatalog.Definition(empty), quests));
	}

}
