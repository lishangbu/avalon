package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.command.TrainingCommandService;
import io.github.lishangbu.avalon.battle.application.command.WorkshopCourseCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.application.query.WorkshopCourseQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.QuestAcceptCommandService;
import io.github.lishangbu.avalon.world.application.command.QuestCommandService;
import io.github.lishangbu.avalon.world.application.command.SceneActionCommandService;
import io.github.lishangbu.avalon.world.application.model.QuestAcceptRequest;
import io.github.lishangbu.avalon.world.application.query.QuestQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 正式开篇的可复用操作路线；只调用真实服务，不直接写任务、经验或战斗结果。 */
class FormalChapterJourney {

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	QuestQueryService questsRead;

	@Autowired
	QuestCommandService quests;

	@Autowired
	QuestAcceptCommandService questAccepts;

	@Autowired
	SceneActionCommandService sceneActions;

	@Autowired
	BattleCommandService battles;

	@Autowired
	TrainingCommandService academy;

	@Autowired
	WorkshopCourseCommandService workshops;

	@Autowired
	WorkshopCourseQueryService workshopQueries;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService inventoryQueries;

	void complete(long owner) {
		reachAcademy(owner);
		assertTrue(questsRead.done(owner, "BQ017"));
		// 入学前七部位装备齐备，但奖励不会替玩家自动穿戴；不足等级的物品仍留在背包。
		var arrivalGear = inventoryQueries.list(owner).stream().filter(item -> item.slot() != null).toList();
		assertTrue(arrivalGear.stream()
			.map(item -> item.slot())
			.collect(Collectors.toSet())
			.containsAll(Set.of("WEAPON", "HEAD", "BODY", "HANDS", "LEGS", "FEET", "ACCESSORY")));
		for (String code : List.of("JOURNEY_BODY", "JOURNEY_FEET", "JOURNEY_HANDS", "JOURNEY_LEGS",
				"JOURNEY_ACCESSORY")) {
			var item = arrivalGear.stream().filter(gear -> gear.code().equals(code)).findFirst().orElseThrow();
			assertEquals(7, item.requiredLevel());
			assertTrue(!item.equipped());
		}

		completeCommandCourse(owner, "BQ018", List.of());
		completeTask(owner, "BQ018");
		completeCommandCourse(owner, "BQ019", List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE"));
		completeTask(owner, "BQ019");
		completeCaptureCourse(owner);
		completeTask(owner, "BQ020");
		completeLearningWorkshop(owner);
		completeTask(owner, "BQ021");
		move(owner, "ACADEMY_PETS");
		var petCourses = workshopQueries.courses(owner);
		assertTrue(petCourses.courses().stream().anyMatch(course -> course.id().equals("BQ022") && course.available()));
		assertTrue(
				petCourses.courses().stream().anyMatch(course -> course.id().equals("BQ021") && !course.available()));
		completeInventoryWorkshop(owner);
		move(owner, "ACADEMY");
		move(owner, "ACADEMY_PETS");
		completeTask(owner, "BQ022");
		move(owner, "ACADEMY");
		completeProfessionCourse(owner);
		completeTask(owner, "BQ023");

		assertTrue(facts.codes(owner)
			.containsAll(List.of("TRAINING:BQ018", "TRAINING:BQ019", "TRAINING:BQ020", "TRAINING:BQ021",
					"TRAINING:BQ022", "TRAINING:BQ023", "SKILL:BQ023_GRANTED")));
		for (String quest : List.of("BQ018", "BQ019", "BQ020", "BQ021", "BQ022", "BQ023"))
			assertTrue(questsRead.done(owner, quest), quest + " 回执未完成");
		completeIndependentInvestigation(owner);
	}

	/** 课程后的调查必须完成真实编队、恢复、场景选择和野外战斗，不能注入任务事实跳过流程。 */
	private void completeIndependentInvestigation(long owner) {
		acceptTask(owner, "BQ024");
		var starter = pets.list(owner).getFirst();
		creatures.position(owner, Long.parseLong(starter.id()), starter.partyPosition());
		move(owner, "ACADEMY_PETS");
		battles.recover(owner);
		move(owner, "ACADEMY");
		String plan = characterQueries.find(owner).profession().equals("MAGE") ? "FOOD_FIRST" : "HABITAT_FIRST";
		scene(owner, "BQ024_PLAN", plan);
		completeTask(owner, "BQ024");
		move(owner, "GNOME_CITY");
		completeTask(owner, "BQ025");
		move(owner, "CITY_OUTSKIRTS");
		// 观察顺序由玩家决定；先看花期也不能绕过其余两项记录直接清路。
		assertThrows(BusinessException.class, () -> scene(owner, "BQ027_CLEAR_PATH"));
		assertThrows(BusinessException.class, () -> scene(owner, "BQ027_CLEAR_PATH"));
		completeTask(owner, "BQ026");
		assertThrows(BusinessException.class, () -> battles.start(owner, "PATH_GUARD", UUID.randomUUID().toString()));
		scene(owner, "BQ027_CLEAR_PATH");
		var battle = battles.start(owner, "PATH_GUARD", UUID.randomUUID().toString());
		assertTrue(battle.units()
			.stream()
			.filter(BattleView.UnitView::enemy)
			.allMatch(unit -> unit.level() == 8 && unit.name().equals("小岩怪")));
		for (int attempt = 0; attempt < 60 && battle.status().equals("ACTIVE"); attempt++) {
			String target = battle.units()
				.stream()
				.filter(unit -> unit.enemy() && unit.hp() > 0)
				.findFirst()
				.orElseThrow()
				.id();
			var orders = battle.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(), unit.actions()
					.stream()
					.filter(action -> action.available() && action.mana() > 0 && action.target().equals("ENEMY"))
					.map(action -> action.id())
					.findFirst()
					.orElse("ATTACK"), target))
				.toList();
			battle = battles.turn(owner, Long.parseLong(battle.id()), battle.round(), orders, false);
		}
		assertEquals("WON", battle.status(), "城郊调查遭遇未完成：" + battle.log());
		completeTask(owner, "BQ027");
		move(owner, "GNOME_CITY");
		move(owner, "ACADEMY");
		scene(owner, "BQ028_REPORT", "KEEP_HABITAT");
		completeTask(owner, "BQ028");
		completeTask(owner, "BQ029");
		move(owner, "GNOME_CITY");
		scene(owner, "BQ030_MEAL");
		completeTask(owner, "BQ030");
		assertTrue(questsRead.state(owner).completed().contains("BS04"), "前端完成提示使用整项任务编号");
		assertTrue(facts.codes(owner).contains("VICTORY:BQ027_PATH_GUARD"));
		// 独立冒险之后仍能沿相邻道路返乡，村庄不能因完成小站照护而永久关闭。
		for (String location : List.of("ACADEMY", "STATION_OUTSKIRTS", "PLAIN_SIGN", "FOREST_EDGE", "RIVERSIDE",
				"STONE_PATH", "SQUARE", "PET_CENTER"))
			move(owner, location);
		battles.recover(owner);
		assertEquals("PET_CENTER", characterQueries.find(owner).location());
	}

	private void completeCommandCourse(long owner, String course, List<String> selected) {
		var view = academy.startAcademy(owner, course, selected, UUID.randomUUID().toString());
		long id = Long.parseLong(view.id());
		for (int attempt = 0; view.status().equals("ACTIVE") && attempt < 24; attempt++) {
			var orders = new ArrayList<Order>();
			for (var unit : view.units()) {
				if (unit.enemy() || unit.hp() <= 0 || !unit.hero() && unit.position() >= 2)
					continue;
				if (course.equals("BQ019") && unit.id().equals("T_CAT"))
					orders.add(new Order(unit.id(), "SWAP", "T_SPROUT"));
				else
					orders.add(new Order(unit.id(), "DEFEND", unit.id()));
			}
			if (course.equals("BQ018")) {
				orders.clear();
				for (var unit : view.units()) {
					if (unit.enemy() || unit.hp() <= 0 || !unit.hero() && unit.position() >= 2)
						continue;
					boolean defend = unit.hero() && view.round() == 2;
					orders
						.add(new Order(unit.id(), defend ? "DEFEND" : "ATTACK", defend ? unit.id() : "academy-target"));
				}
			}
			view = battles.turn(owner, id, view.round(), orders, false);
		}
		assertEquals("WON", view.status(), course + " 未完成：" + view.log());
	}

	private void completeCaptureCourse(long owner) {
		BattleView view = null;
		for (int attempt = 0; attempt < 8; attempt++) {
			view = academy.startAcademy(owner, "BQ020", List.of(), UUID.randomUUID().toString());
			long id = Long.parseLong(view.id());
			view = academy.captureInfo(owner, id, "SEAL_I");
			for (int round = 0; view.status().equals("ACTIVE") && round < 20; round++) {
				var target = view.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow();
				var orders = new ArrayList<Order>();
				for (var unit : view.units()) {
					if (unit.enemy() || unit.hp() <= 0 || (!unit.hero() && unit.position() >= 2))
						continue;
					// 水弹已按新七维提高伤害；目标低于六成生命便停手捕捉，避免机械连打误杀。
					if (unit.hero() && target.hp() <= target.stats().health() * 0.60)
						orders.add(new Order(unit.id(), "SEAL_I", target.id()));
					else if (!unit.hero() && target.hp() > target.stats().health() * 0.60
							&& unit.actions()
								.stream()
								.anyMatch(action -> action.id().equals("水弹") && action.available()))
						orders.add(new Order(unit.id(), "水弹", target.id()));
					else
						orders.add(new Order(unit.id(), "DEFEND", unit.id()));
				}
				view = battles.turn(owner, id, view.round(), orders, false);
			}
			if (view.status().equals("WON"))
				return;
			if (view.status().equals("ACTIVE"))
				battles.turn(owner, id, view.round(), List.of(), true);
		}
		assertEquals("WON", view.status(), "BQ020 捕捉课程没有在正式概率下成功：" + view.log());
	}

	private void completeLearningWorkshop(long owner) {
		var view = workshops.start(owner, "BQ021", UUID.randomUUID().toString());
		view = workshop(owner, view, "VIEW_POOL", null, null);
		view = workshop(owner, view, "LEARN", "BOOK_LIGHT_SCREEN", null);
		view = workshop(owner, view, "FINISH", null, null);
		assertEquals("PASSED", view.status());
	}

	private void completeInventoryWorkshop(long owner) {
		var view = workshops.start(owner, "BQ022", UUID.randomUUID().toString());
		view = workshop(owner, view, "INSPECT_EQUIPMENT", null, null);
		view = workshop(owner, view, "EQUIP", "B", null);
		view = workshop(owner, view, "RESET_VITALS", null, null);
		view = workshop(owner, view, "UNEQUIP", null, null);
		view = workshop(owner, view, "START_BAG", null, null);
		view = workshop(owner, view, "USE_HEALTH", null, null);
		view = workshop(owner, view, "CLAIM_MANA", null, null);
		view = workshop(owner, view, "DEPOSIT", "T_SPROUT", null);
		view = workshop(owner, view, "WITHDRAW", "T_SPROUT", 2);
		view = workshop(owner, view, "LOCK", "T_SPROUT", null);
		view = workshop(owner, view, "FINISH", null, null);
		assertEquals("PASSED", view.status());
	}

	private void completeProfessionCourse(long owner) {
		var view = academy.startAcademy(owner, "BQ023", List.of(), UUID.randomUUID().toString());
		String profession = characterQueries.find(owner).profession();
		for (int attempt = 0; view.status().equals("ACTIVE") && attempt < 12; attempt++)
			view = battles.turn(owner, Long.parseLong(view.id()), view.round(), correct(view, profession), false);
		assertEquals("WON", view.status(), "BQ023 未完成：" + view.log());
	}

	private List<Order> correct(BattleView view, String profession) {
		var orders = new ArrayList<Order>();
		for (var unit : view.units()) {
			if (unit.enemy() || unit.hp() <= 0)
				continue;
			String action = "DEFEND", target = unit.id();
			if (unit.hero()) {
				if (profession.equals("WARRIOR") && view.round() == 1)
					action = "盾墙";
				if (profession.equals("MAGE")) {
					orders.add(new Order(unit.id(), "魔爆术", null,
							view.units()
								.stream()
								.filter(BattleView.UnitView::enemy)
								.map(BattleView.UnitView::id)
								.toList()));
					continue;
				}
				if (profession.equals("PRIEST") && List.of(1, 4, 7).contains(view.round())) {
					action = "恢复";
					target = view.round() == 1 ? "trainee" : "patient-" + (view.round() == 4 ? 0 : 1);
				}
				if (profession.equals("RANGER")) {
					action = view.round() == 1 ? "猎人印记" : "ATTACK";
					target = "training-enemy-0";
				}
			}
			else if (profession.equals("RANGER") && view.round() > 1) {
				action = "ATTACK";
				target = "training-enemy-0";
			}
			orders.add(new Order(unit.id(), action, target));
		}
		return orders;
	}

	private WorkshopCourseView workshop(long owner, WorkshopCourseView view, String type, String target,
			Integer position) {
		return workshops.act(owner, Long.parseLong(view.id()), new WorkshopCourseCommandService.Action(
				UUID.randomUUID().toString(), view.revision(), type, target, position));
	}

	private void completeTask(long owner, String quest) {
		quests.interact(owner, quest);
		assertTrue(questsRead.done(owner, quest), quest + " 未完成");
	}

	private void acceptTask(long owner, String step) {
		questAccepts.accept(owner, step,
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
	}

	private void reachAcademy(long owner) {
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		acceptTask(owner, "BQ001");
		quests.interact(owner, "BQ001");
		move(owner, "ELDER_HOUSE");
		acceptTask(owner, "BQ002");
		quests.interact(owner, "BQ002");
		move(owner, "SQUARE");
		move(owner, "WEAPON_SHOP");
		acceptTask(owner, "BQ003");
		quests.interact(owner, "BQ003");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		acceptTask(owner, "BQ004");
		quests.interact(owner, "BQ004");
		acceptTask(owner, "BQ005");
		var starter = pets.list(owner).getFirst();
		creatures.position(owner, Long.parseLong(starter.id()), starter.partyPosition());
		creatures.viewedSkills(owner, Long.parseLong(starter.id()));
		for (var equipment : inventoryQueries.list(owner))
			if (equipment.slot() != null)
				inventory.equip(owner, Long.parseLong(equipment.id()));
		quests.interact(owner, "BQ005");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		acceptTask(owner, "BQ006");
		quests.interact(owner, "BQ006");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		move(owner, "PET_CENTER");
		questAccepts.accept(owner, "BQ007",
				new QuestAcceptRequest(UUID.randomUUID().toString(), characterQueries.find(owner).revision()));
		long captured = OpeningWildTraining.capture(owner, characters, characterQueries, battles, pets);
		creatures.position(owner, captured, 1);
		creatures.registerCapture(owner);
		assertTrue(facts.codes(owner).contains("CAPTURE:BQ007_REGISTERED"));
		quests.interact(owner, "BQ007");
		move(owner, "SQUARE");
		scene(owner, "BQ008_COLLECT_STONES");
		quests.interact(owner, "BQ008");
		move(owner, "PET_CENTER");
		acceptTask(owner, "BQ009");
		battles.recover(owner);
		quests.interact(owner, "BQ009");
		OpeningWildTraining.toLevel(owner, 5, characters, characterQueries, battles);
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		move(owner, "FOREST_EDGE");
		quests.interact(owner, "BQ010");
		move(owner, "CAVE_ENTRANCE");
		completeCaveBoss(owner);
		assertTrue(questsRead.done(owner, "BQ011"));
		quests.interact(owner, "BQ012");
		move(owner, "FOREST_EDGE");
		move(owner, "RIVERSIDE");
		move(owner, "STONE_PATH");
		move(owner, "SQUARE");
		quests.interact(owner, "BQ013");
		move(owner, "ELDER_HOUSE");
		quests.interact(owner, "BQ014");
		move(owner, "SQUARE");
		move(owner, "STONE_PATH");
		move(owner, "RIVERSIDE");
		move(owner, "FOREST_EDGE");
		move(owner, "PLAIN_SIGN");
		acceptTask(owner, "BQ015");
		quests.interact(owner, "BQ015");
		move(owner, "STATION_OUTSKIRTS");
		scene(owner, "BQ016_DELIVER_CARE");
		quests.interact(owner, "BQ016");
		move(owner, "ACADEMY");
		scene(owner, "BQ017_PRESENT_INTRO");
		quests.interact(owner, "BQ017");
	}

	/** 按首领预告防御，其余回合由人物和两只在场幻兽手动输出。 */
	private void completeCaveBoss(long owner) {
		var view = battles.start(owner, "CAVE_BOSS", UUID.randomUUID().toString());
		for (int attempt = 0; attempt < 60 && view.status().equals("ACTIVE"); attempt++) {
			String boss = view.units().stream().filter(BattleView.UnitView::enemy).findFirst().orElseThrow().id();
			boolean danger = view.telegraph() != null && view.telegraph().contains("即将");
			var orders = new ArrayList<Order>();
			for (var unit : view.units()) {
				if (unit.enemy() || unit.hp() <= 0 || (!unit.hero() && unit.position() >= 2))
					continue;
				var injured = view.units()
					.stream()
					.filter(member -> !member.enemy() && member.hp() > 0 && (member.hero() || member.position() < 2))
					.min(Comparator.comparingInt(member -> member.hp() * 100 / member.stats().health()))
					.orElse(unit);
				String action = danger ? "DEFEND" : "ATTACK";
				String target = danger ? unit.id() : boss;
				if (unit.hero() && injured.hp() * 100 < injured.stats().health() * 45
						&& unit.actions()
							.stream()
							.anyMatch(value -> value.id().equals("HEALTH_POTION") && value.available())) {
					action = "HEALTH_POTION";
					target = injured.id();
				}
				else if (!unit.hero() && injured.hp() * 100 < injured.stats().health() * 55
						&& unit.actions().stream().anyMatch(value -> value.id().equals("微疗") && value.available())) {
					action = "微疗";
					target = injured.id();
				}
				if (!danger && action.equals("ATTACK")) {
					var skill = unit.actions()
						.stream()
						.filter(value -> value.available() && value.target().equals("ENEMY")
								&& !value.id().equals("ATTACK"))
						.findFirst();
					if (skill.isPresent())
						action = skill.get().id();
				}
				orders.add(new Order(unit.id(), action, target));
			}
			view = battles.turn(owner, Long.parseLong(view.id()), view.round(), orders, false);
		}
		assertEquals("WON", view.status(), "洞穴首领未完成：" + view.log());
	}

	private void move(long owner, String destination) {
		characters.move(owner, destination, characterQueries.find(owner).revision());
	}

	private void scene(long owner, String action) {
		scene(owner, action, null);
	}

	private void scene(long owner, String action, String choice) {
		sceneActions.execute(owner, action, new SceneActionCommandService.Request(UUID.randomUUID().toString(),
				characterQueries.find(owner).revision(), choice));
	}

}
