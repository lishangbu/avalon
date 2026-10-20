package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.application.query.SceneActionQueryService;
import io.github.lishangbu.avalon.world.persistence.repository.SceneActionReceiptRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 新开篇查询无副作用，退役观察入口不能写事实或产生回执。 */
class ObservationRemovalTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	SceneActionQueryService scenes;

	@Autowired
	SceneActionCommandService sceneCommands;

	@Autowired
	SceneActionReceiptRepository receipts;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestCommandService quests;

	@Test
	void openingQueriesAreReadOnlyAndRetiredObservationCannotProduceProgress() {
		String name = "info_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Info-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		characters.move(owner, "STONE_PATH", queries.find(owner).revision());
		characters.move(owner, "SQUARE", queries.find(owner).revision());
		var before = queries.find(owner);
		var beforeFacts = facts.codes(owner);
		var state = scenes.state(owner);
		assertTrue(state.information().isEmpty());
		assertEquals(state, scenes.state(owner));
		assertEquals(before, queries.find(owner));
		assertEquals(beforeFacts, facts.codes(owner));
		var request = new SceneActionRequest(UUID.randomUUID().toString(), before.revision(), null);
		assertThrows(BusinessException.class, () -> sceneCommands.execute(owner, "INVALID_SCENE_A", request));
		assertNull(transactions.call(() -> receipts.prior(queries.requireId(owner), request.requestId())));
		assertEquals(beforeFacts, facts.codes(owner));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "O001"), "查询不能代替接取任务");
		acceptance.accept(owner, "O001", new QuestAcceptRequest(UUID.randomUUID().toString(), before.revision()));
		characters.move(owner, "PET_CENTER", queries.find(owner).revision());
		var result = quests.interact(owner, "O001");
		assertEquals(result, quests.interact(owner, "O001"));
		assertTrue(facts.codes(owner).contains("QUEST:O001"));
		assertFalse(facts.codes(owner).contains("SCENE:INVALID_SCENE_A"));
		assertFalse(facts.codes(owner).contains("SCENE:INVALID_SCENE_B"));
		assertEquals(before.experience() + 100, queries.find(owner).experience());
	}

}
