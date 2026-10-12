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
		var access = new EncounterAccess(quests, mock(CharacterFactQueryService.class));
		var encounter = mock(BattleEncounter.class);
		when(encounter.id()).thenReturn("WOODS");
		when(encounter.requires()).thenReturn("BQ009");
		assertFalse(access.allowed(1, encounter));
		when(quests.done(1, "BQ009")).thenReturn(true);
		assertTrue(access.allowed(1, encounter));
	}

	@Test
	void brightCaveProvidesInformationWithoutObservationCommands() {
		var quests = mock(QuestQueryService.class);
		var facts = mock(CharacterFactQueryService.class);
		var encounter = mock(BattleEncounter.class);
		when(encounter.id()).thenReturn("CAVE_BOSS");
		var access = new EncounterAccess(quests, facts);
		assertFalse(access.allowed(1, encounter));
		when(quests.done(1, "BQ010")).thenReturn(true);
		assertTrue(access.allowed(1, encounter));
		verifyNoInteractions(facts);
	}

}
