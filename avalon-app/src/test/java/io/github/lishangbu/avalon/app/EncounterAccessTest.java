package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.query.EncounterAccess;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.battle.model.BattleEncounter;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 入口与命令的公共策略必须遵守当前故事，并允许准备完成后真实开战。 */
class EncounterAccessTest {

	@Test
	void regionalEncountersUseTheirValidatedQuestPrerequisite() {
		var quests = mock(QuestQueryService.class);
		var access = new EncounterAccess(quests);
		var encounter = mock(BattleEncounter.class);
		when(encounter.id()).thenReturn("WOODS");
		when(encounter.requires()).thenReturn("O006");
		assertFalse(access.allowed(1, encounter));
		when(quests.done(1, "O006")).thenReturn(true);
		assertTrue(access.allowed(1, encounter));
	}

	@Test
	void caveProvidesInformationWithoutObservationCommands() {
		var quests = mock(QuestQueryService.class);
		var facts = mock(CharacterFactQueryService.class);
		var encounter = mock(BattleEncounter.class);
		when(encounter.id()).thenReturn("OPENING_CAVE_BOSS");
		when(encounter.requires()).thenReturn("O008");
		var access = new EncounterAccess(quests);
		assertFalse(access.allowed(1, encounter));
		when(quests.done(1, "O008")).thenReturn(true);
		assertTrue(access.allowed(1, encounter));
		verifyNoInteractions(facts);
	}

}
