package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.persistence.repository.BattleRecordRepository;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/**
 * 真实 PostgreSQL 上验证正式捕捉的原子性、所有权、断线读取与回合去重，全部前置通过应用命令完成。
 */
class BattleFlowTest extends SpringIntegrationTest {

	@Autowired
	BattleRecordRepository battleRecordRepository;

	@Autowired
	ObjectMapper battleCodec;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService questAccepts;

	@Autowired
	SceneActionCommandService sceneActions;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService inventoryQueries;

	@Autowired
	BattleCommandService battles;

	@Autowired
	BattleQueryService battleQueries;

	@Autowired
	EntityManager em;

	@Autowired
	BattleCatalog battleCatalog;

	/** 固定合法掉落种子及濒死目标，只验证结算事务，不作为自然战斗难度证据。 */
	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void wildEvolutionMaterialsSurviveOverflowAndRollbackWithoutDuplicateRewards(boolean fullBag) {
		var player = ready("WARRIOR");
		if (fullBag)
			transactions.run(() -> {
				for (int i = 0; i < 60; i++)
					inventory.grant(player.owner(), "MATERIAL_FULL:" + i, "STARTER_BODY", 1, true);
			});
		var initial = battles.start(player.owner(), "VILLAGE", UUID.randomUUID().toString());
		long seed = 0;
		while (true) {
			assertTrue(seed < 100000, "应能找到两种材料同时掉落的合法种子");
			var random = new Random(seed ^ 0x6A09E667F3BCC909L);
			random
				.nextInt(battleCatalog.definition().supplyDrops().values().stream().mapToInt(Integer::intValue).sum());
			random.nextInt(battleCatalog.definition().bookDrops().values().stream().mapToInt(Integer::intValue).sum());
			if (random.nextInt(100) < 20 && random.nextInt(100) < 10)
				break;
			seed++;
		}
		long chosenSeed = seed;
		transactions.run(() -> {
			var record = battleRecordRepository.get(Long.parseLong(initial.id()));
			var state = battleCodec.readValue(record.getState(), BattleState.class);
			state.setSeed(chosenSeed);
			var enemy = state.getUnits().stream().filter(BattleState.Unit::isEnemy).findFirst().orElseThrow();
			enemy.setSpecies("CLAW_CAT");
			enemy.setHp(1);
			record.setState(battleCodec.writeValueAsString(state));
		});
		var prepared = battleQueries.current(player.owner());
		var beforeItems = inventoryQueries.list(player.owner());
		var beforePending = inventoryQueries.pending(player.owner());
		var beforeCharacter = characterQueries.find(player.owner());
		var commands = orders(prepared, "ATTACK", "ATTACK");
		// 在完整结算已执行后抛错，经验、金币、背包、待领取和回合必须一起回滚。
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			assertEquals("WON",
					battles.turn(player.owner(), Long.parseLong(initial.id()), 1, commands, false).status());
			throw new IllegalStateException("模拟结算提交前失败");
		}));
		assertEquals(prepared, battleQueries.current(player.owner()));
		assertEquals(beforeItems, inventoryQueries.list(player.owner()));
		assertEquals(beforePending, inventoryQueries.pending(player.owner()));
		assertEquals(beforeCharacter, characterQueries.find(player.owner()));
		var won = battles.turn(player.owner(), Long.parseLong(initial.id()), 1, commands, false);
		assertEquals("WON", won.status());
		for (String code : List.of("WATER_PEARL", "TIDAL_CLAW_CAT_ESSENCE")) {
			transactions.run(() -> {
				var row = (Object[]) em
					.createNativeQuery("SELECT quantity, pending_quantity, bound FROM owned_item"
							+ " WHERE character_id=:character AND code=:code")
					.setParameter("character", characters.lock(player.owner()).id())
					.setParameter("code", code)
					.getSingleResult();
				assertEquals(fullBag ? 0 : 1, ((Number) row[0]).intValue());
				assertEquals(fullBag ? 1 : 0, ((Number) row[1]).intValue());
				assertEquals(false, row[2], "野外进化材料不绑定");
			});
		}
		var items = inventoryQueries.list(player.owner());
		var pending = inventoryQueries.pending(player.owner());
		assertEquals(won, battles.turn(player.owner(), Long.parseLong(initial.id()), 1, commands, false));
		assertEquals(items, inventoryQueries.list(player.owner()));
		assertEquals(pending, inventoryQueries.pending(player.owner()));
	}

	Player ready(String profession) {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		var tokens = registrations.register("battle_" + suffix, "Battle-test-1234", "测试");
		long owner = accounts.accountId("battle_" + suffix);
		characters.create(owner, "捕捉" + suffix, profession);
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		accept(owner, "BQ001");
		quests.interact(owner, "BQ001");
		move(owner, "ELDER_HOUSE");
		accept(owner, "BQ002");
		quests.interact(owner, "BQ002");
		move(owner, "SQUARE");
		move(owner, "WEAPON_SHOP");
		accept(owner, "BQ003");
		quests.interact(owner, "BQ003");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		accept(owner, "BQ004");
		quests.interact(owner, "BQ004");
		accept(owner, "BQ005");
		var pet = creatureQueries.list(owner).getFirst();
		creatures.position(owner, Long.parseLong(pet.id()), pet.partyPosition());
		creatures.viewedSkills(owner, Long.parseLong(pet.id()));
		for (var item : inventoryQueries.list(owner))
			if (item.slot() != null)
				inventory.equip(owner, Long.parseLong(item.id()));
		quests.interact(owner, "BQ005");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		accept(owner, "BQ006");
		quests.interact(owner, "BQ006");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		questAccepts.accept(owner, "BQ007",
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
		// 升级和穿装只提高上限，捕捉前在真实中心恢复，不能把残血开局当成标准准备。
		battles.recover(owner);
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		return new Player(owner, tokens.accessToken());
	}

	private void accept(long owner, String step) {
		questAccepts.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
	}

	private void scene(long owner, String action) {
		sceneActions.execute(owner, action, new SceneActionCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision(), null));
	}

	void move(long owner, String destination) {
		characters.move(owner, destination, characterQueries.find(owner).revision());
	}

	BattleView start(Player p) {
		return battles.start(p.owner(), "TUTORIAL", UUID.randomUUID().toString());
	}

	@Test
	void ordinaryRiversideEncounterAllowsCaptureBeforeRegistrationQuestIsCompleted() {
		var player = ready("WARRIOR");
		var battle = battles.start(player.owner(), "VILLAGE", UUID.randomUUID().toString());
		var hero = battle.units().stream().filter(BattleView.UnitView::hero).findFirst().orElseThrow();
		assertTrue(hero.actions().stream().anyMatch(action -> action.id().equals("SEAL_I") && action.available()));
		assertTrue(battle.units()
			.stream()
			.filter(BattleView.UnitView::enemy)
			.allMatch(unit -> !unit.captures().isEmpty()));
	}

	List<Order> orders(BattleView battle, String heroAction, String petAction) {
		String enemy = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
		return battle.units()
			.stream()
			.filter(u -> !u.enemy() && u.hp() > 0 && (u.hero() || u.position() < 2))
			.map(u -> new Order(u.id(), u.hero() ? heroAction : petAction,
					(u.hero() ? heroAction : petAction).equals("DEFEND") ? u.id() : enemy))
			.toList();
	}

	@Test
	void fourProfessionsRetryFailedCaptureInSameBattleWithoutRerollingTarget() {
		for (var profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			var player = ready(profession);
			var initial = start(player);
			var target = initial.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow();
			double chance = target.captures()
				.stream()
				.filter(ball -> ball.code().equals("SEAL_I"))
				.findFirst()
				.orElseThrow()
				.chance();
			assertTrue(chance > 0 && chance < 1);
			// 固定一个会失败的合法随机种子，不降低捕捉率、不改目标个体或直接写入失败结果。
			// 首轮三名行动者洗牌后最多先消耗敌人一次伤害随机值，检查三个候选抽值覆盖行动先后。
			long seed = 0;
			while (true) {
				assertTrue(seed < 10000, "应能在有限种子范围内构造捕捉失败样本");
				var random = new Random(seed + initial.round());
				Collections.shuffle(new ArrayList<>(List.of(1, 2, 3)), random);
				if (random.nextDouble() >= chance && random.nextDouble() >= chance && random.nextDouble() >= chance)
					break;
				seed++;
			}
			long chosenSeed = seed;
			transactions.run(() -> {
				var stored = battleRecordRepository.get(Long.parseLong(initial.id()));
				var state = battleCodec.readValue(stored.getState(), BattleState.class);
				state.setSeed(chosenSeed);
				stored.setState(battleCodec.writeValueAsString(state));
			});
			var beforePets = creatureQueries.list(player.owner());
			var identity = targetIdentity(initial.id());
			int beforeBalls = initial.supplies().get("SEAL_I");
			var failedOrders = orders(initial, "SEAL_I", "DEFEND");
			var failed = battles.turn(player.owner(), Long.parseLong(initial.id()), 1, failedOrders, false);
			assertEquals("ACTIVE", failed.status());
			assertEquals(2, failed.round());
			assertEquals(beforeBalls - 1, failed.supplies().get("SEAL_I"));
			assertTrue(failed.log().stream().anyMatch(line -> line.contains("捕捉失败")));
			assertEquals(beforePets, creatureQueries.list(player.owner()));
			var sameTarget = failed.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow();
			assertEquals(target.id(), sameTarget.id());
			assertEquals(target.name(), sameTarget.name());
			assertEquals(target.level(), sameTarget.level());
			assertEquals(target.stats(), sameTarget.stats());
			assertEquals(target.hp(), sameTarget.hp());
			assertFalse(sameTarget.captured());
			assertEquals(identity, targetIdentity(initial.id()), "失败后不能重抽物种或特性槽位");
			// 幻兽本轮仍能正常出招并耗魔，不能把它的整个战斗状态错误地要求为静止快照。
			assertEquals(failed, battleQueries.current(player.owner()));
			assertEquals(failed, battles.turn(player.owner(), Long.parseLong(initial.id()), 1, failedOrders, false));
			// 同场重试本身不要求继续削血，避免额外战斗掩盖本例的失败重放边界。
			var weakened = failed;
			var won = battles.turn(player.owner(), Long.parseLong(initial.id()), weakened.round(),
					orders(weakened, "SEAL_I", "DEFEND"), false);
			assertEquals(initial.id(), won.id());
			// 首次收录小岩怪奖励一球，与本次消耗分别核算；第二次投球并非必定成功。
			int collectionBalls = won.status().equals("WON") ? 1 : 0;
			assertEquals(weakened.supplies().get("SEAL_I") - 1 + collectionBalls, won.supplies().get("SEAL_I"));
			assertTrue(won.status().equals("WON") || won.status().equals("ACTIVE"));
			if (won.status().equals("WON"))
				assertEquals(beforePets.size() + 1, creatureQueries.list(player.owner()).size());
			else {
				assertEquals(beforePets, creatureQueries.list(player.owner()));
				assertEquals(identity, targetIdentity(initial.id()));
				assertTrue(won.log().stream().anyMatch(line -> line.contains("捕捉失败")));
			}
			var settledItems = inventoryQueries.list(player.owner());
			var settledPets = creatureQueries.list(player.owner());
			assertEquals(won, battles.turn(player.owner(), Long.parseLong(initial.id()), weakened.round(),
					orders(weakened, "SEAL_I", "DEFEND"), false));
			assertEquals(settledItems, inventoryQueries.list(player.owner()));
			assertEquals(settledPets, creatureQueries.list(player.owner()));
		}
	}

	/** 隐藏特性不能从公开投影读取，直接核对持久化个体字段，不向客户端增加泄露字段。 */
	private List<String> targetIdentity(String battleId) {
		return transactions.call(() -> {
			var stored = battleRecordRepository.get(Long.parseLong(battleId));
			var state = battleCodec.readValue(stored.getState(), BattleState.class);
			var enemy = state.getUnits().stream().filter(BattleState.Unit::isEnemy).findFirst().orElseThrow();
			return List.of(enemy.getId(), enemy.getSpecies(), enemy.getAbilitySlot(), enemy.getAbility());
		});
	}

	/** 四职业通过真实概率捕获、编队并回中心登记，任务重放不再次发奖。洞穴流程由开篇连续测试覆盖。 */
	@Test
	void allProfessionsCapturePlaceAndReturnWithoutDuplicateRewards() {
		for (String profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			var p = ready(profession);
			long before = characterQueries.find(p.owner()).experience();
			for (String location : List.of("STONE_PATH", "SQUARE", "PET_CENTER"))
				move(p.owner(), location);
			long captured = OpeningWildTraining.capture(p.owner(), characters, characterQueries, battles,
					creatureQueries);
			assertEquals(before, characterQueries.find(p.owner()).experience(), "捕获不提供击杀经验");
			assertFalse(inventoryQueries.list(p.owner())
				.stream()
				.anyMatch(item -> List
					.of("WATER_PEARL", "TIDAL_CLAW_CAT_ESSENCE", "WOOD_SAP", "ANCIENT_WOOD_GUARD_ESSENCE", "EARTH_CORE",
							"ROCK_GIANT_ESSENCE", "THUNDER_CRYSTAL", "THUNDERHORN_LIZARD_ESSENCE")
					.contains(item.code())), "捕获不能同时获得击败材料");
			assertEquals(2, creatureQueries.list(p.owner()).size());
			creatures.position(p.owner(), captured, 1);
			creatures.registerCapture(p.owner());
			var receipt = quests.interact(p.owner(), "BQ007");
			var after = characterQueries.find(p.owner());
			var pets = creatureQueries.list(p.owner());
			var items = inventoryQueries.list(p.owner());
			creatures.registerCapture(p.owner());
			assertEquals(receipt, quests.interact(p.owner(), "BQ007"));
			assertEquals(after, characterQueries.find(p.owner()));
			assertEquals(pets, creatureQueries.list(p.owner()));
			assertEquals(items, inventoryQueries.list(p.owner()));
			move(p.owner(), "SQUARE");
			scene(p.owner(), "BQ008_COLLECT_STONES");
			quests.interact(p.owner(), "BQ008");
			move(p.owner(), "PET_CENTER");
			accept(p.owner(), "BQ009");
			battles.recover(p.owner());
			var preparation = quests.interact(p.owner(), "BQ009");
			assertEquals(preparation, quests.interact(p.owner(), "BQ009"));
		}
	}

	@Test
	void concurrentTurnAndBattleGateProtectResources() throws Exception {
		var p = ready("WARRIOR");
		assertThrows(BusinessException.class, () -> move(p.owner(), "FOREST_EDGE"));
		client.get()
			.uri("/api/battle")
			.header("Authorization", "Bearer " + p.token())
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.battle")
			.isEqualTo(null);
		var battle = start(p);
		long id = Long.parseLong(battle.id());
		assertThrows(BusinessException.class, () -> move(p.owner(), "STONE_PATH"));
		assertThrows(BusinessException.class, () -> quests.interact(p.owner(), "BQ007"));
		var gear = inventoryQueries.list(p.owner()).stream().filter(i -> i.slot() != null).findFirst().orElseThrow();
		assertThrows(BusinessException.class, () -> inventory.equip(p.owner(), Long.parseLong(gear.id())));
		var cat = creatureQueries.list(p.owner()).getFirst();
		assertThrows(BusinessException.class, () -> creatures.position(p.owner(), Long.parseLong(cat.id()), 1));
		assertThrows(BusinessException.class, () -> battles.recover(p.owner()));
		assertThrows(BusinessException.class, () -> battles.turn(p.owner(), id, 1, List.of(), false));
		assertEquals(battle, battleQueries.current(p.owner()));
		CountDownLatch gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Callable<BattleView> action = () -> {
				gate.await();
				return battles.turn(p.owner(), id, 1, orders(battle, "ATTACK", "水弹"), false);
			};
			var first = executor.submit(action);
			var second = executor.submit(action);
			gate.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(2, battleQueries.current(p.owner()).round());
		var other = ready("MAGE");
		client.post()
			.uri("/api/battle/" + id + "/turn")
			.header("Authorization", "Bearer " + other.token())
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("round", 2, "orders", List.of(), "surrender", true))
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		battles.turn(p.owner(), id, 2, List.of(), true);
		assertEquals("SQUARE", characterQueries.find(p.owner()).location());
		move(p.owner(), "PET_CENTER");
		battles.recover(p.owner());
		assertTrue(battleQueries.lobby(p.owner())
			.vitals()
			.stream()
			.allMatch(v -> v.hp() == v.maxHp() && v.mp() == v.maxMp()));
		move(p.owner(), "SQUARE");
		move(p.owner(), "SUPPLY_SHOP");
		String key = UUID.randomUUID().toString();
		battles.purchase(p.owner(), "SEAL_I", key);
		battles.purchase(p.owner(), "SEAL_I", key);
		assertEquals(32, characterQueries.find(p.owner()).gold());
		assertEquals(7,
				inventoryQueries.list(p.owner())
					.stream()
					.filter(i -> i.code().equals("SEAL_I"))
					.mapToInt(i -> i.quantity())
					.sum());
	}

	@Test
	void capturePersistenceFailureRollsBackBallAndRoundThenRetryWorks() {
		var p = ready("WARRIOR");
		var battle = start(p);
		// 本例必须实际走入捕获写库才能触发故障。只选择成功抽样，不修改捕捉公式或目标。
		double chance = battle.units()
			.stream()
			.filter(BattleView.UnitView::enemy)
			.findFirst()
			.orElseThrow()
			.captures()
			.stream()
			.filter(ball -> ball.code().equals("SEAL_I"))
			.findFirst()
			.orElseThrow()
			.chance();
		long seed = 0;
		while (true) {
			assertTrue(seed < 100000, "应能找到成功抽样");
			var random = new Random(seed + battle.round());
			Collections.shuffle(new ArrayList<>(List.of(1, 2, 3)), random);
			if (random.nextDouble() < chance && random.nextDouble() < chance && random.nextDouble() < chance)
				break;
			seed++;
		}
		long captureSeed = seed;
		transactions.run(() -> {
			var stored = battleRecordRepository.get(Long.parseLong(battle.id()));
			var state = battleCodec.readValue(stored.getState(), BattleState.class);
			state.setSeed(captureSeed);
			stored.setState(battleCodec.writeValueAsString(state));
		});
		long character = Long.parseLong(characterQueries.find(p.owner()).id());
		transactions.run(() -> {
			em.createNativeQuery(
					"CREATE FUNCTION reject_test_capture() RETURNS trigger" + " LANGUAGE plpgsql AS $$ BEGIN IF"
							+ " NEW.character_id=" + character + " AND NEW.species='STONE_GOLEM' THEN RAISE"
							+ " EXCEPTION 'test capture failure'; END IF;" + " RETURN NEW; END $$")
				.executeUpdate();
			em.createNativeQuery("CREATE TRIGGER reject_test_capture BEFORE INSERT ON"
					+ " owned_creature FOR EACH ROW EXECUTE FUNCTION" + " reject_test_capture()")
				.executeUpdate();
		});
		try {
			assertThrows(RuntimeException.class, () -> battles.turn(p.owner(), Long.parseLong(battle.id()),
					battle.round(), orders(battle, "SEAL_I", "DEFEND"), false));
			assertEquals(battle, battleQueries.current(p.owner()));
			assertEquals(1, creatureQueries.list(p.owner()).size());
		}
		finally {
			transactions.run(() -> {
				em.createNativeQuery("DROP TRIGGER reject_test_capture ON" + " owned_creature").executeUpdate();
				em.createNativeQuery("DROP FUNCTION reject_test_capture()").executeUpdate();
			});
		}
		assertEquals("WON",
				battles
					.turn(p.owner(), Long.parseLong(battle.id()), battle.round(), orders(battle, "SEAL_I", "DEFEND"),
							false)
					.status());
	}

	/**
	 * 误杀教学目标能重新遭遇，但不能刷教学奖励；普通遭遇按真实击败结果发放一次收益。
	 */
	@Test
	void tutorialKillCanRetryAndOrdinaryVictoryAwardsExactlyOnce() {
		var p = ready("WARRIOR");
		var before = characterQueries.find(p.owner());
		String key = UUID.randomUUID().toString();
		var tutorial = battles.start(p.owner(), "TUTORIAL", key);
		assertEquals(tutorial, battles.start(p.owner(), "TUTORIAL", key));
		for (int i = 0; i < 12 && tutorial.status().equals("ACTIVE"); i++)
			tutorial = battles.turn(p.owner(), Long.parseLong(tutorial.id()), tutorial.round(),
					orders(tutorial, "ATTACK", "ATTACK"), false);
		assertEquals("WON", tutorial.status());
		assertEquals(before.experience(), characterQueries.find(p.owner()).experience());
		assertEquals(before.gold(), characterQueries.find(p.owner()).gold());
		var retry = start(p);
		assertNotEquals(tutorial.id(), retry.id());
		battles.turn(p.owner(), Long.parseLong(retry.id()), retry.round(), List.of(), true);
		move(p.owner(), "PET_CENTER");
		battles.recover(p.owner());
		move(p.owner(), "SQUARE");
		move(p.owner(), "STONE_PATH");
		move(p.owner(), "RIVERSIDE");
		var wild = battles.start(p.owner(), "VILLAGE", UUID.randomUUID().toString());
		int lastRound = wild.round();
		for (int i = 0; i < 15 && wild.status().equals("ACTIVE"); i++) {
			lastRound = wild.round();
			wild = battles.turn(p.owner(), Long.parseLong(wild.id()), lastRound, orders(wild, "ATTACK", "ATTACK"),
					false);
		}
		assertEquals("WON", wild.status());
		var rewarded = characterQueries.find(p.owner());
		assertTrue(rewarded.experience() > before.experience());
		assertEquals(before.gold() + 1, rewarded.gold());
		assertEquals(wild, battles.turn(p.owner(), Long.parseLong(wild.id()), lastRound, List.of(), true));
		assertEquals(rewarded, characterQueries.find(p.owner()));
		assertEquals(1, creatureQueries.list(p.owner()).size());
	}

	/** 捕捉补给耗尽时，可用免费恢复和普通野战重新购买封印球，不依赖重复领取任务补给。 */
	@Test
	void exhaustedCaptureSuppliesCanBeEarnedAgainBeforeCaptureTaskCompletion() {
		var player = ready("WARRIOR");
		long owner = player.owner();
		transactions.run(() -> {
			characters.lock(owner);
			characters.changeGold(owner, -characterQueries.find(owner).gold());
			for (var item : inventoryQueries.list(owner))
				if (List.of("SEAL_I", "HEALTH_POTION", "MANA_POTION").contains(item.code()))
					inventory.consume(owner, Long.parseLong(item.id()), item.code(), item.quantity());
		});
		for (int fight = 0; fight < 8; fight++) {
			var battle = battles.start(owner, "VILLAGE", UUID.randomUUID().toString());
			for (int round = 0; round < 20 && battle.status().equals("ACTIVE"); round++)
				battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(),
						orders(battle, "ATTACK", "ATTACK"), false);
			assertEquals("WON", battle.status());
			move(owner, "STONE_PATH");
			move(owner, "SQUARE");
			move(owner, "PET_CENTER");
			battles.recover(owner);
			move(owner, "SQUARE");
			move(owner, "STONE_PATH");
			move(owner, "RIVERSIDE");
		}
		assertEquals(8, characterQueries.find(owner).gold());
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		move(owner, "SUPPLY_SHOP");
		int before = inventoryQueries.list(owner)
			.stream()
			.filter(item -> item.code().equals("SEAL_I"))
			.mapToInt(item -> item.quantity())
			.sum();
		String request = UUID.randomUUID().toString();
		battles.purchase(owner, "SEAL_I", request);
		battles.purchase(owner, "SEAL_I", request);
		assertEquals(0, characterQueries.find(owner).gold());
		assertEquals(before + 1,
				inventoryQueries.list(owner)
					.stream()
					.filter(item -> item.code().equals("SEAL_I"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(1, creatureQueries.list(owner).size(), "恢复补给不要求先完成捕捉");
	}

	record Player(long owner, String token) {
	}

}
