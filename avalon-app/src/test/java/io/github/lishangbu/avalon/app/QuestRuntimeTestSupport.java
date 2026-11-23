package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.quest.QuestDialogueCatalog;
import io.github.lishangbu.avalon.content.quest.QuestRulesCatalog;
import io.github.lishangbu.avalon.content.quest.model.QuestContentDefinition;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.content.quest.model.QuestContentStep;
import io.github.lishangbu.avalon.content.quest.model.QuestPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRepeatPolicy;
import io.github.lishangbu.avalon.content.quest.model.QuestRule;
import io.github.lishangbu.avalon.content.quest.model.QuestRulesDefinition;
import io.github.lishangbu.avalon.content.quest.model.QuestType;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.quest.QuestInstance;
import io.github.lishangbu.avalon.quest.model.QuestBranch;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEvent;
import io.github.lishangbu.avalon.quest.model.QuestMeasure;
import io.github.lishangbu.avalon.quest.model.QuestObjective;
import io.github.lishangbu.avalon.quest.model.QuestTimeout;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.quest.QuestClock;
import io.github.lishangbu.avalon.world.quest.QuestRuntimeService;
import io.github.lishangbu.avalon.world.quest.model.QuestInstanceRequest;
import io.github.lishangbu.avalon.world.quest.model.QuestOperation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import io.github.lishangbu.avalon.quest.model.*;
import io.github.lishangbu.avalon.content.quest.model.QuestReception;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 正式Spring宿主和PostgreSQL验证新实例规则、奖励与状态共同回滚，以及实例命令的归属和防重。
 * 临时任务拥有完整目录和规则，通过生产接取入口创建实例，不直接伪造奖励或进度。
 */
abstract class QuestRuntimeTestSupport extends SpringIntegrationTest {

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
	QuestQueryService questQueries;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService inventoryCommands;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService inventoryQueries;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.SceneActionCommandService sceneCommands;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.BattleCommandService openingBattles;

	@Autowired
	io.github.lishangbu.avalon.battle.application.query.BattleQueryService openingBattleQueries;

	@Autowired
	io.github.lishangbu.avalon.battle.application.command.TrainingCommandService openingTraining;

	@Autowired
	io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository openingBattleRecords;

	@Autowired
	io.github.lishangbu.avalon.battle.domain.BattleEngine openingEngine;

	@Autowired
	tools.jackson.databind.ObjectMapper openingCodec;

	@Autowired
	io.github.lishangbu.avalon.battle.application.query.EncounterAccess encounterAccess;

	@Autowired
	io.github.lishangbu.avalon.content.battle.BattleCatalog battleCatalog;

	@Autowired
	io.github.lishangbu.avalon.world.persistence.repository.QuestRepository completionReceipts;

	/** 为独立任务测试建立正式前置完成回执，不把普通剧情事实冒充任务完成。 */
	void completeGate(long owner, String step) {
		if (questQueries.done(owner, step))
			return;
		transactions.run(() -> {
			var receipt = new io.github.lishangbu.avalon.world.persistence.entity.QuestReceipt();
			receipt.setCharacterId(queries.requireId(owner));
			receipt.setStep(step);
			receipt.setQuestCode(catalog.step(step).quest());
			receipt.setCompleted(true);
			receipt.setResponse("{}");
			receipt.setCreatedAt(Instant.now());
			completionReceipts.save(receipt);
			facts.record(owner, "QUEST:" + step);
		});
	}

	/** 章节隔离用例的角色已经完成新开篇；自然开篇用例仍从注册开始。 */
	void completeOpening(long owner) {
		for (String gate : List.of("O013", "O014", "O015"))
			completeGate(owner, gate);
	}

	/** 纯引擎筛选成功样本后只固定该回合种子，实际捕获、扣球、事件与回执仍走正式事务。 */
	void openingCapture(long owner) {
		var battle = openingBattles.start(owner, "OPENING_RIVERSIDE_PATROL", UUID.randomUUID().toString());
		String hero = battle.units().stream().filter(unit -> unit.hero()).findFirst().orElseThrow().id();
		String target = battle.units().stream().filter(unit -> unit.enemy()).findFirst().orElseThrow().id();
		var orders = battle.units()
			.stream()
			.filter(unit -> !unit.enemy() && (unit.hero() || unit.position() < 2))
			.map(unit -> new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(),
					unit.hero() ? "SEAL_I" : "DEFEND", unit.hero() ? target : unit.id()))
			.toList();
		var snapshot = new io.github.lishangbu.avalon.battle.domain.BattleResourceSnapshot(
				java.util.Map.of(hero, java.util.Map.of("SEAL_I", (long) battle.supplies().get("SEAL_I"))),
				java.util.Set.of(target));
		var state = transactions
			.call(() -> openingCodec.readValue(openingBattleRecords.get(Long.parseLong(battle.id())).getState(),
					io.github.lishangbu.avalon.battle.domain.BattleState.class));
		long seed = 0;
		while (true) {
			assertTrue(seed < 10000, "普通捕捉应存在可复现的成功样本");
			state.setSeed(seed);
			var result = openingEngine.advance(state, orders, false, snapshot, java.util.Map.of());
			if (result.state().getUnits().stream().anyMatch(unit -> unit.isEnemy() && unit.isCaptured()))
				break;
			seed++;
		}
		long selected = seed;
		transactions.run(() -> {
			var stored = openingBattleRecords.get(Long.parseLong(battle.id()));
			var current = openingCodec.readValue(stored.getState(),
					io.github.lishangbu.avalon.battle.domain.BattleState.class);
			current.setSeed(selected);
			stored.setState(openingCodec.writeValueAsString(current));
		});
		int before = creatureQueries.list(owner).size();
		var result = BattleTestCommands.turn(openingBattles, owner, Long.parseLong(battle.id()), battle.round(), orders,
				false);
		assertEquals("WON", result.status());
		assertEquals(before + 1, creatureQueries.list(owner).size());
	}

	void openingMove(long owner, String... locations) {
		for (String location : locations)
			characters.move(owner, location, queries.find(owner).revision());
	}

	void openingRiversideTrip(long owner) {
		openingMove(owner, "SQUARE", "STONE_PATH", "RIVERSIDE");
		openingFight(owner, "OPENING_RIVERSIDE_PATROL");
		openingMove(owner, "STONE_PATH", "SQUARE", "PET_CENTER");
		openingBattles.recover(owner);
	}

	/** 使用公开战局选招、药剂与替补，不修改种子、敌人血量、人物等级或资源。 */
	void openingFight(long owner, String encounter) {
		var battle = openingBattles.start(owner, encounter, UUID.randomUUID().toString());
		int rounds = 0;
		while (battle.status().equals("ACTIVE")) {
			assertTrue(rounds++ < 30, "初级遭遇应在30回合内完成");
			var replacements = new java.util.LinkedHashMap<String, Integer>();
			var reserves = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && !unit.hero() && unit.hp() > 0 && unit.position() >= 2
						&& unit.position() <= 3)
				.iterator();
			for (int position = 0; position < 2 && reserves.hasNext(); position++) {
				int slot = position;
				if (battle.units()
					.stream()
					.noneMatch(unit -> !unit.enemy() && !unit.hero() && unit.position() == slot && unit.hp() > 0))
					replacements.put(reserves.next().id(), position);
			}
			var party = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0
						&& (unit.hero() || unit.position() < 2 || replacements.containsKey(unit.id())))
				.toList();
			var injured = party.stream()
				.min(java.util.Comparator.comparingDouble(unit -> (double) unit.hp() / unit.stats().health()))
				.orElseThrow();
			boolean warning = battle.telegraph() != null && battle.telegraph().contains("即将");
			String target = battle.units()
				.stream()
				.filter(unit -> unit.enemy() && unit.hp() > 0)
				.findFirst()
				.orElseThrow()
				.id();
			var orders = party.stream().map(unit -> {
				String action = warning ? "DEFEND" : "ATTACK";
				String aim = warning ? unit.id() : target;
				if (unit.hero() && injured.hp() < injured.stats().health() * 0.45
						&& unit.actions()
							.stream()
							.anyMatch(value -> value.id().equals("HEALTH_POTION") && value.available())) {
					action = "HEALTH_POTION";
					aim = injured.id();
				}
				else if (!warning && !unit.hero()
						&& unit.actions().stream().anyMatch(value -> value.id().equals("水弹") && value.available()))
					action = "水弹";
				return new io.github.lishangbu.avalon.battle.domain.BattleStateOrder(unit.id(), action, aim);
			}).toList();
			battle = BattleTestCommands.turn(openingBattles, owner, Long.parseLong(battle.id()), battle.round(), orders,
					false, replacements);
		}
		assertEquals("WON", battle.status());
	}

	/** 使用正式赠予和编队入口验证展示，不把捕捉事件或前端状态伪造成当前所有权。 */
	@Autowired
	io.github.lishangbu.avalon.creature.application.command.CreatureCommandService creatures;

	@Autowired
	io.github.lishangbu.avalon.creature.application.query.CreatureQueryService creatureQueries;

	@Autowired
	io.github.lishangbu.avalon.creature.application.command.CreatureManagementService creatureManagement;

	@MockitoSpyBean
	QuestCatalog catalog;

	@MockitoSpyBean
	QuestRulesCatalog rules;

	@MockitoSpyBean
	QuestDialogueCatalog dialogues;

	@MockitoSpyBean
	QuestClock clock;

	QuestContentStep switchableStep(QuestContentStep base, String id, String option) {
		var source = catalog.step(option.equals("A") ? "N026_POTIONS" : "N026_HERBS");
		return new QuestContentStep(id, base.quest(), "可切换方法" + option, base.location(), base.label(), base.dialogue(),
				List.of(), false, source.effects(), source.objectives(), List.of(), QuestDelivery.MANUAL,
				new QuestBranch("method", option, io.github.lishangbu.avalon.quest.model.QuestBranchMode.SWITCHABLE),
				null, null, true, List.of(), new QuestReception(base.location(), null));
	}

	/** 同组两条互斥路线加公共汇合，所有显示文本仍来自游戏内容而非核心编号。 */
	QuestContentStep branchStep(QuestContentStep base, String id, List<String> requires, QuestBranch branch,
			QuestDelivery delivery) {
		return new QuestContentStep(id, base.quest(), "路线" + id, base.location(), base.label(), base.dialogue(),
				requires, true, List.of(new QuestEffect("GOLD", null, 7)), List.of(), List.of(), delivery, branch, null,
				null, true, List.of(), new QuestReception(base.location(), null));
	}

	/** 测试内容使用生产扩展字段；旧地点与NPC引用仍保留正式校验语义。 */
	void configure(String id, QuestRepeatPolicy repeat, int limit, int count, QuestTimeout timeout) {
		var step = new QuestContentStep(id, id, "测试任务", "VILLAGE_HOME", "交付", "已完成", List.of(), true,
				List.of(new QuestEffect("GOLD", null, 7)),
				List.of(new QuestObjective("kills", QuestMeasure.EVENT, "KILL:TEST", count)), List.of(),
				QuestDelivery.MANUAL, null, null, null, true, List.of(), new QuestReception("VILLAGE_HOME", null));
		var all = new ArrayList<>(catalog.definition().steps());
		all.add(step);
		doReturn(new QuestContentDefinition(all)).when(catalog).definition();
		doReturn(step).when(catalog).step(id);
		doReturn(dialogues.step("O001")).when(dialogues).step(id);
		doReturn(new QuestRule(id, "测试任务", QuestType.SIDE, List.of(), true,
				new QuestPolicy(repeat, "Asia/Hong_Kong", limit, 10, timeout, true,
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
	void record(long owner) {
		transactions.run(
				() -> runtime.record(owner, new QuestEvent(UUID.randomUUID().toString(), "KILL:TEST", 1, clock.now())));
	}

	long create() {
		String name = "qr" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		registration.register(name, "Quest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	QuestInstance acceptOpening(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		return runtime.state(owner)
			.stream()
			.filter(state -> state.quest().equals(catalog.step(step).quest()))
			.findFirst()
			.orElseThrow();
	}

	QuestInstance accept(long owner, String step) {
		acceptance.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		return runtime.state(owner).getLast();
	}

	QuestInstanceRequest request(QuestInstance state, QuestOperation operation, String step) {
		return new QuestInstanceRequest(UUID.randomUUID().toString(), state.id(), state.round(), operation, step, null,
				null);
	}

}
