package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.model.SceneActionRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import io.github.lishangbu.avalon.world.application.query.SceneActionQueryService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 四职业的放归闭环：资料只读、答错可改、撤退可恢复；团聚不消耗或赠送幻兽。 */
class HarborReunionTest extends SpringIntegrationTest {

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
	QuestCatalog catalog;

	@Autowired
	QuestAcceptCommandService acceptance;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestQueryService questQueries;

	@Autowired
	SceneActionCommandService actions;

	@Autowired
	SceneActionQueryService scenes;

	@Autowired
	BattleCommandService battles;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	GrowthRules growth;

	@Autowired
	EntityManager em;

	@ParameterizedTest
	@ValueSource(strings = { "WARRIOR", "MAGE", "PRIEST", "RANGER" })
	void reunionRequiresDecisionsSafePassageAndRealRelease(String profession) {
		String name = "reunion_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Reunion-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			em.createNativeQuery("UPDATE player_character SET location='UNDINE_WHARF' WHERE id=:id")
				.setParameter("id", id)
				.executeUpdate();
		});
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_CITY"));
		// 只准备试航结束存档；本段八步不写事实或奖励夹具。
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			for (var step : catalog.definition().steps()) {
				int number = Integer.parseInt(step.id().substring(2));
				if (number > 66)
					continue;
				factCommands.record(owner, "QUEST:" + step.id());
				for (String fact : step.objectives().stream().map(goal -> goal.code()).toList())
					factCommands.record(owner, fact);
				em.createNativeQuery(
						"INSERT INTO quest_receipt(id, character_id, step, quest_code, completed, response, created_at)"
								+ " VALUES (:receipt, :id, :step, :quest, :complete, '{}', now())")
					.setParameter("receipt", -id - number)
					.setParameter("id", id)
					.setParameter("step", step.id())
					.setParameter("quest", step.quest())
					.setParameter("complete", step.completes())
					.executeUpdate();
			}
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
		});
		for (int position = 0; position < 2; position++) {
			String species = position == 0 ? "THUNDER_LIZARD" : "STONE_GOLEM";
			long pet = transactions.call(() -> creatures.capture(owner, "REUNION_FIXTURE:" + species, species, 15, "A",
					"坚岩", List.of(species.equals("THUNDER_LIZARD") ? "雷牙" : "碎岩击")));
			creatures.position(owner, pet, position);
		}
		var petIds = creatureQueries.list(owner).stream().map(pet -> pet.id()).toList();
		move(owner, "UNDINE_CITY");
		move(owner, "UNDINE_CARE_STATION");
		var beforeRead = queries.find(owner);
		var beforeFacts = facts.codes(owner);
		assertTrue(scenes.state(owner).information().stream().anyMatch(entry -> entry.id().equals("BS09_CARE")));
		assertEquals(beforeRead, queries.find(owner));
		assertEquals(beforeFacts, facts.codes(owner));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ067"));
		assertThrows(BusinessException.class, () -> choose(owner, "BQ068_BEHAVIOR", "FOLLOW_CALL"));
		assertThrows(BusinessException.class, () -> move(owner, "UNDINE_SHALLOWS"));
		move(owner, "UNDINE_CITY");
		move(owner, "UNDINE_WHARF");
		acceptance.accept(owner, "BQ067",
				new QuestAcceptRequest(UUID.randomUUID().toString(), queries.find(owner).revision()));
		move(owner, "UNDINE_CITY");
		move(owner, "UNDINE_CARE_STATION");
		claim(owner, "BQ067");
		var supplies = inventory.list(owner);
		assertFalse(choose(owner, "BQ068_BEHAVIOR", "KEEP"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ068"));
		assertTrue(choose(owner, "BQ068_BEHAVIOR", "FOLLOW_CALL"));
		claim(owner, "BQ068");
		move(owner, "UNDINE_SHALLOWS");
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "SHALLOWS_WILD", UUID.randomUUID().toString()));
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "SHALLOWS_PASSAGE_GUARD", UUID.randomUUID().toString()));
		assertFalse(choose(owner, "BQ069_GROUP", "INNER"));
		assertTrue(choose(owner, "BQ069_GROUP", "OUTER"));
		claim(owner, "BQ069");
		move(owner, "UNDINE_ESTUARY");
		assertFalse(choose(owner, "BQ070_ROUTE", "WEST"));
		assertTrue(choose(owner, "BQ070_ROUTE", "EAST"));
		claim(owner, "BQ070");
		move(owner, "UNDINE_SHALLOWS");
		move(owner, "UNDINE_CARE_STATION");
		act(owner, "BQ071_PLAN");
		claim(owner, "BQ071");
		assertEquals(supplies, inventory.list(owner), "路线选择和放归安排不消耗玩家药品、食物或球");
		move(owner, "UNDINE_SHALLOWS");
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "SHALLOWS_PASSAGE_GUARD", UUID.randomUUID().toString()));
		act(owner, "BQ072_NET");
		assertThrows(BusinessException.class, () -> act(owner, "BQ072_EXIT"));
		var retreat = battles.start(owner, "SHALLOWS_PASSAGE_GUARD", UUID.randomUUID().toString());
		retreat = battles.turn(owner, Long.parseLong(retreat.id()), retreat.round(), List.of(), true);
		assertEquals("SURRENDERED", retreat.status());
		assertEquals("UNDINE_CARE_STATION", queries.find(owner).location());
		assertFalse(facts.codes(owner).contains("VICTORY:SHALLOWS_PASSAGE_GUARD"));
		assertTrue(facts.codes(owner).contains("SCENE:BQ072_NET"));
		battles.recover(owner);
		move(owner, "UNDINE_SHALLOWS");
		var battle = battles.start(owner, "SHALLOWS_PASSAGE_GUARD", UUID.randomUUID().toString());
		for (int turn = 0; turn < 60 && battle.status().equals("ACTIVE"); turn++) {
			String target = battle.units()
				.stream()
				.filter(unit -> unit.enemy() && unit.hp() > 0)
				.findFirst()
				.orElseThrow()
				.id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new BattleStateOrder(unit.id(), "ATTACK", target))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status(), battle.log().toString());
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ072"));
		act(owner, "BQ072_EXIT");
		claim(owner, "BQ072");
		assertThrows(BusinessException.class,
				() -> battles.start(owner, "SHALLOWS_PASSAGE_GUARD", UUID.randomUUID().toString()));
		var afterBattleItems = inventory.list(owner);
		assertFalse(choose(owner, "BQ073_GUIDE", "CARRY"));
		assertTrue(choose(owner, "BQ073_GUIDE", "STEP_ASIDE"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ073"));
		act(owner, "BQ073_REUNION");
		claim(owner, "BQ073");
		assertTrue(scenes.state(owner).information().stream().anyMatch(entry -> entry.id().equals("BS09_REUNITED")));
		assertFalse(scenes.state(owner).information().stream().anyMatch(entry -> entry.id().equals("BS09_SIGNALS")));
		move(owner, "UNDINE_CARE_STATION");
		assertThrows(BusinessException.class, () -> act(owner, "BQ074_REGISTER"));
		assertThrows(BusinessException.class, () -> quests.interact(owner, "BQ074"));
		move(owner, "UNDINE_SHALLOWS");
		act(owner, "BQ074_MARKERS");
		move(owner, "UNDINE_CARE_STATION");
		act(owner, "BQ074_REGISTER");
		claim(owner, "BQ074");
		assertTrue(questQueries.state(owner).completed().contains("BS09"));
		assertEquals(afterBattleItems, inventory.list(owner));
		assertEquals(petIds, creatureQueries.list(owner).stream().map(pet -> pet.id()).toList());
		assertTrue(scenes.state(owner).information().stream().anyMatch(entry -> entry.id().equals("BS09_HANDOVER")));
		assertFalse(scenes.state(owner).information().stream().anyMatch(entry -> entry.id().equals("BS09_CARE")));
		battles.recover(owner);
		move(owner, "UNDINE_SHALLOWS");
		var exploration = battles.start(owner, "SHALLOWS_WILD", UUID.randomUUID().toString());
		assertEquals("ACTIVE", exploration.status(), "团聚交接后开放普通浅滩探索");
		battles.turn(owner, Long.parseLong(exploration.id()), exploration.round(), List.of(), true);
		assertEquals("UNDINE_CARE_STATION", queries.find(owner).location());
	}

	private void move(long owner, String location) {
		characters.move(owner, location, queries.find(owner).revision());
	}

	private void act(long owner, String id) {
		assertTrue(choose(owner, id, null));
	}

	private boolean choose(long owner, String id, String choice) {
		return actions
			.execute(owner, id,
					new SceneActionRequest(UUID.randomUUID().toString(), queries.find(owner).revision(), choice))
			.completed();
	}

	private void claim(long owner, String id) {
		var result = quests.interact(owner, id);
		var beforeReplay = queries.find(owner);
		assertEquals(result, quests.interact(owner, id));
		assertEquals(beforeReplay.experience(), queries.find(owner).experience());
		assertEquals(beforeReplay.gold(), queries.find(owner).gold());
	}

}
