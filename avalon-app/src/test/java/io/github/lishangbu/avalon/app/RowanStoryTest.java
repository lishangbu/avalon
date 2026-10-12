package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 从开篇完成事实接续小店故事；后续移动、观察、回答和领奖全部使用正式命令。 */
class RowanStoryTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	SceneActionCommandService scenes;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	EntityManager em;

	@Autowired
	BattleCommandService battles;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	GrowthRules growth;

	@Autowired
	QuestQueryService questQueries;

	@Test
	void shelvesInterviewsAndSortingRequireRealActionsAndDoNotConsumePlayerInventory() {
		String name = "rowan_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Rowan-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			facts.record(owner, "QUEST:BQ024");
			facts.record(owner, "QUEST:BQ025");
			facts.record(owner, "QUEST:BQ029");
			em.createNativeQuery("UPDATE player_character SET location='GNOME_CITY' WHERE id=:id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
		});
		assertThrows(BusinessException.class, () -> move(owner, "GNOME_ROWAN_SHOP"));
		transactions.run(() -> {
			facts.record(owner, "QUEST:BQ030");
			em.createNativeQuery(
					"INSERT INTO quest_receipt (id, character_id, step, quest_code, completed, response, created_at)"
							+ " VALUES (:id, :id, 'BQ030', 'BS04', true, '{}', now())")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
		});
		move(owner, "GNOME_ROWAN_SHOP");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ031"));
		acceptance.accept(owner, "BQ031",
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		var first = quests.interact(owner, "BQ031");
		assertEquals(first, quests.interact(owner, "BQ031"));
		move(owner, "GNOME_CITY");
		quests.interact(owner, "BQ032");
		move(owner, "GNOME_ROWAN_SHOP");
		var before = inventory.list(owner);
		var wrong = scenes.execute(owner, "BQ033_BUCKLE", request(owner, "SELL"));
		assertFalse(wrong.completed());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ033"));
		action(owner, "BQ033_BOTTLE", "SELL");
		action(owner, "BQ033_BAG", "REPAIR");
		action(owner, "BQ033_BUCKLE", "DISCARD");
		var finalReceipt = quests.interact(owner, "BQ033");
		assertEquals(finalReceipt, quests.interact(owner, "BQ033"));
		assertEquals(15593, queries.find(owner).experience());
		assertEquals(before, inventory.list(owner));
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		move(owner, "GNOME_PORT_ROAD");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ034"));
		assertFalse(scenes.execute(owner, "BQ034_BATCH", request(owner, "BULK")).completed());
		action(owner, "BQ034_BATCH", "TRAVEL");
		quests.interact(owner, "BQ034");
		move(owner, "CITY_OUTSKIRTS");
		assertFalse(scenes.execute(owner, "BQ035_ROUTE", request(owner, "WATER")).completed());
		action(owner, "BQ035_ROUTE", "HIGH");
		var routeReceipt = quests.interact(owner, "BQ035");
		assertEquals(routeReceipt, quests.interact(owner, "BQ035"));
		assertEquals(28067, queries.find(owner).experience());
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(q -> q.id().equals("BQ036"))
			.findFirst()
			.orElseThrow()
			.guide()
			.contains("河口商道，领取试送货包"));
		assertEquals(before, inventory.list(owner));
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "ROWAN_TRIAL_DELIVERY", UUID.randomUUID().toString()));
		move(owner, "GNOME_PORT_ROAD");
		action(owner, "BQ036_PARCEL", null);
		action(owner, "BQ036_PARCEL", null);
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(q -> q.id().equals("BQ036"))
			.findFirst()
			.orElseThrow()
			.guide()
			.contains("完成「护送罗温的试送货包」战斗"));
		assertEquals(1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("ROWAN_TRIAL_PARCEL"))
					.mapToInt(item -> item.quantity())
					.sum());
		move(owner, "CITY_OUTSKIRTS");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ036"));
		// 剧情专项从既有进度准备12级队伍；战斗使用原始敌人数值和正式回合结算。
		transactions.run(() -> characters.grantExperience(owner,
				Math.toIntExact(growth.threshold(12) - queries.find(owner).experience())));
		for (int i = 0; i < 2; i++) {
			int index = i;
			long pet = transactions.call(() -> creatures.capture(owner, "ROWAN_TEST:" + index, "CLAW_CAT", 12, "A",
					"完好防护", List.of("水弹", "微疗")));
			creatures.position(owner, pet, i);
		}
		move(owner, "GNOME_CITY");
		move(owner, "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		var battle = battles.start(owner, "ROWAN_TRIAL_DELIVERY", UUID.randomUUID().toString());
		// 首次试送故意全队防御直至战败，验证真实伤害触发的撤退，不直接改战局或冒充失败。
		for (int round = 0; round < 250 && battle.status().equals("ACTIVE"); round++) {
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(), "DEFEND", unit.id()))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("LOST", battle.status());
		assertEquals("GNOME_CITY", queries.find(owner).location());
		assertEquals(1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("ROWAN_TRIAL_PARCEL"))
					.mapToInt(item -> item.quantity())
					.sum(),
				"战败不能损毁唯一任务货包");
		move(owner, "CITY_PETS");
		battles.recover(owner);
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ036"));
		move(owner, "GNOME_PORT_ROAD");
		action(owner, "BQ036_PARCEL", null);
		assertEquals(1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("ROWAN_TRIAL_PARCEL"))
					.mapToInt(item -> item.quantity())
					.sum(),
				"失败后回看领取记录不能多发货包");
		move(owner, "CITY_OUTSKIRTS");
		battle = battles.start(owner, "ROWAN_TRIAL_DELIVERY", UUID.randomUUID().toString());
		for (int round = 0; round < 20 && battle.status().equals("ACTIVE"); round++) {
			String enemy = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(),
						unit.actions().stream().anyMatch(a -> a.id().equals("水弹") && a.available()) ? "水弹" : "ATTACK",
						enemy))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status());
		long beforeDelivery = queries.find(owner).experience();
		assertTrue(questQueries.state(owner)
			.available()
			.stream()
			.filter(q -> q.id().equals("BQ036"))
			.findFirst()
			.orElseThrow()
			.guide()
			.contains("交付试送货包并签收"));
		var delivery = quests.interact(owner, "BQ036");
		assertEquals(delivery, quests.interact(owner, "BQ036"));
		assertEquals(beforeDelivery + 9355, queries.find(owner).experience());
		assertTrue(inventory.list(owner).stream().noneMatch(item -> item.code().equals("ROWAN_TRIAL_PARCEL")));
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "ROWAN_TRIAL_DELIVERY", UUID.randomUUID().toString()));
		move(owner, "GNOME_CITY");
		move(owner, "GNOME_ROWAN_SHOP");
		assertThrows(BusinessException.class,
				() -> battles.purchase(owner, "HEALTH_POTION", UUID.randomUUID().toString()));
		for (String suffix : List.of("MEDICINE", "BALL", "REPAIR"))
			action(owner, "BQ037_" + suffix, null);
		quests.interact(owner, "BQ037");
		var trialItems = inventory.list(owner);
		assertFalse(scenes.execute(owner, "BQ038_FIND", request(owner, "HEALTH")).completed());
		action(owner, "BQ038_FIND", "MANA");
		action(owner, "BQ038_USE", "II");
		action(owner, "BQ038_EXCHANGE", "REPLACE");
		quests.interact(owner, "BQ038");
		assertEquals(trialItems, inventory.list(owner), "试营业不消耗玩家库存");
		var opening = quests.interact(owner, "BQ039");
		assertEquals(opening, quests.interact(owner, "BQ039"));
		transactions.run(() -> characters.changeGold(owner, 100));
		long gold = queries.find(owner).gold();
		int potions = inventory.list(owner)
			.stream()
			.filter(item -> item.code().equals("HEALTH_POTION"))
			.mapToInt(item -> item.quantity())
			.sum();
		String purchase = UUID.randomUUID().toString();
		battles.purchase(owner, "HEALTH_POTION", purchase);
		battles.purchase(owner, "HEALTH_POTION", purchase);
		assertEquals(gold - 12, queries.find(owner).gold());
		assertEquals(potions + 1,
				inventory.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	private void move(long owner, String destination) {
		characters.move(owner, destination, queries.find(owner).revision());
	}

	private SceneActionCommandService.Request request(long owner, String choice) {
		return new SceneActionCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision(),
				choice);
	}

	private void action(long owner, String id, String choice) {
		assertTrue(scenes.execute(owner, id, request(owner, choice)).completed());
	}

}
