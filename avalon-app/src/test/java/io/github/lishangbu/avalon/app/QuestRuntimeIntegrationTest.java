package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffect;
import io.github.lishangbu.avalon.content.quest.model.QuestContentEffectKind;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestRuntimeConfig;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestStatus;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.quest.QuestClock;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 正式Spring宿主和PostgreSQL验证新实例规则、奖励与状态共同回滚，以及实例命令的归属和防重。
 * 临时任务拥有完整目录和规则，通过生产接取入口创建实例，不直接伪造奖励或进度。
 */
class QuestRuntimeIntegrationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registration;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestRuntimeService runtime;

	@Autowired
	io.github.lishangbu.avalon.world.application.query.QuestQueryService questQueries;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	QuestCatalog catalog;

	@MockitoSpyBean
	QuestRulesCatalog rules;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	@MockitoSpyBean
	QuestClock clock;

	/** 每日任务跨周期开启新实例，旧READY资格保留；重复交付请求完整重放且不会再次发金币。 */
	@Test
	void dailyInstancesAndRequestReplaySurviveTransactionBoundaries() {
		Instant today = Instant.parse("2026-10-12T15:59:00Z");
		doReturn(today).when(clock).now();
		configure("TEST_DAILY", QuestRepeatPolicy.DAILY, 1, 2, null);
		long owner = create();
		var first = accept(owner, "TEST_DAILY");
		record(owner);
		record(owner);
		var ready = runtime.state(owner).getLast();
		assertEquals(QuestStatus.READY, ready.status());
		doReturn(today.plusSeconds(60)).when(clock).now();
		var request = request(ready, QuestOperation.DELIVER, "TEST_DAILY");
		long before = queries.find(owner).gold();
		var settled = runtime.command(owner, request);
		assertEquals(QuestStatus.COMPLETED, settled.instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
		assertEquals(settled, runtime.command(owner, request));
		assertEquals(before + 7, queries.find(owner).gold());
		var second = accept(owner, "TEST_DAILY");
		assertNotEquals(first.id(), second.id());
		assertTrue(second.progress().isEmpty());
		assertThrows(RuntimeException.class, () -> runtime.command(create(), request));
		assertThrows(RuntimeException.class, () -> runtime.command(owner, new QuestInstanceRequest(request.requestId(),
				second.id(), second.round(), request.operation(), request.step(), null, null)));
	}

	/** 真正的回执约束失败会回滚金币和实例完成状态，相同请求号修复后可以安全重试。 */
	@Test
	void receiptFailureRollsBackAssetsAndInstanceTogether() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_ATOMIC", QuestRepeatPolicy.ONCE, 1, 1, null);
		long owner = create();
		var state = accept(owner, "TEST_ATOMIC");
		record(owner);
		var request = request(state, QuestOperation.DELIVER, "TEST_ATOMIC");
		long before = queries.find(owner).gold();
		jdbc.execute("alter table quest_runtime_request add constraint test_runtime_failure check (false) not valid");
		try {
			assertThrows(RuntimeException.class, () -> runtime.command(owner, request));
		}
		finally {
			jdbc.execute("alter table quest_runtime_request drop constraint test_runtime_failure");
		}
		assertEquals(before, queries.find(owner).gold());
		assertEquals(QuestStatus.READY, runtime.state(owner).getLast().status());
		assertEquals(QuestStatus.COMPLETED, runtime.command(owner, request).instance().status());
		assertEquals(before + 7, queries.find(owner).gold());
	}

	/** 环内第二轮拒绝第一轮旧命令；放弃不返还开启次数，次日可创建从第一轮开始的新实例。 */
	@Test
	void ringRoundAndDailyOpeningQuotaAreEnforcedByHost() {
		var today = Instant.now().plusSeconds(60);
		doReturn(today).when(clock).now();
		configure("TEST_RING", QuestRepeatPolicy.RING, 1, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_RING");
		record(owner);
		var request = request(first, QuestOperation.DELIVER, "TEST_RING");
		var next = runtime.command(owner, request).instance();
		assertEquals(2, next.round());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), first.id(), 1,
						QuestOperation.DELIVER, "TEST_RING", null, null)));
		runtime.command(owner, request(next, QuestOperation.ABANDON, null));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_RING"));
		doReturn(today.plusSeconds(86400)).when(clock).now();
		var restarted = accept(owner, "TEST_RING");
		assertEquals(1, restarted.round());
		assertNotEquals(first.id(), restarted.id());
	}

	/** 任务超时后重试清空计数，并且使用新的实例与完整计时窗口。 */
	@Test
	void timeoutCreatesFreshAttempt() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		configure("TEST_TIMED", QuestRepeatPolicy.ONCE, 1, 2, new QuestTimeout(30L, null));
		long owner = create();
		var first = accept(owner, "TEST_TIMED");
		record(owner);
		doReturn(start.plusSeconds(30)).when(clock).now();
		var failed = runtime.command(owner, request(first, QuestOperation.REFRESH, null)).instance();
		assertEquals(QuestStatus.FAILED, failed.status());
		var retry = accept(owner, "TEST_TIMED");
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.progress().isEmpty());
	}

	/** 已交付的旧步骤保留历史奖励，但不能阻止限时新尝试从入口重新开始。 */
	@Test
	void timedRetryCanRestartPreviouslyDeliveredEntry() {
		var start = Instant.now().plusSeconds(60);
		doReturn(start).when(clock).now();
		String id = "TEST_TIMED_STAGES";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, new QuestTimeout(30L, null));
		var entry = catalog.step(id);
		String nextId = id + "_NEXT";
		var next = new QuestContentStep(nextId, id, entry.title(), entry.location(), entry.label(), entry.dialogue(),
				List.of(id), true, List.of(), null, List.of(), 0, false, null, true, List.of(), null, entry.runtime());
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(next);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(next).when(catalog).step(nextId);
		doReturn(dialogues.step("BQ001")).when(dialogues).step(nextId);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var advanced = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertTrue(advanced.completed().contains(id));
		doReturn(start.plusSeconds(30)).when(clock).now();
		var retry = accept(owner, id);
		assertNotEquals(first.id(), retry.id());
		assertTrue(retry.completed().isEmpty());
		record(owner);
		var secondAdvance = runtime.command(owner, request(retry, QuestOperation.DELIVER, id)).instance();
		assertTrue(secondAdvance.completed().contains(id));
		accept(owner, nextId);
	}

	/** 分支选择、公共汇合、日志完成数与旧剧情回执使用同一实例规则，未选路线不需要交付。 */
	@Test
	void branchSelectionCompletesOnlyChosenRouteAndCommonJoin() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_BRANCH";
		configure(id, QuestRepeatPolicy.ONCE, 1, 1, null);
		var base = catalog.step(id);
		var a = branchStep(base, id, List.of(), new QuestBranch("route", "A"), QuestDelivery.MANUAL);
		var b = branchStep(base, id + "_B", List.of(), new QuestBranch("route", "B"), QuestDelivery.MANUAL);
		var join = branchStep(base, id + "_JOIN", List.of(a.id(), b.id()), null, QuestDelivery.AUTO);
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(step -> step.quest().equals(id));
		all.addAll(List.of(a, b, join));
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		for (var step : List.of(a, b, join))
			doReturn(step).when(catalog).step(step.id());
		long owner = create();
		var state = accept(owner, id);
		assertEquals(2, runtime.views(owner).getLast().branches().size());
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, request(state, QuestOperation.DELIVER, a.id())));
		var chosen = runtime
			.command(owner,
					new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1, QuestOperation.CHOOSE, null,
							"route", "B"))
			.instance();
		assertThrows(RuntimeException.class,
				() -> runtime.command(owner, new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), 1,
						QuestOperation.CHOOSE, null, "route", "A")));
		var complete = runtime.command(owner, request(chosen, QuestOperation.DELIVER, b.id())).instance();
		assertEquals(QuestStatus.COMPLETED, complete.status());
		assertFalse(complete.completed().contains(a.id()));
		assertTrue(questQueries.done(owner, join.id()));
		var entry = questQueries.state(owner)
			.journal()
			.stream()
			.filter(value -> value.id().equals(id))
			.findFirst()
			.orElseThrow();
		assertTrue(entry.completed());
		assertEquals(2, entry.totalSteps());
		assertEquals(2, entry.completedSteps());
	}

	/** 同组两条互斥路线加公共汇合，所有显示文本仍来自游戏内容而非核心编号。 */
	private QuestContentStep branchStep(QuestContentStep base, String id, List<String> requires, QuestBranch branch,
			QuestDelivery delivery) {
		return new QuestContentStep(id, base.quest(), "路线" + id, base.location(), base.label(), base.dialogue(),
				requires, true, List.of(), null, List.of(), 0, false, null, true, List.of(), null,
				new QuestRuntimeConfig(List.of(), List.of(), List.of(new QuestEffect("GOLD", null, 7)), delivery,
						branch, null));
	}

	/** 同一微秒完成多次每日任务仍以尝试序号定位最新实例，并准确限制次数。 */
	@Test
	void sameTimestampAttemptsRemainOrderedAndRespectQuota() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		configure("TEST_MULTI_DAILY", QuestRepeatPolicy.DAILY, 2, 1, null);
		long owner = create();
		var first = accept(owner, "TEST_MULTI_DAILY");
		record(owner);
		runtime.command(owner, request(first, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		var second = accept(owner, "TEST_MULTI_DAILY");
		assertNotEquals(first.id(), second.id());
		assertEquals(second.id(), runtime.latest(queries.requireId(owner), "TEST_MULTI_DAILY").id());
		assertTrue(second.progress().isEmpty());
		record(owner);
		runtime.command(owner, request(second, QuestOperation.DELIVER, "TEST_MULTI_DAILY"));
		assertThrows(RuntimeException.class, () -> accept(owner, "TEST_MULTI_DAILY"));
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.noneMatch(value -> value.quest().equals("TEST_MULTI_DAILY")));
	}

	/** 同时刻新尝试不能复用旧补给资格，环内每轮补给独立且同轮重接保持幂等。 */
	@Test
	void suppliesAreScopedToAttemptAndRingRound() {
		doReturn(Instant.now().plusSeconds(60)).when(clock).now();
		String id = "TEST_RING_SUPPLIES";
		configure(id, QuestRepeatPolicy.RING, 1, 1, null);
		var base = catalog.step(id);
		var step = new QuestContentStep(id, id, base.title(), "VILLAGE_HOME", base.label(), base.dialogue(), List.of(),
				true, List.of(), null, List.of(), 0, false, null, true,
				List.of(new QuestContentEffect(QuestContentEffectKind.GIVE_ITEM, "HEALTH_POTION", 2)), null,
				base.runtime());
		var all = new ArrayList<>(catalog.definition().steps());
		all.removeIf(value -> value.id().equals(id));
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		long owner = create();
		var first = accept(owner, id);
		record(owner);
		var second = runtime.command(owner, request(first, QuestOperation.DELIVER, id)).instance();
		assertEquals(2, second.round());
		record(owner);
		// 下一轮即使已经完成击杀，也须接取本轮补给后才能交付。
		assertThrows(RuntimeException.class, () -> runtime.command(owner, request(second, QuestOperation.DELIVER, id)));
		var grant = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertEquals(1, grant.rewards().size());
		var repeated = acceptance.accept(owner, id,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertTrue(repeated.rewards().isEmpty());
		assertEquals(3, runtime.command(owner, request(second, QuestOperation.DELIVER, id)).instance().round());
	}

	/** 测试内容使用生产扩展字段；旧地点与NPC引用仍保留正式校验语义。 */
	private void configure(String id, QuestRepeatPolicy repeat, int limit, int count, QuestTimeout timeout) {
		var step = new QuestContentStep(id, id, "测试任务", "VILLAGE_HOME", "交付", "已完成", List.of(), true, List.of(), null,
				List.of(), 0, false, null, true, List.of(), null,
				new QuestRuntimeConfig(List.of(new QuestObjective("kills", QuestMeasure.EVENT, "KILL:TEST", count)),
						List.of(), List.of(new QuestEffect("GOLD", null, 7)), QuestDelivery.MANUAL, null, null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		doReturn(dialogues.step("BQ001")).when(dialogues).step(id);
		doReturn(new QuestRule(id, "测试任务", QuestType.SIDE, List.of(), repeat, true,
				new QuestPolicy("Asia/Hong_Kong", limit, 10, timeout, true,
						List.of(new QuestEffect("GOLD", null, 100))
							.stream()
							.filter(effect -> repeat == QuestRepeatPolicy.RING)
							.toList())))
			.when(rules)
			.rule(id); // 日志从整份规则目录聚合，临时任务也加入完整定义而非只替换单条查询。
		var allRules = new ArrayList<>(rules.definition().quests());
		allRules.add(rules.rule(id));
		doReturn(new QuestRulesDefinition(rules.definition().tags(), allRules)).when(rules).definition();

	}

	/** 固定任务时钟下发布带相同时刻的真实服务器事件，避免测试机器日期影响截止断言。 */
	private void record(long owner) {
		transactions.run(
				() -> runtime.record(owner, new QuestEvent(UUID.randomUUID().toString(), "KILL:TEST", 1, clock.now())));
	}

	private long create() {
		String name = "qr" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registration.register(name, "Quest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private QuestInstance accept(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		return runtime.state(owner).getLast();
	}

	private QuestInstanceRequest request(QuestInstance state, QuestOperation operation, String step) {
		return new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(), operation, step, null,
				null);
	}

}
