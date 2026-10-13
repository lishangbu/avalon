package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.AvalonApplication;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.content.world.SceneActionCatalog;
import io.github.lishangbu.avalon.content.world.model.SceneAction;
import io.github.lishangbu.avalon.content.world.model.SceneActionChoice;
import io.github.lishangbu.avalon.content.world.model.SceneActionType;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionView;
import io.github.lishangbu.avalon.world.application.query.SceneActionQueryService;
import io.github.lishangbu.avalon.world.persistence.repository.SceneActionReceiptRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 隔离真实PostgreSQL验证角色锁、回执与资产原子性；MOCK上下文不启动HTTP监听。 */
@SpringBootTest(classes = AvalonApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class SceneActionIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18.6-trixie");
	static {
		DATABASE.start();
	}

	@MockitoBean(name = "maintenanceStartup")
	ApplicationRunner maintenanceStartup;

	@MockitoSpyBean
	SceneActionCatalog catalog;

	@MockitoSpyBean
	QuestCatalog quests;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	/** 测试临时任务使用显式生命周期规则，保持与正式内容的加载要求一致。 */
	@MockitoSpyBean
	QuestRulesCatalog questRules;

	@Autowired
	QuestCommandService questCommands;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	SceneActionReceiptRepository receipts;

	@Autowired
	SceneActionCommandService commands;

	@Autowired
	SceneActionQueryService scenes;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	EntityManager em;

	@Autowired
	PlatformTransactionManager manager;

	TestTransactions transactions;

	private SceneAction action(String id, SceneActionType type, List<String> requires) {
		boolean report = type == SceneActionType.PRESENT_RECORD;
		return new SceneAction(id, type, "VILLAGE_HOME", "照护", "现场", requires, null, null,
				type == SceneActionType.COLLECT_TASK ? "QUEST_WOOD_SWORD" : null,
				type == SceneActionType.COLLECT_TASK ? 1 : 0,
				report ? List.of(new SceneActionChoice("YES", "保留巢域"), new SceneActionChoice("NO", "拆掉巢域")) : List.of(),
				report ? "YES" : null, report ? List.of("YES") : List.of(), "现场完成", report ? "请重看记录" : null, 0);
	}

	@BeforeEach
	void prepare() {
		transactions = new TestTransactions(manager);
		var actions = List.of(action("LOOK", SceneActionType.CARE, List.of()),
				action("GET", SceneActionType.COLLECT_TASK, List.of("SCENE:LOOK")),
				action("CARE", SceneActionType.CARE, List.of("SCENE:GET")),
				action("REPORT", SceneActionType.PRESENT_RECORD, List.of("SCENE:CARE")));
		doReturn(actions).when(catalog).actions();
		// 只替换只读内容，资格判断、角色锁、事实、仓库和回执仍执行正式实现。
	}

	private long owner() {
		String name = "scene_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registration.register(name, "Scene-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private SceneActionRequest request(long owner, String choice) {
		return new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), choice);
	}

	@Test
	void realActionsWrongAnswerCorrectionAndReview() {
		long owner = owner();
		assertThrows(BusinessException.class, () -> commands.execute(owner, "GET", request(owner, null)));
		assertEquals(List.of("LOOK"), scenes.state(owner).actions().stream().map(SceneActionView::id).toList());
		commands.execute(owner, "LOOK", request(owner, null));
		commands.execute(owner, "GET", request(owner, null));
		commands.execute(owner, "CARE", request(owner, null));
		var wrong = request(owner, "NO");
		assertFalse(commands.execute(owner, "REPORT", wrong).completed());
		assertFalse(facts.codes(owner).contains("SCENE:REPORT"));
		assertTrue(commands.execute(owner, "REPORT", request(owner, "YES")).completed());
		assertFalse(commands.execute(owner, "REPORT", wrong).completed(), "错误请求原样重放，不能被后来正确答案覆盖");
		assertTrue(commands.execute(owner, "REPORT", request(owner, null)).completed());
		assertEquals(0, commands.execute(owner, "GET", request(owner, null)).quantity());
		assertEquals(1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("QUEST_WOOD_SWORD"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void movementRevisionOwnershipAndPayloadCannotBeBypassed() {
		long owner = owner();
		var original = request(owner, null);
		var saved = commands.execute(owner, "LOOK", original);
		characters.move(owner, "STONE_PATH", queries.find(owner).revision());
		assertEquals(saved, commands.execute(owner, "LOOK", original));
		assertThrows(BusinessException.class, () -> commands.execute(owner, "GET", original));
		assertThrows(BusinessException.class, () -> commands.execute(owner, "LOOK", request(owner, null)));
		characters.move(owner, "VILLAGE_HOME", queries.find(owner).revision());
		assertThrows(BusinessException.class, () -> commands.execute(owner, "LOOK",
				new SceneActionRequest(UUID.randomUUID().toString(), original.expectedRevision(), null)));
		long other = owner();
		assertThrows(BusinessException.class, () -> commands.execute(other, "GET",
				new SceneActionRequest(original.requestId(), queries.find(other).revision(), null)));
		assertFalse(facts.codes(other).contains("SCENE:LOOK"));
	}

	@Test
	void concurrentDifferentRequestsGiveOneTaskItem() throws Exception {
		long owner = owner();
		commands.execute(owner, "LOOK", request(owner, null));
		var first = request(owner, null);
		var second = request(owner, null);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var a = executor.submit(() -> {
				start.await();
				return commands.execute(owner, "GET", first);
			});
			var b = executor.submit(() -> {
				start.await();
				return commands.execute(owner, "GET", second);
			});
			start.countDown();
			assertEquals(1, a.get(15, TimeUnit.SECONDS).quantity() + b.get(15, TimeUnit.SECONDS).quantity());
		}
		assertEquals(1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("QUEST_WOOD_SWORD"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void receiptFailureRollsBackItemAndFactThenSameRequestCanRetry() {
		long owner = owner();
		commands.execute(owner, "LOOK", request(owner, null));
		var request = request(owner, null);
		transactions.run(() -> em.createNativeQuery(
				"alter table scene_action_receipt add constraint test_scene_failure check (payload not like '%GET%') not valid")
			.executeUpdate());
		try {
			assertThrows(RuntimeException.class, () -> commands.execute(owner, "GET", request));
		}
		finally {
			transactions
				.run(() -> em.createNativeQuery("alter table scene_action_receipt drop constraint test_scene_failure")
					.executeUpdate());
		}
		assertFalse(facts.codes(owner).contains("SCENE:GET"));
		assertTrue(inventory.list(owner).isEmpty());
		assertNull(transactions.call(() -> receipts.prior(queries.requireId(owner), request.requestId())));
		assertEquals(1, commands.execute(owner, "GET", request).quantity());
	}

	@Test
	void environmentalQuestNeedsActualSceneFactsAndRewardsOnce() {
		long owner = owner();
		var step = new QuestContentStep("BQ_ENV_TEST", "BQ_TEST", "现场成果", "VILLAGE_HOME", "确认照护", "已经记录", List.of(),
				true, List.of(new QuestEffect("GOLD", null, 7)),
				List.<String>copyOf(List.of("SCENE:LOOK"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, null, true, List.of(),
				new QuestReception("VILLAGE_HOME", null));
		doReturn(step).when(quests).step("BQ_ENV_TEST");
		doReturn(new QuestRule(step.quest(), "现场成果", QuestType.SIDE, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
			.when(questRules)
			.rule(step.quest());
		doReturn(new QuestContentDefinition(List.of(step))).when(quests).definition();
		long gold = queries.find(owner).gold();
		assertThrows(BusinessException.class, () -> questCommands.interact(owner, "BQ_ENV_TEST"));
		assertEquals(gold, queries.find(owner).gold());
		assertTrue(scenes.state(owner).actions().isEmpty());
		assertThrows(BusinessException.class, () -> commands.execute(owner, "LOOK", request(owner, null)));
		doReturn(dialogues.step("BQ006")).when(dialogues).step(step.id());
		acceptance.accept(owner, step.id(),
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		commands.execute(owner, "LOOK", request(owner, null));
		var result = questCommands.interact(owner, "BQ_ENV_TEST");
		assertEquals(result, questCommands.interact(owner, "BQ_ENV_TEST"));
		assertEquals(gold + 7, queries.find(owner).gold());
		assertTrue(facts.codes(owner).contains("QUEST:BQ_ENV_TEST"));
	}

}
