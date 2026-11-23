package io.github.lishangbu.avalon.content.quest;

import io.github.lishangbu.avalon.content.ContentLoader;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 四城任务的规模、前置与叙事约束。 */
class StoryCatalogTest {

	@Test
	void allStoryDependenciesResolve() {
		var world = new WorldCatalog();
		var quests = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());
		var rules = new QuestRulesCatalog(quests);
		assertEquals(300, rules.definition().quests().size());
		assertEquals(145, rules.definition().quests().stream().filter(quest -> quest.type() == QuestType.MAIN).count());
		assertEquals(155, rules.definition().quests().stream().filter(quest -> quest.type() == QuestType.SIDE).count());
		var ids = quests.definition().steps().stream().map(step -> step.id()).collect(Collectors.toSet());
		for (var step : quests.definition().steps()) {
			assertTrue(step.quest().matches("[ONUSFJE]\\d{3}"), step.id());
			if (step.label().contains("不消耗"))
				assertFalse(step.effects().stream().anyMatch(effect -> effect.kind().equals("TAKE_ITEM")),
						step.id() + " promises to retain items but consumes them");
			for (var dependency : step.requires())
				assertTrue(ids.contains(dependency), step.id() + " -> " + dependency);
		}
		assertTrue(world.provides("F_VOLCANIC_MINE", "PVP"));
		assertFalse(world.provides("SALAMANDER_CITY", "PVP"));
	}

	@Test
	void runtimeStoryFilesFollowNarrativeConstraints() throws Exception {
		for (String file : Stream
			.concat(ContentLoader.discover("quests").stream(), ContentLoader.discover("dungeons").stream())
			.toList()) {
			try (var stream = getClass().getResourceAsStream(file)) {
				assertNotNull(stream, file);
				String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
				assertFalse(text.contains("船员"), file);
			}
		}
	}

}
