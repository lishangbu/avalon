package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;

import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;

import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestObjectiveQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestDeliveryRequirements;
import io.github.lishangbu.avalon.world.persistence.repository.QuestRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import java.util.List;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真实任务目录上的记录投影，验证未开放剧情不泄露及并行课程归档的完整性。 */
class QuestJournalTest {

	private final WorldCatalog world = new WorldCatalog();

	private final QuestCatalog catalog = new QuestCatalog(world, new ItemCatalog(world), new CreatureCatalog());

	private final QuestRepository receipts = mock(QuestRepository.class);

	private QuestQueryService query() {
		var characters = mock(CharacterQueryService.class);
		var facts = mock(CharacterFactQueryService.class);
		when(characters.requireId(1)).thenReturn(2L);
		when(facts.codes(1)).thenReturn(Set.of());
		return new QuestQueryService(characters, receipts, catalog, facts, mock(SceneActionCatalog.class), world,
				mock(BattleCatalog.class), new QuestRulesCatalog(catalog), mock(QuestJournalStateRepository.class),
				mock(QuestObjectiveQueryService.class), mock(QuestDeliveryRequirements.class),
				mock(QuestRuntimeService.class));
	}

	@Test
	void newCharacterOnlySeesFirstAvailableStory() {
		when(receipts.progress(2)).thenReturn(List.of());
		var state = query().state(1);
		assertEquals(List.of("BQ001"), state.journal().stream().map(entry -> entry.id()).toList());
		assertFalse(state.journal().getFirst().completed());
		assertEquals(0, state.journal().getFirst().completedSteps());
	}

	@Test
	void currentGroupDoesNotRevealFutureStepTitlesOrDestinations() {
		var progress = catalog.definition()
			.steps()
			.stream()
			.filter(step -> step.id().compareTo("BQ031") < 0)
			.map(step -> new QuestRepository.Progress(step.id(), step.quest(), step.completes()))
			.toList();
		when(receipts.progress(2)).thenReturn(progress);
		var rowan = query().state(1)
			.journal()
			.stream()
			.filter(entry -> entry.id().equals("BS05"))
			.findFirst()
			.orElseThrow();
		assertEquals(9, rowan.totalSteps());
		assertEquals(List.of("BQ031"), rowan.steps().stream().map(step -> step.id()).toList());
	}

	@Test
	void partialAcademyStaysCurrentAndAllCoursesCompleteTheGroup() {
		var progress = catalog.definition()
			.steps()
			.stream()
			.filter(step -> step.id().compareTo("BQ024") < 0)
			.map(step -> new QuestRepository.Progress(step.id(), step.quest(), step.completes()))
			.toList();
		when(receipts.progress(2)).thenReturn(progress.stream().filter(step -> !step.step().equals("BQ019")).toList());
		var partial = query().state(1);
		var academy = partial.journal().stream().filter(entry -> entry.id().equals("BS03")).findFirst().orElseThrow();
		assertFalse(academy.completed());
		assertEquals(8, academy.completedSteps());
		assertEquals(9, academy.totalSteps());
		assertTrue(academy.tags().stream().anyMatch(tag -> tag.name().equals("引导")));
		assertFalse(partial.completed().contains("BS03"));
		assertFalse(partial.journal().stream().anyMatch(entry -> entry.id().equals("BS04")));
		when(receipts.progress(2)).thenReturn(progress);
		var complete = query().state(1);
		assertTrue(complete.completed().containsAll(List.of("BQ005", "BS03")));
		assertTrue(complete.journal()
			.stream()
			.filter(entry -> entry.id().equals("BS03"))
			.findFirst()
			.orElseThrow()
			.completed());
		assertTrue(complete.journal().stream().anyMatch(entry -> entry.id().equals("BS04")));
	}

}
