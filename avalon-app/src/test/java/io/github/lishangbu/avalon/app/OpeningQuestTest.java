package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.QuestState;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import tools.jackson.databind.JsonNode;

/**
 * 开篇真实事务验证，四职业走相同 JSON 流程；后置故障通过数据库触发器注入。
 */
class OpeningQuestTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accountQueries;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestQueryService questQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService inventoryQueries;

	@Autowired
	CreatureQueryService creatures;

	@Autowired
	EntityManager em;

	@Autowired
	BattleCommandService battles;

	@Autowired
	CombatVitalsStore vitals;

	@Autowired
	CreatureCommandService creatureCommands;

	/**
	 * 模拟开篇资产耗尽后的恢复路径。仅初始困境由夹具设置，后续恢复、移动、战斗、掉落和补购均走正式服务，
	 * 不注入经验、金币、胜利或捕获事实，也不修改随机种子。裸装人物和初始伙伴使用已有攻击技能，魔力不足时普攻。
	 */
	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "MAGE", "PRIEST", "RANGER" })
	void emptyResourcesCanRecoverFightAndBuyBall(String profession) {
		var p = player(profession);
		beforeReward(p);
		step(p, "BQ004");
		long character = Long.parseLong(characterQueries.find(p.owner()).id());
		long pet = Long.parseLong(creatures.list(p.owner()).getFirst().id());
		creatureCommands.position(p.owner(), pet, 0);
		transactions.run(() -> {
			characters.changeGold(p.owner(), -characterQueries.find(p.owner()).gold());
			em.createNativeQuery(
					"UPDATE owned_item SET quantity=0, pending_quantity=0, equipped=false WHERE character_id=:id")
				.setParameter("id", character)
				.executeUpdate();
			vitals.saveVitals(character, character, 0, 0);
			vitals.saveVitals(character, pet, 0, 0);
		});
		assertTrue(inventoryQueries.list(p.owner()).isEmpty());
		for (int attempt = 0; characterQueries.find(p.owner()).gold() < 8 && attempt < 16; attempt++) {
			long gold = characterQueries.find(p.owner()).gold();
			battles.recover(p.owner());
			assertEquals(gold, characterQueries.find(p.owner()).gold(), "中心恢复不收费");
			for (String location : List.of("SQUARE", "STONE_PATH", "RIVERSIDE"))
				move(p, location);
			var view = battles.start(p.owner(), "VILLAGE", UUID.randomUUID().toString());
			assertTrue(view.units()
				.stream()
				.filter(unit -> !unit.enemy())
				.allMatch(unit -> unit.hp() == unit.stats().health()));
			for (int round = 0; view.status().equals("ACTIVE") && round < 40; round++) {
				String target = view.units()
					.stream()
					.filter(unit -> unit.enemy() && unit.hp() > 0)
					.findFirst()
					.orElseThrow()
					.id();
				var orders = view.units()
					.stream()
					.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
					.map(unit -> new BattleStateOrder(unit.id(), unit.actions()
						.stream()
						.filter(action -> action.available() && action.mana() > 0 && action.target().equals("ENEMY"))
						.map(action -> action.id())
						.findFirst()
						.orElse("ATTACK"), target))
					.toList();
				view = battles.turn(p.owner(), Long.parseLong(view.id()), view.round(), orders, false);
			}
			assertEquals("WON", view.status(), profession + "裸装恢复路线失败：" + view.log());
			for (String location : List.of("STONE_PATH", "SQUARE", "PET_CENTER"))
				move(p, location);
		}
		assertTrue(characterQueries.find(p.owner()).gold() >= 8, "真实野战应能积累补球费用");
		move(p, "SQUARE");
		move(p, "SUPPLY_SHOP");
		long gold = characterQueries.find(p.owner()).gold();
		int balls = inventoryQueries.list(p.owner())
			.stream()
			.filter(item -> item.code().equals("SEAL_I"))
			.mapToInt(item -> item.quantity())
			.sum();
		String request = UUID.randomUUID().toString();
		battles.purchase(p.owner(), "SEAL_I", request);
		battles.purchase(p.owner(), "SEAL_I", request);
		assertEquals(gold - 8, characterQueries.find(p.owner()).gold());
		assertEquals(balls + 1,
				inventoryQueries.list(p.owner())
					.stream()
					.filter(item -> item.code().equals("SEAL_I"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	Player player(String profession) {
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10), username = "q_" + suffix;
		String access = client.post()
			.uri("/api/auth/register")
			.contentType(MediaType.APPLICATION_JSON)
			.body(Map.of("username", username, "password", "Opening-test-1234"))
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(JsonNode.class)
			.getResponseBody()
			.at("/accessToken")
			.asString();
		long owner = accountQueries.accountId(username);
		characters.create(owner, "冒险" + suffix, profession);
		return new Player(owner, access);
	}

	void move(Player p, String destination) {
		characters.move(p.owner(), destination, characterQueries.find(p.owner()).revision());
	}

	void step(Player p, String id) {
		client.post()
			.uri("/api/quests/" + id)
			.header("Authorization", "Bearer " + p.access())
			.exchange()
			.expectStatus()
			.isEqualTo(200);
	}

	void beforeReward(Player p) {
		move(p, "STONE_PATH");
		move(p, "SQUARE");
		accept(p, "BQ001");
		step(p, "BQ001");
		move(p, "ELDER_HOUSE");
		accept(p, "BQ002");
		step(p, "BQ002");
		assertTrue(inventoryQueries.list(p.owner()).stream().anyMatch(i -> i.code().equals("VILLAGE_INTRODUCTION")));
		assertEquals(250, characterQueries.find(p.owner()).experience());
		assertEquals(2, characterQueries.find(p.owner()).level());
		move(p, "SQUARE");
		move(p, "WEAPON_SHOP");
		accept(p, "BQ003");
		step(p, "BQ003");
		assertTrue(inventoryQueries.list(p.owner()).stream().noneMatch(i -> i.code().equals("VILLAGE_INTRODUCTION")));
		assertTrue(inventoryQueries.list(p.owner())
			.stream()
			.anyMatch(i -> i.code().equals("QUEST_WOOD_SWORD") && i.slot() == null));
		move(p, "SQUARE");
		move(p, "PET_CENTER");
		accept(p, "BQ004");
		assertTrue(creatures.list(p.owner()).isEmpty(), "确认结契前不提前发宠");
	}

	void accept(Player p, String step) {
		acceptance.accept(p.owner(), step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(p.owner()).revision()));
	}

	/**
	 * 四职业分别走完整开篇，验证职业武器属性、经验、首宠和任务物品交付。重复最后一步必须重放原响应，离开地点后重试也不能再次发奖。
	 */
	@Test
	void fourProfessionsReceiveTaskExperienceGoldAndPendingItemsWhenBagIsFull() {
		for (var profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			var p = player(profession);
			beforeReward(p);
			transactions.run(() -> {
				// 任务物品独立于六十格背包；填充普通装备以制造真实满包边界。
				for (int i = 0; i < 60; i++)
					inventory.grant(p.owner(), "FULL:" + i, "STARTER_BODY", 1, true);
			});
			var first = quests.interact(p.owner(), "BQ004");
			assertEquals(700, characterQueries.find(p.owner()).experience());
			assertEquals(1, creatures.list(p.owner()).size());
			// 三件基础装备和首次物种收集奖励的一枚封印球全部转入待领。
			assertEquals(4, inventoryQueries.pending(p.owner()).stream().mapToInt(item -> item.quantity()).sum());
			var pendingGear = inventoryQueries.pending(p.owner());
			assertEquals(first, quests.interact(p.owner(), "BQ004"));
			assertEquals(pendingGear, inventoryQueries.pending(p.owner()));
			assertEquals(10, characterQueries.find(p.owner()).gold(), "首次物种奖励只发放一次");
		}
	}

	@Test
	void fourProfessionsReceiveCorrectOnceOnlyRewards() {
		for (String profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			Player p = player(profession);
			beforeReward(p);
			step(p, "BQ004");
			QuestState state = questQueries.state(p.owner());
			assertEquals(List.of("BQ001", "BQ002", "BQ003", "BQ004"), state.completed());
			assertEquals(700, characterQueries.find(p.owner()).experience());
			assertEquals(3, characterQueries.find(p.owner()).level());
			assertEquals(1, creatures.list(p.owner()).size());
			var pet = creatures.list(p.owner()).getFirst();
			assertEquals(3, pet.level());
			assertEquals(700, pet.experience());
			assertTrue(Set.of("A", "B", "H").contains(pet.abilitySlot()));
			assertEquals(pet.abilitySlot().equals("H") ? "回春" : pet.abilitySlot().equals("A") ? "完好防护" : "生命共鸣",
					pet.ability());
			assertEquals(List.of("水弹", "微疗"), pet.skills());
			assertEquals(4, inventoryQueries.list(p.owner()).size());
			assertTrue(inventoryQueries.list(p.owner()).stream().noneMatch(i -> i.code().equals("QUEST_WOOD_SWORD")));
			var weapon = inventoryQueries.list(p.owner())
				.stream()
				.filter(i -> "WEAPON".equals(i.slot()))
				.findFirst()
				.orElseThrow();
			assertEquals(Map.of(Set.of("WARRIOR", "RANGER").contains(profession) ? "physicalAttack" : "magicAttack", 8),
					weapon.stats());
			assertTrue(inventoryQueries.list(p.owner()).stream().noneMatch(i -> i.equipped()));
			for (var item : inventoryQueries.list(p.owner()))
				if (item.slot() != null)
					inventory.equip(p.owner(), Long.parseLong(item.id()));
			assertEquals(3, inventoryQueries.list(p.owner()).stream().filter(i -> i.equipped()).count());
			var first = quests.interact(p.owner(), "BQ004");
			move(p, "SQUARE");
			assertEquals(first, quests.interact(p.owner(), "BQ004"), "已完成命令可重放原响应，无需回到现场再次发奖");
			assertEquals(700, characterQueries.find(p.owner()).experience());
			assertEquals(1, creatures.list(p.owner()).size());
			assertEquals(4, inventoryQueries.list(p.owner()).size());
		}
	}

	/**
	 * 验证未认证、错误地点、缺少前置及他人物品穿戴拒绝。失败后读取独立查询模型，确认没有奖励或物品副作用。
	 */
	@Test
	void rejectsRemoteOutOfOrderAndForeignEquipment() {
		Player p = player("WARRIOR");
		client.post().uri("/api/quests/BQ001").exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri("/api/quests/BQ002")
			.header("Authorization", "Bearer " + p.access())
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		move(p, "STONE_PATH");
		client.post()
			.uri("/api/quests/BQ002")
			.header("Authorization", "Bearer " + p.access())
			.exchange()
			.expectStatus()
			.isEqualTo(409);
		assertTrue(questQueries.state(p.owner()).completed().isEmpty());
		assertEquals(0, characterQueries.find(p.owner()).experience());
		Player other = player("MAGE");
		beforeReward(other);
		step(other, "BQ004");
		var weapon = inventoryQueries.list(other.owner())
			.stream()
			.filter(i -> i.slot() != null)
			.findFirst()
			.orElseThrow();
		client.post()
			.uri("/api/inventory/equipment/" + weapon.id() + "/equip")
			.header("Authorization", "Bearer " + p.access())
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		assertTrue(inventoryQueries.list(p.owner()).isEmpty());
	}

	/**
	 * 并发提交同一领奖步骤，两个请求返回相同回执且库存、伙伴、经验只变化一次。该测试同时覆盖角色锁和任务凭证的实际数据库行为。
	 */
	@Test
	void concurrentFinalSubmissionGrantsOnlyOnce() throws Exception {
		Player p = player("PRIEST");
		beforeReward(p);
		CountDownLatch gate = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Callable<String> call = () -> {
				gate.await();
				return client.post()
					.uri("/api/quests/BQ004")
					.header("Authorization", "Bearer " + p.access())
					.exchange()
					.expectStatus()
					.isEqualTo(200)
					.returnResult(String.class)
					.getResponseBody();
			};
			var a = executor.submit(call);
			var b = executor.submit(call);
			gate.countDown();
			assertEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(700, characterQueries.find(p.owner()).experience());
		assertEquals(1, creatures.list(p.owner()).size());
		assertEquals(4, inventoryQueries.list(p.owner()).size());
	}

	/**
	 * 数据库触发器在最后装备写入阶段制造故障，验证伙伴、图鉴奖励和任务回执整体回滚。finally清除触发器，随后重试成功，防止测试污染其他用例。
	 */
	@Test
	void equipmentFailureRollsBackExperiencePetAndProgressThenRetrySucceeds() {
		Player p = player("RANGER");
		beforeReward(p);
		long id = Long.parseLong(characterQueries.find(p.owner()).id());
		// 仅针对本测试角色拒绝武器写入，故障发生在幻兽及首次图鉴奖励写入之后。
		transactions.run(() -> {
			em.createNativeQuery(
					"CREATE FUNCTION reject_test_reward() RETURNS trigger" + " LANGUAGE plpgsql AS $$ BEGIN IF"
							+ " NEW.character_id=" + id + " AND NEW.code='STARTER_WEAPON' THEN RAISE"
							+ " EXCEPTION 'test reward failure'; END IF;" + " RETURN NEW; END $$")
				.executeUpdate();
			em.createNativeQuery("CREATE TRIGGER reject_test_reward BEFORE INSERT ON"
					+ " owned_item FOR EACH ROW EXECUTE FUNCTION" + " reject_test_reward()")
				.executeUpdate();
		});
		try {
			assertThrows(RuntimeException.class, () -> quests.interact(p.owner(), "BQ004"));
			assertEquals(450, characterQueries.find(p.owner()).experience());
			assertTrue(creatures.list(p.owner()).isEmpty());
			assertEquals(1, inventoryQueries.list(p.owner()).size());
			assertEquals("QUEST_WOOD_SWORD", inventoryQueries.list(p.owner()).getFirst().code(), "奖励写入失败必须恢复已交付的任务信物");
			assertFalse(questQueries.state(p.owner()).completed().contains("BQ004"));
		}
		finally {
			transactions.run(() -> {
				em.createNativeQuery("DROP TRIGGER reject_test_reward ON owned_item").executeUpdate();
				em.createNativeQuery("DROP FUNCTION reject_test_reward()").executeUpdate();
			});
		}
		step(p, "BQ004");
		assertEquals(700, characterQueries.find(p.owner()).experience());
		assertEquals(1, creatures.list(p.owner()).size());
	}

	record Player(long owner, String access) {
	}

}
