package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import io.github.lishangbu.avalon.content.world.model.WorldSafeReturn;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 地图和任务各自合法不代表跨目录引用合法，所有开关与安全返回条件均须检查。 */
class WorldQuestReferenceTest {

	@Test
	void everyLocationGateAndSafeReturnMustReferenceAnExistingQuest() {
		var world = new WorldCatalog();
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog()).definition();
		assertDoesNotThrow(() -> QuestCatalog.validateWorldReferences(quests, world.definition()));
		for (int field = 0; field < 4; field++) {
			var place = new WorldPlace("SQUARE", "广场", "描述", 0, 0, List.of(), field == 0 ? "QUEST:MISSING" : null,
					List.of(), field == 1 ? "QUEST:MISSING" : null, List.of(),
					field == 2 ? List.of("QUEST:MISSING") : List.of());
			var changed = new WorldDefinition("SQUARE", world.definition().professions(), List.of(place),
					List.of(new WorldSafeReturn("SQUARE", field == 3 ? "QUEST:MISSING" : null, List.of())));
			assertThrows(IllegalArgumentException.class, () -> QuestCatalog.validateWorldReferences(quests, changed));
		}
	}

}
