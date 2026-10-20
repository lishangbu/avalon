package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestDialogue;
import io.github.lishangbu.avalon.content.quest.model.QuestDialoguesDefinition;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
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
			.collect(Collectors.toMap(QuestContentStep::id, step -> catalog.step(step.id())));
		// 正式对白与新故事所有步骤逐项对应。
		assertEquals(327, texts.size());
		assertDoesNotThrow(() -> QuestDialogueCatalog.validate(new QuestDialoguesDefinition(texts), quests));
		var missing = new HashMap<>(texts);
		missing.remove("O001");
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialoguesDefinition(missing), quests));
		var extra = new HashMap<>(texts);
		extra.put("OBSOLETE", texts.get("O001"));
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialoguesDefinition(extra), quests));
		var empty = new HashMap<>(texts);
		empty.put("O001", new QuestDialogue(" ", "接取", "进行", "待交", "再访", "重接"));
		assertThrows(IllegalArgumentException.class,
				() -> QuestDialogueCatalog.validate(new QuestDialoguesDefinition(empty), quests));
	}

}
