package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.SceneActionQueryService;
import io.github.lishangbu.avalon.world.persistence.repository.SceneActionReceiptRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 正式内容中资料只读，不点观察也能交谈推进；旧观察请求不能写事实或产生回执。 */
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
	CharacterFactCommandService factCommands;

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

	@Autowired
	EntityManager em;

	@Test
	void facilityInformationIsReadOnlyAndTaskDoesNotRequireObservation() {
		String name = "info_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Info-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		characters.move(owner, "STONE_PATH", queries.find(owner).revision());
		characters.move(owner, "RIVERSIDE", queries.find(owner).revision());
		assertTrue(scenes.state(owner).information().isEmpty(), "后续任务资料仍遵守剧情门槛");
		transactions.run(() -> {
			factCommands.record(owner, "QUEST:BQ005");
			long id = characters.lock(owner).id();
			em.createNativeQuery(
					"INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)"
							+ " VALUES (:receipt, :id, 'BQ005', 'BQ005', true, '{}', now())")
				.setParameter("receipt", -id)
				.setParameter("id", id)
				.executeUpdate();
		});
		var before = queries.find(owner);
		var beforeFacts = facts.codes(owner);
		var state = scenes.state(owner);
		assertEquals(2, state.information().size());
		assertTrue(state.actions().isEmpty());
		assertEquals(state, scenes.state(owner));
		assertEquals(before, queries.find(owner));
		assertEquals(beforeFacts, facts.codes(owner));
		var request = new SceneActionCommandService.Request(UUID.randomUUID().toString(), before.revision(), null);
		assertThrows(BusinessException.class, () -> sceneCommands.execute(owner, "BQ006_STONE_TRACE_A", request));
		assertNull(transactions.call(() -> receipts.prior(queries.requireId(owner), request.requestId())));
		assertEquals(beforeFacts, facts.codes(owner));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ006"), "资料不能代替接取任务");
		acceptance.accept(owner, "BQ006", new QuestAcceptRequest(UUID.randomUUID().toString(), before.revision()));
		var result = quests.interact(owner, "BQ006");
		assertEquals(result, quests.interact(owner, "BQ006"));
		assertTrue(facts.codes(owner).contains("QUEST:BQ006"));
		assertFalse(facts.codes(owner).contains("SCENE:BQ006_STONE_TRACE_A"));
		assertFalse(facts.codes(owner).contains("SCENE:BQ006_STONE_TRACE_B"));
		assertEquals(before.experience() + 150, queries.find(owner).experience());
	}

}
