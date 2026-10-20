package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 开篇接取和到达目标；四职业捕获、首领与学院连续路线见NaturalExtendedStoryTest。 */
class StoryOpeningQuestRuntimeTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestQueryService questQueries;

	@Test
	void openingRequiresFormalAcceptanceAndNewArrivalAndReplaysWithoutReward() {
		String name = "op_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registrations.register(name, "Opening-test-1234", "开篇测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		// 接取前访问不抵扣本次委托的到达目标。
		move(owner, "PET_CENTER");
		move(owner, "SQUARE");
		var state = questQueries.state(owner);
		var first = state.available().stream().filter(action -> action.id().equals("O001")).findFirst().orElseThrow();
		assertTrue(first.acceptAvailable());
		assertFalse(first.interactable());
		assertTrue(state.completed().isEmpty());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "O001"));
		acceptance.accept(owner, "O001",
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "O001"));
		assertEquals(0, queries.find(owner).experience());
		move(owner, "PET_CENTER");
		var result = quests.interact(owner, "O001");
		assertEquals(100, queries.find(owner).experience());
		assertTrue(questQueries.done(owner, "O001"));
		move(owner, "SQUARE");
		assertEquals(result, quests.interact(owner, "O001"));
		assertEquals(100, queries.find(owner).experience());
	}

	private void move(long owner, String location) {
		characters.move(owner, location, queries.find(owner).revision());
	}

}
