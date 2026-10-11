package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import java.util.List;
import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 两条地方故事必须汇合；邀请不会传送玩家，领奖要求本人抵达港城登记。 */
class PortInvitationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	SceneActionCommandService scenes;

	@Autowired
	EntityManager em;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	BattleCommandService battles;

	@Autowired
	GrowthRules growth;

	@ParameterizedTest
	@ValueSource(strings = { "BQ039", "BQ056" })
	void bothStoriesAndActualArrivalAreRequired(String first) {
		String name = "port_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Port-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			for (String step : new String[] { "BQ024", "BQ025", "BQ030" })
				facts.record(owner, "QUEST:" + step);
			em.createNativeQuery("UPDATE player_character SET location='GNOME_PORT_ROAD' WHERE id=:id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
		});
		completed(owner, first, 1);
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_CITY"));
		move(owner, "CITY_OUTSKIRTS");
		move(owner, "GNOME_CITY");
		move(owner, "GNOME_ROWAN_SHOP");
		completed(owner, first.equals("BQ039") ? "BQ056" : "BQ039", 2);
		acceptance.accept(owner, "BQ057",
				new QuestAcceptCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision()));
		assertEquals("GNOME_ROWAN_SHOP", queries.find(owner).location());
		assertThrows(BusinessException.class, () -> action(owner, "BQ057_ARRIVAL"));
		move(owner, "GNOME_CITY");
		move(owner, "CITY_OUTSKIRTS");
		move(owner, "GNOME_PORT_ROAD");
		move(owner, "UNDINE_CITY");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ057"));
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_WHARF"));
		action(owner, "BQ057_ARRIVAL");
		var reward = quests.interact(owner, "BQ057");
		assertEquals(reward, quests.interact(owner, "BQ057"));
		assertEquals(8504, queries.find(owner).experience());
		move(owner, "GNOME_PORT_ROAD");
		move(owner, "UNDINE_CITY");
		var supplies = inventory.list(owner);
		move(owner, "UNDINE_WHARF");
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_SHIPYARD"));
		action(owner, "BQ058_PLAN");
		claim(owner, "BQ058");
		move(owner, "UNDINE_SHIPYARD");
		assertFalse(choice(owner, "BQ059_BATCH", "A"));
		assertFalse(choice(owner, "BQ059_BATCH", "C"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ059"));
		assertTrue(choice(owner, "BQ059_BATCH", "B"));
		claim(owner, "BQ059");
		action(owner, "BQ060_FIT");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ060"));
		action(owner, "BQ060_RECHECK");
		claim(owner, "BQ060");
		assertEquals(29763, queries.find(owner).experience());
		assertEquals(supplies, inventory.list(owner), "修船使用船坞任务样料，不消耗玩家库存");
		move(owner, "UNDINE_WHARF");
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_ESTUARY"));
		assertFalse(choice(owner, "BQ061_LOAD", "SIDE"));
		assertFalse(choice(owner, "BQ061_LOAD", "HIGH"));
		assertThrows(BusinessException.class, () -> action(owner, "BQ061_SECURE"));
		assertTrue(choice(owner, "BQ061_LOAD", "BALANCED"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ061"));
		action(owner, "BQ061_SECURE");
		claim(owner, "BQ061");
		move(owner, "UNDINE_ESTUARY");
		claim(owner, "BQ062");
		assertEquals(46769, queries.find(owner).experience());
		assertEquals(supplies, inventory.list(owner), "试载箱不占用玩家库存");
		move(owner, "UNDINE_WHARF");
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_BUOY_ONE"));
		assertThrows(BusinessException.class, () -> action(owner, "BQ063_TEAM"));
		long pet = transactions
			.call(() -> creatures.capture(owner, "PORT_TEST_PET", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		assertThrows(BusinessException.class, () -> action(owner, "BQ063_TEAM"));
		creatures.position(owner, pet, 0);
		action(owner, "BQ063_TEAM");
		assertEquals("UNDINE_WHARF", queries.find(owner).location(), "确认准备并不立即移动");
		move(owner, "UNDINE_BUOY_ONE");
		move(owner, "UNDINE_WHARF");
		move(owner, "UNDINE_BUOY_ONE");
		move(owner, "UNDINE_BUOY_TWO");
		assertEquals("UNDINE_BUOY_TWO", queries.find(owner).location(), "重新查询保留真实航行位置");
		move(owner, "UNDINE_WHARF");
		move(owner, "UNDINE_BUOY_ONE");
		move(owner, "UNDINE_BUOY_TWO");
		move(owner, "UNDINE_BAY");
		claim(owner, "BQ063");
		assertEquals(55272, queries.find(owner).experience());
		move(owner, "UNDINE_WHARF");
		// 战斗专项准备15级队伍；不把此处补经验作为自然升级节奏证据。
		transactions.run(() -> characters.grantExperience(owner,
				Math.toIntExact(growth.threshold(15) - queries.find(owner).experience())));
		for (int position = 0; position < 2; position++) {
			int index = position;
			long fighter = transactions.call(() -> creatures.capture(owner, "PORT_FIGHTER:" + index, "THUNDER_LIZARD",
					15, "A", "完好防护", List.of("雷牙")));
			creatures.position(owner, fighter, position);
		}
		battles.recover(owner);
		move(owner, "UNDINE_BAY");
		assertThrows(BusinessException.class, () -> action(owner, "BQ064_REPAIR"));
		var battle = battles.start(owner, "BAY_MARKER_GUARD", UUID.randomUUID().toString());
		battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), List.of(), true);
		assertEquals("SURRENDERED", battle.status());
		assertEquals("UNDINE_WHARF", queries.find(owner).location());
		battles.recover(owner);
		move(owner, "UNDINE_BAY");
		assertThrows(BusinessException.class, () -> action(owner, "BQ064_REPAIR"));
		battle = battles.start(owner, "BAY_MARKER_GUARD", UUID.randomUUID().toString());
		for (int turn = 0; turn < 40 && battle.status().equals("ACTIVE"); turn++) {
			String target = battle.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(), "ATTACK", target))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ064"));
		action(owner, "BQ064_REPAIR");
		claim(owner, "BQ064");
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "BAY_MARKER_GUARD", UUID.randomUUID().toString()));
		var afterBattleItems = inventory.list(owner);
		// 允许提前返港休整，但捷径不能替代原路航标核验。
		move(owner, "UNDINE_WHARF");
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ065"));
		move(owner, "UNDINE_BAY");
		move(owner, "UNDINE_BUOY_TWO");
		move(owner, "UNDINE_BUOY_ONE");
		move(owner, "UNDINE_WHARF");
		claim(owner, "BQ065");
		action(owner, "BQ066_LOG");
		claim(owner, "BQ066");
		assertEquals(afterBattleItems, inventory.list(owner));
		move(owner, "UNDINE_BUOY_ONE");
		move(owner, "UNDINE_BUOY_TWO");
		move(owner, "UNDINE_BAY");
		move(owner, "UNDINE_WHARF");
	}

	private void claim(long owner, String step) {
		var reward = quests.interact(owner, step);
		assertEquals(reward, quests.interact(owner, step));
	}

	private boolean choice(long owner, String id, String answer) {
		return scenes
			.execute(owner, id,
					new SceneActionCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision(),
							answer))
			.completed();
	}

	private void completed(long owner, String step, int index) {
		transactions.run(() -> {
			facts.record(owner, "QUEST:" + step);
			long id = characters.lock(owner).id();
			em.createNativeQuery(
					"INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)"
							+ " VALUES (:receipt, :id, :step, :quest, true, '{}', now())")
				.setParameter("receipt", -id - index)
				.setParameter("id", id)
				.setParameter("step", step)
				.setParameter("quest", step.equals("BQ039") ? "BS05" : "BS07")
				.executeUpdate();
		});
	}

	private void move(long owner, String location) {
		characters.move(owner, location, queries.find(owner).revision());
	}

	private void action(long owner, String id) {
		assertTrue(scenes
			.execute(owner, id,
					new SceneActionCommandService.Request(UUID.randomUUID().toString(), queries.find(owner).revision(),
							null))
			.completed());
	}

}
