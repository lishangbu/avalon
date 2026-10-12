package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.AvalonApplication;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
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
import io.github.lishangbu.avalon.world.application.model.QuestConversation.Stage;
import io.github.lishangbu.avalon.world.application.query.QuestAcceptQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestConversationQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.QuestTrackingQueryService;
import io.github.lishangbu.avalon.world.persistence.entity.QuestParticipation;
import io.github.lishangbu.avalon.world.persistence.repository.QuestObjectiveRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestParticipationRepository;
import io.github.lishangbu.avalon.world.persistence.repository.QuestTrackingRepository;
import java.util.ArrayList;
import java.util.List;
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
	QuestParticipationRepository participation;

	@Autowired
	QuestObjectiveRepository objectives;

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

	QuestCatalog.Step step;

	@BeforeEach
	void prepare() {
		transactions = new TestTransactions(manager);
		step = new QuestCatalog.Step("ACCEPT_TEST", "BQ007_TEST", "首次捕捉", "SQUARE", "登记捕获", "完成", List.of("BQ001"),
				true, List.of(new QuestCatalog.Effect(QuestCatalog.Kind.GOLD, null, 9)), null, List.of("CAPTURE:TEST"),
				0, false, "TERLIN", false,
				List.of(new QuestCatalog.Effect(QuestCatalog.Kind.GIVE_ITEM, "SEAL_I", 5),
						new QuestCatalog.Effect(QuestCatalog.Kind.GIVE_ITEM, "HEALTH_POTION", 2),
						new QuestCatalog.Effect(QuestCatalog.Kind.GIVE_ITEM, "MANA_POTION", 1)),
				null);
		var definitions = new ArrayList<>(catalog.definition().steps());
		definitions.add(step);
		doReturn(new QuestCatalog.Definition(definitions)).when(catalog).definition();
		doReturn(step).when(catalog).step(step.id());
		doReturn(dialogues.step("BQ007")).when(dialogues).step(step.id());
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
			assertEquals(Stage.OFFER,
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
		assertEquals(Stage.PROGRESS,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		abandonment.abandon(owner, step.quest(), new QuestAbandonCommandService.Request(0L));
		assertEquals(Stage.RESUME,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		assertEquals(dialogues.step(step.id()).resume(),
				acceptance.accept(owner, step.id(), request(owner)).dialogue());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertEquals(Stage.READY,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		assertEquals(Stage.OFFER,
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
		doReturn(new QuestCatalog.Definition(List.of(step))).when(catalog).definition();
		var revisit = conversations.local(owner).getFirst();
		assertEquals(Stage.REVISIT, revisit.stage());
		assertEquals(dialogues.step(step.id()).revisit(), revisit.text());
		assertEquals(gold + 9, charactersRead.find(owner).gold());
	}

	@Test
	void deliveryReadinessRequiresCurrentInventoryAndConsumptionIsAtomic() {
		var deliver = new QuestCatalog.Step(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true,
				List.of(new QuestCatalog.Effect(QuestCatalog.Kind.TAKE_ITEM, "BQ_FESTIVAL_STONES", 1),
						new QuestCatalog.Effect(QuestCatalog.Kind.GOLD, null, 9)),
				null, step.facts(), 0, false, step.npcId(), false, step.acceptEffects(), null);
		var definitions = new ArrayList<>(catalog.definition().steps());
		definitions.removeIf(value -> value.id().equals(step.id()));
		definitions.add(deliver);
		doReturn(new QuestCatalog.Definition(definitions)).when(catalog).definition();
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
		assertEquals(Stage.PROGRESS,
				conversations.local(owner)
					.stream()
					.filter(value -> value.step().equals(step.id()))
					.findFirst()
					.orElseThrow()
					.stage());
		long gold = charactersRead.find(owner).gold();
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		assertEquals(gold, charactersRead.find(owner).gold());
		transactions.run(() -> inventory.grant(owner, "DELIVERY_TEST", "BQ_FESTIVAL_STONES", 1, false));
		assertEquals(Stage.READY,
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
		assertEquals(1, amount(owner, "BQ_FESTIVAL_STONES"));
		assertEquals(gold, charactersRead.find(owner).gold());
		quests.interact(owner, step.id());
		quests.interact(owner, step.id());
		assertEquals(0, amount(owner, "BQ_FESTIVAL_STONES"));
		assertEquals(gold + 9, charactersRead.find(owner).gold());
	}

	/** 同一观察可以共享，实际交付的库存必须分别消耗；页面旧的可交付状态不能代替事务内复核。 */
	@Test
	void twoQuestsCannotSpendTheSameDeliveryItem() {
		var definitions = new ArrayList<>(catalog.definition().steps());
		for (String id : List.of("DELIVERY_FIRST", "DELIVERY_SECOND")) {
			var delivery = new QuestCatalog.Step(id, id, "石材委托", "SQUARE", "交付", "已收到石材", List.of("BQ001"), true,
					List.of(new QuestCatalog.Effect(QuestCatalog.Kind.TAKE_ITEM, "BQ_FESTIVAL_STONES", 1),
							new QuestCatalog.Effect(QuestCatalog.Kind.GOLD, null, 9)),
					null, List.of("CAPTURE:TEST"), 0, false, "TERLIN", false, List.of(), null);
			definitions.add(delivery);
			doReturn(delivery).when(catalog).step(id);
			doReturn(dialogues.step("BQ007")).when(dialogues).step(id);
		}
		doReturn(new QuestCatalog.Definition(definitions)).when(catalog).definition();
		long owner = create(true);
		acceptance.accept(owner, "DELIVERY_FIRST", request(owner));
		acceptance.accept(owner, "DELIVERY_SECOND", request(owner));
		transactions.run(() -> {
			factCommands.record(owner, "CAPTURE:TEST");
			inventory.grant(owner, "SHARED_DELIVERY", "BQ_FESTIVAL_STONES", 1, false);
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
		assertEquals(0, amount(owner, "BQ_FESTIVAL_STONES"));
		assertEquals(gold + 9, charactersRead.find(owner).gold());
		transactions.run(() -> inventory.grant(owner, "SECOND_DELIVERY", "BQ_FESTIVAL_STONES", 1, false));
		quests.interact(owner, "DELIVERY_SECOND");
		assertEquals(0, amount(owner, "BQ_FESTIVAL_STONES"));
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
			acceptance.accept(owner, "BQ001", request(owner));
			quests.interact(owner, "BQ001");
		}
		return owner;
	}

	private QuestAcceptCommandService.Request request(long owner) {
		return new QuestAcceptCommandService.Request(UUID.randomUUID().toString(),
				charactersRead.find(owner).revision());
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
		var second = new QuestCatalog.Step("TRACK_SECOND", "TRACK_SIDE", "另一份观察委托", "SQUARE", "交付", "完成",
				List.of("BQ001"), true, List.of(), null, List.of("CAPTURE:TEST"), 0, false, "TERLIN", false, List.of(),
				null);
		var steps = new ArrayList<>(catalog.definition().steps());
		steps.add(second);
		doReturn(new QuestCatalog.Definition(steps)).when(catalog).definition();
		doReturn(second).when(catalog).step(second.id());
		doReturn(dialogues.step("BQ007")).when(dialogues).step(second.id());
		var configured = new ArrayList<>(rules.definition().quests());
		for (String id : List.of(step.quest(), second.quest())) {
			var rule = new QuestRulesCatalog.Rule(id, "可选观察", QuestRulesCatalog.Type.SIDE, List.of(),
					QuestRulesCatalog.RepeatPolicy.ONCE, true);
			configured.add(rule);
			doReturn(rule).when(rules).rule(id);
		}
		doReturn(new QuestRulesCatalog.Definition(rules.definition().tags(), configured)).when(rules).definition();
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
			var task = new QuestCatalog.Step(id, id, "观察委托", "SQUARE", "交付", "完成", List.of("BQ001"), true, List.of(),
					null, List.of("CAPTURE:CAPACITY"), 0, false, "TERLIN", false, List.of(), null);
			steps.add(task);
			doReturn(task).when(catalog).step(id);
			doReturn(dialogues.step("BQ007")).when(dialogues).step(id);
			var rule = new QuestRulesCatalog.Rule(id, "观察委托", QuestRulesCatalog.Type.SIDE, List.of(),
					QuestRulesCatalog.RepeatPolicy.ONCE, true);
			configured.add(rule);
			doReturn(rule).when(rules).rule(id);
		}
		doReturn(new QuestCatalog.Definition(steps)).when(catalog).definition();
		doReturn(new QuestRulesCatalog.Definition(rules.definition().tags(), configured)).when(rules).definition();
		long owner = create(true);
		for (String id : ids)
			acceptance.accept(owner, id, request(owner));
		long character = charactersRead.requireId(owner);
		assertEquals(55,
				participation.findByCharacterId(character)
					.stream()
					.filter(value -> ids.contains(value.getQuestCode())
							&& value.getStatus() == QuestParticipation.Status.ACTIVE)
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
		assertEquals("BQ002", trackingQueries.state(owner).main());
		assertTrue(trackingRepository.findByCharacterId(character).isEmpty(), "读取默认指引不创建偏好");
		assertThrows(BusinessException.class, () -> tracking.select(owner,
				new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.MAIN, "BS08", 0L)));
		assertThrows(BusinessException.class, () -> tracking.select(owner,
				new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.SIDE, "BQ002", 0L)));
		acceptance.accept(owner, step.id(), request(owner));
		acceptance.accept(owner, "TRACK_SECOND", request(owner));
		tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.SIDE, step.quest(), 0L));
		assertEquals("BQ002", trackingQueries.state(owner).main());
		assertEquals(step.quest(), trackingQueries.state(owner).side());
		tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.SIDE, "TRACK_SIDE", 1L));
		assertEquals("TRACK_SIDE", trackingQueries.state(owner).side());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(value -> List.of(step.id(), "TRACK_SECOND").contains(value.id()))
			.allMatch(value -> value.interactable()), "一次有效行为推进两个已接取任务，未追踪任务不能被暂停");
		tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.MAIN, null, 2L));
		assertNull(trackingQueries.state(owner).main(), "显式取消不能被查询自动恢复");
		assertEquals("TRACK_SIDE", trackingQueries.state(owner).side());
		quests.interact(owner, "TRACK_SECOND");
		assertNull(trackingQueries.state(owner).side(), "完成后从地图隐藏");
		assertThrows(BusinessException.class, () -> tracking.select(owner,
				new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.SIDE, "TRACK_SIDE", 3L)));
		tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.SIDE, step.quest(), 3L));
		abandonment.abandon(owner, step.quest(), new QuestAbandonCommandService.Request(0L));
		assertNull(trackingQueries.state(owner).side(), "放弃后不显示正在追踪");
		long other = create(false);
		assertEquals("BQ001", trackingQueries.state(other).main());
		assertNull(trackingQueries.state(other).side(), "另一个角色不能继承偏好");
	}

	@Test
	void trackingRejectsStaleWritesAndRollsBackWithTheOuterTransaction() throws Exception {
		long owner = create(true);
		var clear = new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.MAIN, null, 0L);
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
			tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.MAIN, "BQ002", 1L));
			throw new IllegalStateException("模拟外层失败");
		}));
		assertNull(trackingQueries.state(owner).main());
		assertEquals(1, trackingQueries.state(owner).revision());
		tracking.select(owner, new QuestTrackingCommandService.Request(QuestRulesCatalog.Type.MAIN, "BQ002", 1L));
		assertThrows(BusinessException.class, () -> tracking.select(owner, clear));
		assertEquals("BQ002", trackingQueries.state(owner).main());
	}

	@Test
	void ordinaryAcceptanceAndAbandonmentRespectStateVersionAndCompletion() {
		long owner = create(false);
		long character = Long.parseLong(charactersRead.find(owner).id());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ001"));
		var original = request(owner);
		acceptance.accept(owner, "BQ001", original);
		assertFalse(questQueries.state(owner).completed().contains("BQ001"));
		assertThrows(BusinessException.class,
				() -> abandonment.abandon(owner, "BQ001", new QuestAbandonCommandService.Request(0L)));
		// 同一通用执行器验证可放弃支线，不将正式主线内容改成可选任务。
		doReturn(new QuestRulesCatalog.Rule("BQ001", "测试委托", QuestRulesCatalog.Type.SIDE, List.of(),
				QuestRulesCatalog.RepeatPolicy.ONCE, true))
			.when(rules)
			.rule("BQ001");
		abandonment.abandon(owner, "BQ001", new QuestAbandonCommandService.Request(0L));
		abandonment.abandon(owner, "BQ001", new QuestAbandonCommandService.Request(0L));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ001"));
		assertFalse(questQueries.state(owner).available().getFirst().accepted());
		acceptance.accept(owner, "BQ001", original);
		assertEquals(QuestParticipation.Status.ABANDONED,
				participation.findByCharacterIdAndQuestCode(character, "BQ001").orElseThrow().getStatus());
		acceptance.accept(owner, "BQ001", request(owner));
		assertThrows(BusinessException.class,
				() -> abandonment.abandon(owner, "BQ001", new QuestAbandonCommandService.Request(0L)));
		var result = quests.interact(owner, "BQ001");
		assertEquals(result, quests.interact(owner, "BQ001"));
		assertEquals(QuestParticipation.Status.COMPLETED,
				participation.findByCharacterIdAndQuestCode(character, "BQ001").orElseThrow().getStatus());
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, "BQ001", request(owner)));
	}

	@Test
	void separateReceptionProjectsAndEnforcesTheActualPlaceWithoutTeleporting() {
		long owner = create(true);
		var remote = new QuestCatalog.Step(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true, step.effects(), null, step.facts(), 0, false, step.npcId(),
				false, step.acceptEffects(), new QuestCatalog.Reception("ELDER_HOUSE", "AD"));
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(remote);
		doReturn(new QuestCatalog.Definition(definition)).when(catalog).definition();
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
		var captureStep = new QuestCatalog.Step(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), true, step.effects(), null, List.of("CAPTURE:WILD"), 0, false,
				step.npcId(), false, step.acceptEffects(), null);
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(captureStep);
		doReturn(new QuestCatalog.Definition(definition)).when(catalog).definition();
		doReturn(captureStep).when(catalog).step(step.id());
		long captured = transactions
			.call(() -> creatures.capture(owner, "CAPTURE_BEFORE", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals(captured, transactions
			.call(() -> creatures.capture(owner, "CAPTURE_BEFORE", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹"))));
		assertTrue(objectives.findByCharacterId(character).isEmpty());
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		transactions.run(() -> creatures.capture(owner, "CAPTURE_AFTER", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertEquals(1, objectives.findByCharacterId(character).size());
		quests.interact(owner, step.id());
	}

	@Test
	void historicalAndAbandonedActionsDoNotCountAndRecordedProgressSurvivesResume() {
		long owner = create(true);
		long character = charactersRead.requireId(owner);
		var extended = new QuestCatalog.Step(step.id(), step.quest(), step.title(), step.location(), step.label(),
				step.dialogue(), step.requires(), step.completes(), step.effects(), null,
				List.of("CAPTURE:TEST", "VICTORY:TEST"), 0, false, step.npcId(), false, step.acceptEffects(), null);
		var definition = new ArrayList<>(catalog.definition().steps());
		definition.removeIf(value -> value.id().equals(step.id()));
		definition.add(extended);
		doReturn(new QuestCatalog.Definition(definition)).when(catalog).definition();
		doReturn(extended).when(catalog).step(step.id());
		doReturn(new QuestRulesCatalog.Rule(step.quest(), "测试委托", QuestRulesCatalog.Type.SIDE, List.of(),
				QuestRulesCatalog.RepeatPolicy.ONCE, true))
			.when(rules)
			.rule(step.quest());
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		acceptance.accept(owner, step.id(), request(owner));
		assertTrue(objectives.findByCharacterId(character).isEmpty(), "接取不能回填历史捕获");
		assertThrows(BusinessException.class, () -> quests.interact(owner, step.id()));
		transactions.run(() -> factCommands.record(owner, "CAPTURE:TEST"));
		assertEquals(1, objectives.findByCharacterId(character).size(), "新的同类行为必须产生有效进度");
		abandonment.abandon(owner, step.quest(), new QuestAbandonCommandService.Request(0L));
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		acceptance.accept(owner, step.id(), request(owner));
		assertEquals(1, objectives.findByCharacterId(character).size(), "重接保留已有目标但不补算放弃期间行为");
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
		assertEquals(1, objectives.findByCharacterId(character).size());
		long other = create(true);
		transactions.run(() -> factCommands.record(other, "VICTORY:TEST"));
		assertEquals(1, objectives.findByCharacterId(character).size(), "其他角色不共享任务进度");
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		transactions.run(() -> factCommands.record(owner, "VICTORY:TEST"));
		assertEquals(2, objectives.findByCharacterId(character).size());
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
	void gatesRevisionIdentityAndReceiptPayloadAreIndependentOfClientButtons() {
		long owner = create(false);
		assertThrows(BusinessException.class, () -> acceptance.accept(owner, step.id(), request(owner)));
		acceptance.accept(owner, "BQ001", request(owner));
		quests.interact(owner, "BQ001");
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
				new QuestAcceptCommandService.Request(UUID.randomUUID().toString(), original.expectedRevision())));
		long other = create(false);
		assertThrows(BusinessException.class, () -> acceptance.accept(other, step.id(),
				new QuestAcceptCommandService.Request(original.requestId(), charactersRead.find(other).revision())));
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
