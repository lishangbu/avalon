package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.world.application.model.QuestConversationStage;

import io.github.lishangbu.avalon.AvalonApplication;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;

import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAbandonCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestTrackingCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAbandonRequest;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.QuestTrackingRequest;
import io.github.lishangbu.avalon.world.application.query.QuestAcceptQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestConversationQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestTrackingQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestJournalStateStatus;
import org.springframework.context.annotation.Import;
import io.github.lishangbu.avalon.world.persistence.repository.QuestJournalStateRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestTrackingRepository;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.quest.model.*;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 真实数据库验证接取与完成分离、满包待领、同任务并发一次和回执写失败整体回滚。 */
@SpringBootTest(classes = AvalonApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(QuestProgressFixture.class)
class QuestAcceptanceIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:18.6-trixie");
	static {
		DATABASE.start();
	}
	@MockitoBean(name = "maintenanceStartup")
	ApplicationRunner maintenanceStartup;

	@MockitoSpyBean
	QuestCatalog catalog;

	@MockitoSpyBean
	QuestRulesCatalog rules;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	@Autowired
	QuestConversationQueryService conversations;

	@Autowired
	QuestAbandonCommandService abandonment;

	@Autowired
	QuestTrackingCommandService tracking;

	@Autowired
	QuestTrackingQueryService trackingQueries;

	@Autowired
	QuestTrackingRepository trackingRepository;

	@Autowired
	QuestJournalStateRepository participation;

	@Autowired
	QuestProgressFixture objectives;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestAcceptQueryService acceptQueries;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestQueryService questQueries;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService charactersRead;

	@Autowired
	CharacterFactCommandService factCommands;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	PlatformTransactionManager manager;

	@Autowired
	JdbcTemplate jdbc;

	TestTransactions transactions;

	QuestContentStep step;

	@BeforeEach
	void prepare() {
		transactions = new TestTransactions(manager);
		step = new QuestContentStep("ACCEPT_TEST", "TEST_CAPTURE", "首次捕捉", "SQUARE", "登记捕获", "完成", List.of("O001"),
				true, List.of(new QuestEffect("GOLD", null, 9)),
				List.<String>copyOf(List.of("CAPTURE:TEST"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, "TERLIN", false,
				List.of(new QuestEffect("GIVE_ITEM", "SEAL_I", 5), new QuestEffect("GIVE_ITEM", "HEALTH_POTION", 2),
						new QuestEffect("GIVE_ITEM", "MANA_POTION", 1)),
				new QuestReception("SQUARE", "TERLIN"));
		var definitions = new ArrayList<>(catalog.definition().steps());
		definitions.add(step);
		doReturn(new QuestContentDefinition(definitions)).when(catalog).definition();
		doReturn(step).when(catalog).step(step.id());
		// 引擎要求每项任务具备显式生命周期规则，测试临时内容也遵循正式目录约束。
		doReturn(new QuestRule(step.quest(), "测试委托", QuestType.SIDE, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
			.when(rules)
			.rule(step.quest());
		doReturn(dialogues.step("O001")).when(dialogues).step(step.id());
	}

	/** 阅读同一段对白不会代替接取或交付，也不会向另一名角色泄露进度。 */
	@Test
	void conversationsFollowRealProgressWithoutMutatingRewards() {
		trackingSideQuests();
		long owner = create(true);
		long other = create(true);
		var before = charactersRead.find(owner);
		var inventoryBefore = items.list(owner);
		for (int i = 0; i < 2; i++)
			assertEquals(QuestConversationStage.OFFER,
					conversations.local(owner)
						.stream()
						.filter(value -> value.step().equals(step.id()))
						.findFirst()
						.orElseThrow()
						.stage());
		assertEquals(before, charactersRead.find(owner));
		assertEquals(inventoryBefore, items.list(owner));
		assertFalse(questQueries.done(owner, step.id()));
		var original = request(owner);
		var accepted = acceptance.accept(owner, step.id(), original);
		assertEquals(dialogues.step(step.id()).accepted(), accepted.dialogue());
		assertEquals(QuestConversationStage.PROGRESS,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		abandonment.abandon(owner, step.quest(), new QuestAbandonRequest(0L));
		assertEquals(QuestConversationStage.RESUME,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		assertEquals(dialogues.step(step.id()).resume(),
				acceptance.accept(owner, step.id(), request(owner)).dialogue());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertEquals(QuestConversationStage.READY,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		assertEquals(QuestConversationStage.OFFER,
				conversations.local(other)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		long gold = charactersRead.find(owner).gold();
		assertFalse(questQueries.done(owner, step.id()));
		quests.interact(owner, step.id());
		assertEquals(gold + 9, charactersRead.find(owner).gold());
		assertEquals(accepted, acceptance.accept(owner, step.id(), original));
		conversations.local(owner);
		conversations.local(owner);
		assertEquals(gold + 9, charactersRead.find(owner).gold());
		// 广场另一条可接委托优先交谈；已经完成的本任务不再冒充当前委托。
		assertTrue(conversations.local(owner).stream().noneMatch(value -> value.step().equals(step.id())));
		// 该人物没有后续委托时显示再访，不能拿完成回执伪装成再次交付。
		// 保留已接取定义，仅关闭广场其他入口，符合新引擎不能删除存量任务的部署约束。
		var retained = catalog.definition()
			.steps()
			.stream()
			.filter(value -> value.id().equals(step.id()) || value.quest().equals("O001"))
			.toList();
		doReturn(new QuestContentDefinition(retained)).when(catalog).definition();
		var revisit = conversations.local(owner).getFirst();
		assertEquals(QuestConversationStage.REVISIT, revisit.stage());
		assertEquals(dialogues.step(step.id()).revisit(), revisit.text());
		assertEquals(gold + 9, charactersRead.find(owner).gold());
	}

	@Test
	void deliveryReadinessRequiresCurrentInventoryAndConsumptionIsAtomic() {
		var deliver = new QuestContentStep(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true,
				List.of(new QuestEffect("TAKE_ITEM", "LIFE_ORE_0", 1), new QuestEffect("GOLD", null, 9)),
				step.objectives(), List.of(), QuestDelivery.MANUAL, null, null, step.npcId(), false,
				step.acceptEffects(), new QuestReception(step.location(), step.npcId()));
		var definitions = new ArrayList<>(catalog.definition().steps());
		definitions.removeIf(value -> value.id().equals(step.id()));
		definitions.add(deliver);
		doReturn(new QuestContentDefinition(definitions)).when(catalog).definition();
		doReturn(deliver).when(catalog).step(step.id());
		long owner = create(true);
		acceptance.accept(owner, step.id(), request(owner));
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertFalse(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow()
			.interactable());
		assertEquals(QuestConversationStage.PROGRESS,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		long gold = charactersRead.find(owner).gold();
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		assertEquals(gold, charactersRead.find(owner).gold());
		transactions.run(() -> inventory.grant(owner, "DELIVERY_TEST", "LIFE_ORE_0", 1, false));
		assertEquals(QuestConversationStage.READY,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			quests.interact(owner, step.id());
			throw new IllegalStateException("回滚任务交付");
		}));
		assertEquals(1, amount(owner, "LIFE_ORE_0"));
		assertEquals(gold, charactersRead.find(owner).gold());
		quests.interact(owner, step.id());
		quests.interact(owner, step.id());
		assertEquals(0, amount(owner, "LIFE_ORE_0"));
		assertEquals(gold + 9, charactersRead.find(owner).gold());
	}

	/** 同一观察可以共享，实际交付的库存必须分别消耗；页面旧的可交付状态不能代替事务内复核。 */
	@Test
	void twoQuestsCannotSpendTheSameDeliveryItem() {
		var definitions = new ArrayList<>(catalog.definition().steps());
		for (String id : List.of("DELIVERY_FIRST", "DELIVERY_SECOND")) {
			var delivery = new QuestContentStep(id, id, "石材委托", "SQUARE", "交付", "已收到石材", List.of("O001"), true,
					List.of(new QuestEffect("TAKE_ITEM", "LIFE_ORE_0", 1), new QuestEffect("GOLD", null, 9)),
					List.<String>copyOf(List.of("CAPTURE:TEST"))
						.stream()
						.map(code -> new QuestObjective(code,
								code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
										: QuestMeasure.EVENT,
								code, 1))
						.toList(),
					List.of(), QuestDelivery.MANUAL, null, null, "TERLIN", false, List.of(),
					new QuestReception("SQUARE", "TERLIN"));
			definitions.add(delivery);
			doReturn(delivery).when(catalog).step(id);
			// 两份独立委托使用独立规则，不能借用另一任务的参与状态。
			doReturn(new QuestRule(id, "石材委托", QuestType.SIDE, List.of(), true,
					new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
				.when(rules)
				.rule(id);
			doReturn(dialogues.step("O001")).when(dialogues).step(id);
		}
		doReturn(new QuestContentDefinition(definitions)).when(catalog).definition();
		long owner = create(true);
		acceptance.accept(owner, "DELIVERY_FIRST", request(owner));
		acceptance.accept(owner, "DELIVERY_SECOND", request(owner));
		transactions.run(() -> {
			factCommands.record(owner, "CAPTURE:TEST");
			inventory.grant(owner, "SHARED_DELIVERY", "LIFE_ORE_0", 1, false);
		});
		assertEquals(2,
				questQueries.state(owner)
					.available()
					.stream()
					.filter(value -> value.id().startsWith("DELIVERY_") && value.interactable())
					.count());
		long gold = charactersRead.find(owner).gold();
		quests.interact(owner, "DELIVERY_FIRST");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "DELIVERY_SECOND"));
		assertFalse(questQueries.done(owner, "DELIVERY_SECOND"));
		assertEquals(0, amount(owner, "LIFE_ORE_0"));
		assertEquals(gold + 9, charactersRead.find(owner).gold());
		transactions.run(() -> inventory.grant(owner, "SECOND_DELIVERY", "LIFE_ORE_0", 1, false));
		quests.interact(owner, "DELIVERY_SECOND");
		assertEquals(0, amount(owner, "LIFE_ORE_0"));
		assertEquals(gold + 18, charactersRead.find(owner).gold());
	}

	private long create(boolean prerequisite) {
		String name = "accept_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registration.register(name, "Accept-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		characters.move(owner, "STONE_PATH", charactersRead.find(owner).revision());
		characters.move(owner, "SQUARE", charactersRead.find(owner).revision());
		if (prerequisite) {
			acceptance.accept(owner, "O001", request(owner));
			characters.move(owner, "PET_CENTER", charactersRead.find(owner).revision());
			quests.interact(owner, "O001");
			characters.move(owner, "SQUARE", charactersRead.find(owner).revision());
		}
		return owner;
	}

	private QuestAcceptRequest request(long owner) {
		return new QuestAcceptRequest(UUID.randomUUID().toString(), charactersRead.find(owner).revision());
	}

	private List<QuestProgressGoal> captureGoals(long character) {
		return objectives.goals(character).stream().filter(goal -> goal.step().equals(step.id())).toList();
	}

	private int amount(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

	/** 测试目录增加两条可选委托，验证双槽位不依赖正式主线数量或编号前缀。 */
	private void trackingSideQuests() {
		var second = new QuestContentStep("TRACK_SECOND", "TRACK_SIDE", "另一份观察委托", "SQUARE", "交付", "完成",
				List.of("O001"), true, List.of(),
				List.<String>copyOf(List.of("CAPTURE:TEST"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, "TERLIN", false, List.of(),
				new QuestReception("SQUARE", "TERLIN"));
		var steps = new ArrayList<>(catalog.definition().steps());
		steps.add(second);
		doReturn(new QuestContentDefinition(steps)).when(catalog).definition();
		doReturn(second).when(catalog).step(second.id());
		doReturn(dialogues.step("O001")).when(dialogues).step(second.id());
		var configured = new ArrayList<>(rules.definition().quests());
		for (String id : List.of(step.quest(), second.quest())) {
			var rule = new QuestRule(id, "可选观察", QuestType.SIDE, List.of(), true,
					new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of()));
			configured.add(rule);
			doReturn(rule).when(rules).rule(id);
		}
		doReturn(new QuestRulesDefinition(rules.definition().tags(), configured)).when(rules).definition();
	}

	/** 接取数量与追踪槽位无关，超过常见的50条任务上限仍正常记录各自进度。 */
	@Test
	void moreThanFiftySideQuestsCanRemainActiveAndProgressTogether() {
		var steps = new ArrayList<>(catalog.definition().steps());
		var configured = new ArrayList<>(rules.definition().quests());
		var ids = new ArrayList<String>();
		for (int i = 0; i < 55; i++) {
			String id = "SIDE_CAPACITY_" + i;
			ids.add(id);
			var task = new QuestContentStep(id, id, "观察委托", "SQUARE", "交付", "完成", List.of("O001"), true, List.of(),
					List.<String>copyOf(List.of("CAPTURE:CAPACITY"))
						.stream()
						.map(code -> new QuestObjective(code,
								code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
										: QuestMeasure.EVENT,
								code, 1))
						.toList(),
					List.of(), QuestDelivery.MANUAL, null, null, "TERLIN", false, List.of(),
					new QuestReception("SQUARE", "TERLIN"));
			steps.add(task);
			doReturn(task).when(catalog).step(id);
			doReturn(dialogues.step("O001")).when(dialogues).step(id);
			var rule = new QuestRule(id, "观察委托", QuestType.SIDE, List.of(), true,
					new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of()));
			configured.add(rule);
			doReturn(rule).when(rules).rule(id);
		}
		doReturn(new QuestContentDefinition(steps)).when(catalog).definition();
		doReturn(new QuestRulesDefinition(rules.definition().tags(), configured)).when(rules).definition();
		long owner = create(true);
		for (String id : ids)
			acceptance.accept(owner, id, request(owner));
		long character = charactersRead.requireId(owner);
		assertEquals(55, participation.findByCharacterId(character)
			.stream()
			.filter(value -> ids.contains(value.getQuestCode()) && value.getStatus() == QuestJournalStateStatus.ACTIVE)
			.count());
		assertNull(trackingQueries.state(owner).side());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:CAPACITY"));
		var available = questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> ids.contains(value.id()))
			.toList();
		assertEquals(55, available.size());
		assertTrue(available.stream().allMatch(value -> value.interactable()));
		quests.interact(owner, ids.getLast());
		assertTrue(questQueries.done(owner, ids.getLast()));
		assertFalse(questQueries.done(owner, ids.getFirst()));
	}

	@Test
	void trackingPersistsTwoSlotsWithoutPausingOtherObjectivesAndDoesNotLeakFutureTasks() {
		trackingSideQuests();
		long owner = create(true);
		long character = charactersRead.requireId(owner);
		assertEquals("O002", trackingQueries.state(owner).main());
		assertTrue(trackingRepository.findByCharacterId(character).isEmpty(), "读取默认指引不创建偏好");
		assertThrows(BusinessException.class,
				() -> tracking.select(owner, new QuestTrackingRequest(QuestType.MAIN, "E015", 0L)));
		assertThrows(BusinessException.class,
				() -> tracking.select(owner, new QuestTrackingRequest(QuestType.SIDE, "O002", 0L)));
		acceptance.accept(owner, step.id(), request(owner));
		acceptance.accept(owner, "TRACK_SECOND", request(owner));
		tracking.select(owner, new QuestTrackingRequest(QuestType.SIDE, step.quest(), 0L));
		assertEquals("O002", trackingQueries.state(owner).main());
		assertEquals(step.quest(), trackingQueries.state(owner).side());
		tracking.select(owner, new QuestTrackingRequest(QuestType.SIDE, "TRACK_SIDE", 1L));
		assertEquals("TRACK_SIDE", trackingQueries.state(owner).side());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> List.of(step.id(), "TRACK_SECOND").contains(value.id()))
			.allMatch(value -> value.interactable()), "一次有效行为推进两个已接取任务，未追踪任务不能被暂停");
		tracking.select(owner, new QuestTrackingRequest(QuestType.MAIN, null, 2L));
		assertNull(trackingQueries.state(owner).main(), "显式取消不能被查询自动恢复");
		assertEquals("TRACK_SIDE", trackingQueries.state(owner).side());
		quests.interact(owner, "TRACK_SECOND");
		assertNull(trackingQueries.state(owner).side(), "完成后从地图隐藏");
		assertThrows(BusinessException.class,
				() -> tracking.select(owner, new QuestTrackingRequest(QuestType.SIDE, "TRACK_SIDE", 3L)));
		tracking.select(owner, new QuestTrackingRequest(QuestType.SIDE, step.quest(), 3L));
		abandonment.abandon(owner, step.quest(), new QuestAbandonRequest(0L));
		assertNull(trackingQueries.state(owner).side(), "放弃后不显示正在追踪");
		long other = create(false);
		assertEquals("O001", trackingQueries.state(other).main());
		assertNull(trackingQueries.state(other).side(), "另一个角色不能继承偏好");
	}

	@Test
	void trackingRejectsStaleWritesAndRollsBackWithTheOuterTransaction() throws Exception {
		long owner = create(true);
		var clear = new QuestTrackingRequest(QuestType.MAIN, null, 0L);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var a = executor.submit(() -> {
				start.await();
				tracking.select(owner, clear);
				return true;
			});
			var b = executor.submit(() -> {
				start.await();
				tracking.select(owner, clear);
				return true;
			});
			start.countDown();
			assertTrue(a.get(15, TimeUnit.SECONDS));
			assertTrue(b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(1, trackingQueries.state(owner).revision());
		assertNull(trackingQueries.state(owner).main());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			tracking.select(owner, new QuestTrackingRequest(QuestType.MAIN, "O002", 1L));
			throw new IllegalStateException("模拟外层失败");
		}));
		assertNull(trackingQueries.state(owner).main());
		assertEquals(1, trackingQueries.state(owner).revision());
		tracking.select(owner, new QuestTrackingRequest(QuestType.MAIN, "O002", 1L));
		assertThrows(BusinessException.class, () -> tracking.select(owner, clear));
		assertEquals("O002", trackingQueries.state(owner).main());
	}

	@Test
	void ordinaryAcceptanceAndAbandonmentRespectStateVersionAndCompletion() {
		long owner = create(false);
		long character = Long.parseLong(charactersRead.find(owner).id());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "O001"));
		var original = request(owner);
		acceptance.accept(owner, "O001", original);
		assertFalse(questQueries.state(owner).completed().contains("O001"));
		assertThrows(BusinessException.class, () -> abandonment.abandon(owner, "O001", new QuestAbandonRequest(0L)));
		// 同一通用执行器验证可放弃支线，不将正式主线内容改成可选任务。
		doReturn(new QuestRule("O001", "测试委托", QuestType.SIDE, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
			.when(rules)
			.rule("O001");
		abandonment.abandon(owner, "O001", new QuestAbandonRequest(0L));
		abandonment.abandon(owner, "O001", new QuestAbandonRequest(0L));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "O001"));
		assertFalse(questQueries.state(owner).available().getFirst().accepted());
		acceptance.accept(owner, "O001", original);
		assertEquals(QuestJournalStateStatus.ABANDONED,
				participation.findByCharacterIdAndQuestCode(character, "O001").orElseThrow().getStatus());
		acceptance.accept(owner, "O001", request(owner));
		assertThrows(BusinessException.class, () -> abandonment.abandon(owner, "O001", new QuestAbandonRequest(0L)));
		characters.move(owner, "PET_CENTER", charactersRead.find(owner).revision());
		var result = quests.interact(owner, "O001");
		assertEquals(result, quests.interact(owner, "O001"));
		assertEquals(QuestJournalStateStatus.COMPLETED,
				participation.findByCharacterIdAndQuestCode(character, "O001").orElseThrow().getStatus());
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, "O001", request(owner)));
	}

	@Test
	void separateReceptionProjectsAndEnforcesTheActualPlaceWithoutTeleporting() {
		long owner = create(true);
		var remote = new QuestContentStep(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true, step.effects(), step.objectives(), List.of(),
				QuestDelivery.MANUAL, null, null, step.npcId(), false, step.acceptEffects(),
				new QuestReception("ELDER_HOUSE", "AD"));
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(remote);
		doReturn(new QuestContentDefinition(definition)).when(catalog).definition();
		doReturn(remote).when(catalog).step(step.id());
		var before = questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow();
		assertEquals("ELDER_HOUSE", before.location());
		assertEquals("AD", before.npcId());
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		characters.move(owner, "ELDER_HOUSE", charactersRead.find(owner).revision());
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals("ELDER_HOUSE", charactersRead.find(owner).location(), "接取不能传送到交付地点");
		var after = questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow();
		assertEquals("SQUARE", after.location());
		assertEquals("TERLIN", after.npcId());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		characters.move(owner, "SQUARE", charactersRead.find(owner).revision());
		quests.interact(owner, step.id());
	}

	@Test
	void onlyANewCaptureCountsAfterAcceptanceAndSourceReplayCannotCreditAnotherQuest() {
		long owner = create(true);
		long character = charactersRead.requireId(owner);
		var captureStep = new QuestContentStep(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true, step.effects(),
				List.<String>copyOf(List.of("CAPTURE:WILD"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, step.npcId(), false, step.acceptEffects(),
				new QuestReception(step.location(), step.npcId()));
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(captureStep);
		doReturn(new QuestContentDefinition(definition)).when(catalog).definition();
		doReturn(captureStep).when(catalog).step(step.id());
		long captured = transactions
			.call(() -> creatures.capture(owner, "CAPTURE_BEFORE", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals(captured, transactions
			.call(() -> creatures.capture(owner, "CAPTURE_BEFORE", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹"))));
		assertTrue(captureGoals(character).isEmpty());
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		transactions.run(() -> creatures.capture(owner, "CAPTURE_AFTER", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(1, captureGoals(character).size());
		quests.interact(owner, step.id());
	}

	@Test
	void historicalAndAbandonedActionsDoNotCountAndRecordedProgressSurvivesResume() {
		long owner = create(true);
		long character = charactersRead.requireId(owner);
		var extended = new QuestContentStep(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), step.completes(), step.effects(),
				List.<String>copyOf(List.of("CAPTURE:TEST", "VICTORY:TEST"))
					.stream()
					.map(code -> new QuestObjective(code,
							code.startsWith("ACTION:") || code.startsWith("SKILL:") ? QuestMeasure.HISTORY
									: QuestMeasure.EVENT,
							code, 1))
					.toList(),
				List.of(), QuestDelivery.MANUAL, null, null, step.npcId(), false, step.acceptEffects(),
				new QuestReception(step.location(), step.npcId()));
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(extended);
		doReturn(new QuestContentDefinition(definition)).when(catalog).definition();
		doReturn(extended).when(catalog).step(step.id());
		doReturn(new QuestRule(step.quest(), "测试委托", QuestType.SIDE, List.of(), true,
				new QuestPolicy(QuestRepeatPolicy.ONCE, "Asia/Hong_Kong", 1, 10, null, false, List.of())))
			.when(rules)
			.rule(step.quest());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		acceptance.accept(owner, step.id(), request(owner));
		assertTrue(captureGoals(character).isEmpty(), "接取不能回填历史捕获");
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertEquals(1, captureGoals(character).size(), "新的同类行为必须产生有效进度");
		abandonment.abandon(owner, step.quest(), new QuestAbandonRequest(0L));
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals(1, captureGoals(character).size(), "重接保留已有目标但不补算放弃期间行为");
		assertFalse(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow()
			.interactable());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			factCommands.record(owner, "VICTORY:TEST");
			throw new IllegalStateException("模拟原行为事务回滚");
		}));
		assertEquals(1, captureGoals(character).size());
		long other = create(true);
		transactions.run(() -> factCommands.record(other, "VICTORY:TEST"));
		assertEquals(1, captureGoals(character).size(), "其他角色不共享任务进度");
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		assertEquals(2, captureGoals(character).size());
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow()
			.interactable());
		quests.interact(owner, step.id());
	}

	@Test
	void readDoesNotGrantAndAcceptanceDoesNotCompleteCaptureTask() {
		long owner = create(true);
		var available = questQueries.state(owner)
			.available()
			.stream()
			.filter(action -> action.id().equals(step.id()))
			.findFirst()
			.orElseThrow();
		assertTrue(available.acceptRequired());
		assertTrue(available.acceptAvailable());
		assertFalse(available.accepted());
		assertFalse(available.interactable());
		assertEquals(0, amount(owner, "SEAL_I"));
		assertNull(acceptQueries.acceptedAt(owner, step.id()));
		var request = request(owner);
		var result = acceptance.accept(owner, step.id(), request);
		assertEquals(result, acceptance.accept(owner, step.id(), request));
		var again = acceptance.accept(owner, step.id(), request(owner));
		assertTrue(again.rewards().isEmpty());
		assertEquals(result.acceptedAt(), again.acceptedAt());
		assertEquals(result.acceptedAt(), acceptQueries.acceptedAt(owner, step.id()));
		assertEquals(5, amount(owner, "SEAL_I"));
		assertEquals(2, amount(owner, "HEALTH_POTION"));
		assertEquals(1, amount(owner, "MANA_POTION"));
		assertTrue(facts.codes(owner).contains("QUEST_ACCEPT:" + step.id()));
		assertFalse(facts.codes(owner).contains("QUEST:" + step.id()));
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		long gold = charactersRead.find(owner).gold();
		var completion = quests.interact(owner, step.id());
		assertEquals(completion, quests.interact(owner, step.id()));
		assertEquals(gold + 9, charactersRead.find(owner).gold());
		assertEquals(5, amount(owner, "SEAL_I"));
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		assertEquals(result, acceptance.accept(owner, step.id(), request));
	}

	@Test
	void historicalAcceptanceFactAndOtherRoundReceiptCannotMarkCurrentStepAccepted() {
		long owner = create(true);
		acceptance.accept(owner, step.id(), request(owner));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow()
			.accepted());
		// 将已有回执归入别轮，模拟历史接取仍在而本轮尚未登记的持久化状态；不修改永久事实。
		// 读侧必须和引擎一样核对实例及轮次，不能因永久事实存在而隐藏正式接取入口。
		jdbc.update(
				"update quest_accept_receipt set runtime_round = runtime_round + 1 where character_id = ? and step = ?",
				charactersRead.requireId(owner), step.id());
		assertTrue(facts.codes(owner).contains("QUEST_ACCEPT:" + step.id()));
		var current = questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> value.id().equals(step.id()))
			.findFirst()
			.orElseThrow();
		assertFalse(current.accepted());
		assertTrue(current.acceptAvailable());
		assertFalse(current.interactable());
	}

	@Test
	void gatesRevisionIdentityAndReceiptPayloadAreIndependentOfClientButtons() {
		long owner = create(false);
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		acceptance.accept(owner, "O001", request(owner));
		characters.move(owner, "PET_CENTER", charactersRead.find(owner).revision());
		quests.interact(owner, "O001");
		characters.move(owner, "SQUARE", charactersRead.find(owner).revision());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertEquals("请先接取任务并领取出行补给",
				assertThrows(BusinessException.class, () -> quests.interact(owner, step.id())).getMessage());
		doReturn(false).when(catalog).acceptanceAvailable(ArgumentMatchers.eq(step), ArgumentMatchers.any());
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		doCallRealMethod().when(catalog).acceptanceAvailable(ArgumentMatchers.eq(step), ArgumentMatchers.any());
		var original = request(owner);
		var result = acceptance.accept(owner, step.id(), original);
		characters.move(owner, "STONE_PATH", charactersRead.find(owner).revision());
		assertEquals(result, acceptance.accept(owner, step.id(), original));
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, "OTHER", original));
		characters.move(owner, "VILLAGE_HOME", charactersRead.find(owner).revision());
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(),
				new QuestAcceptRequest(UUID.randomUUID().toString(), original.expectedRevision())));
		long other = create(false);
		assertThrows(BusinessException.class, () -> acceptance.accept(other, step.id(),
				new QuestAcceptRequest(original.requestId(), charactersRead.find(other).revision())));
		assertEquals(0, amount(other, "SEAL_I"));
	}

	@Test
	void concurrentFreshRequestsGiveOneSupplyBatch() throws Exception {
		long owner = create(true);
		var first = request(owner);
		var second = request(owner);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var a = executor.submit(() -> {
				start.await();
				return acceptance.accept(owner, step.id(), first);
			});
			var b = executor.submit(() -> {
				start.await();
				return acceptance.accept(owner, step.id(), second);
			});
			start.countDown();
			var left = a.get(15, TimeUnit.SECONDS);
			var right = b.get(15, TimeUnit.SECONDS);
			assertEquals(3, left.rewards().size() + right.rewards().size());
			assertEquals(left.acceptedAt(), right.acceptedAt());
		}
		assertEquals(5, amount(owner, "SEAL_I"));
	}

	@Test
	void fullBagUsesPendingWithoutRegrantingOnNewRequest() {
		long owner = create(true);
		transactions.run(() -> {
			for (int i = 0; i < 60; i++)
				inventory.grant(owner, "FULL:" + i, "STARTER_BODY", 1, true);
		});
		acceptance.accept(owner, step.id(), request(owner));
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals(0, amount(owner, "SEAL_I"));
		assertEquals(8, items.pending(owner).stream().mapToInt(item -> item.quantity()).sum());
		assertTrue(facts.codes(owner).contains("QUEST_ACCEPT:" + step.id()));
	}

	@Test
	void realReceiptFailureRollsBackSuppliesAndAcceptanceTimestamp() {
		long owner = create(true);
		var request = request(owner);
		jdbc.execute("alter table quest_accept_receipt add constraint test_accept_failure check (false) not valid");
		try {
			assertThrows(RuntimeException.class, () -> acceptance.accept(owner, step.id(), request));
		}
		finally {
			jdbc.execute("alter table quest_accept_receipt drop constraint test_accept_failure");
		}
		assertEquals(0, amount(owner, "SEAL_I"));
		assertFalse(facts.codes(owner).contains("QUEST_ACCEPT:" + step.id()));
		assertNull(acceptQueries.acceptedAt(owner, step.id()));
		assertTrue(acceptance.accept(owner, step.id(), request).accepted());
		assertEquals(5, amount(owner, "SEAL_I"));
	}

}
