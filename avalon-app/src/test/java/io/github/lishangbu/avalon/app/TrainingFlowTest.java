package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.command.TrainingCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleState.Order;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.TrainingCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 四职业走正式学院战局；夹具仅准备前置课程与真实低资源，验证技能、隔离、并发和回滚。 */
class TrainingFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	BattleCommandService battles;

	@Autowired
	TrainingCommandService training;

	@Autowired
	BattleQueryService queries;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	PartyFactory party;

	@Autowired
	TrainingCatalog catalog;

	@Autowired
	EntityManager em;

	private long ready(String profession, boolean unlocked) {
		String name = "train_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Training-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		transactions.run(() -> {
			characters.grantExperience(owner, 2100);
			creatures.grant(owner, "REAL:CAT", "CLAW_CAT");
			creatures.grant(owner, "REAL:STONE", "STONE_GOLEM");
			if (unlocked)
				facts.record(owner, "QUEST:BQ018");
			long id = characters.lock(owner).id();
			em.createNativeQuery("UPDATE player_character SET location = 'ACADEMY' WHERE id = :id")
				.setParameter("id", id)
				.executeUpdate();
			// 第二只移入仓库，检验训练只奖励真实入场编队。
			em.createNativeQuery(
					"UPDATE owned_creature SET party_position = 100 WHERE character_id = :id AND source = 'REAL:STONE'")
				.setParameter("id", id)
				.executeUpdate();
			combatVitalsStore.saveVitals(id, id, 10, 2);
			for (var pet : pets.list(owner))
				combatVitalsStore.saveVitals(id, Long.parseLong(pet.id()), 11, 3);
		});
		return owner;
	}

	private BattleView start(long owner) {
		return training.startAcademy(owner, "BQ023", List.of(), UUID.randomUUID().toString());
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

	private BattleView complete(long owner, String profession, BattleView state) {
		for (int i = 0; state.status().equals("ACTIVE") && i < 10; i++)
			state = battles.turn(owner, Long.parseLong(state.id()), state.round(), correct(state, profession), false);
		assertEquals("WON", state.status(), state.log().toString());
		return state;
	}

	@Test
	void professionCourseIsAvailableOnlyAtTheAcademy() {
		long owner = ready("WARRIOR", true);
		for (String location : List.of("GNOME_CITY", "ELDER_HOUSE")) {
			transactions.run(
					() -> em.createNativeQuery("UPDATE player_character SET location=:location WHERE account_id=:owner")
						.setParameter("location", location)
						.setParameter("owner", owner)
						.executeUpdate());
			assertThrows(BusinessException.class, () -> start(owner));
		}
	}

	@Test
	void allProfessionsCompleteWithPermanentSkillsOnceAndNoRealResourceChanges() {
		for (var profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			long owner = ready(profession, true);
			var before = characterQueries.find(owner);
			var petsBefore = pets.list(owner);
			var bag = inventory.list(owner);
			var state = start(owner);
			assertTrue(state.training());
			assertTrue(state.supplies().isEmpty());
			assertEquals(state.id(), queries.current(owner).id());
			complete(owner, profession, state);
			assertEquals(before.experience(), characterQueries.find(owner).experience());
			assertEquals(before.gold(), characterQueries.find(owner).gold());
			assertEquals("ACADEMY", characterQueries.find(owner).location());
			assertNull(characterQueries.find(owner).activeBattleId());
			assertEquals(bag, inventory.list(owner));
			for (var pet : pets.list(owner)) {
				var old = petsBefore.stream().filter(p -> p.id().equals(pet.id())).findFirst().orElseThrow();
				assertEquals(old.experience(), pet.experience());
			}
			transactions.run(() -> {
				var real = party.create(owner, false);
				assertTrue(real.getFirst().getSkills().contains(catalog.course(profession).skill()));
				var vitals = combatVitalsRepository.get(Long.parseLong(before.id()));
				assertEquals(10, vitals.getHp());
				assertEquals(2, vitals.getMp());
				for (var pet : petsBefore) {
					assertEquals(11, combatVitalsRepository.get(Long.parseLong(pet.id())).getHp());
					assertEquals(3, combatVitalsRepository.get(Long.parseLong(pet.id())).getMp());
				}
			});
			complete(owner, profession, start(owner));
			assertEquals(before.gold(), characterQueries.find(owner).gold());
			assertEquals(before.experience(), characterQueries.find(owner).experience());
		}
	}

	@Test
	void allProfessionsFailThenRetryWithoutLosingRealResourcesOrDuplicatingRewards() {
		for (var profession : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			long owner = ready(profession, true);
			var before = characterQueries.find(owner);
			var bag = inventory.list(owner);
			var roster = pets.list(owner);
			var vitals = queries.lobby(owner).vitals();
			var state = start(owner);
			for (int round = 0; state.status().equals("ACTIVE") && round < 10; round++) {
				// 全员防御不会达成任何职业目标，使用正式回合超限失败，不修改战局结果。
				var orders = state.units()
					.stream()
					.filter(unit -> !unit.enemy() && unit.hp() > 0)
					.map(unit -> new Order(unit.id(), "DEFEND", unit.id()))
					.toList();
				state = battles.turn(owner, Long.parseLong(state.id()), state.round(), orders, false);
			}
			assertEquals("LOST", state.status(), profession);
			assertEquals(before.gold(), characterQueries.find(owner).gold());
			assertEquals(before.experience(), characterQueries.find(owner).experience());
			assertEquals(bag, inventory.list(owner));
			assertEquals(roster, pets.list(owner));
			assertEquals(vitals, queries.lobby(owner).vitals());
			assertFalse(transactions.call(() -> party.create(owner, false)
				.getFirst()
				.getSkills()
				.contains(catalog.course(profession).skill())));
			complete(owner, profession, start(owner));
			assertEquals(before.gold(), characterQueries.find(owner).gold());
			assertEquals(before.experience(), characterQueries.find(owner).experience());
			complete(owner, profession, start(owner));
			assertEquals(before.gold(), characterQueries.find(owner).gold());
			assertEquals(before.experience(), characterQueries.find(owner).experience());
			assertEquals(bag, inventory.list(owner));
		}
	}

	@Test
	void gatesExitFailureAndInvalidTargetsDoNotConsumeOrUnlock() {
		long owner = ready("MAGE", false);
		assertThrows(BusinessException.class, () -> start(owner));
		transactions.run(() -> facts.record(owner, "QUEST:BQ018"));
		var state = start(owner);
		long id = Long.parseLong(state.id());
		assertThrows(BusinessException.class, () -> battles.turn(owner, id, 1,
				List.of(new Order("trainee", "魔爆术", null, List.of("training-enemy-0", "training-enemy-0"))), false));
		assertThrows(BusinessException.class,
				() -> battles.turn(owner, id, 1, List.of(new Order("trainee", "HEALTH_POTION", "trainee")), false));
		assertEquals(1, queries.current(owner).round());
		var lost = battles.turn(owner, id, 1, List.of(new Order("trainee", "ATTACK", "training-enemy-0")), false);
		assertEquals("LOST", lost.status());
		assertEquals("ACADEMY", characterQueries.find(owner).location());
		var retry = start(owner);
		var exit = battles.turn(owner, Long.parseLong(retry.id()), 1, List.of(), true);
		assertEquals("SURRENDERED", exit.status());
		assertFalse(transactions.call(() -> party.create(owner, false).getFirst().getSkills().contains("魔爆术")));
		assertEquals(2100, characterQueries.find(owner).experience());
		long other = ready("MAGE", true);
		assertThrows(BusinessException.class, () -> battles.turn(other, id, 1, List.of(), true));
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location = 'SQUARE' WHERE account_id = :owner")
				.setParameter("owner", owner)
				.executeUpdate());
		assertThrows(BusinessException.class, () -> start(owner));
	}

	@Test
	void borrowedTrainingDoesNotCreateOrRefillRealResourceArchives() {
		long owner = ready("MAGE", true);
		transactions.run(() -> em.createNativeQuery("DELETE FROM combat_vitals WHERE character_id=:id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		var before = queries.lobby(owner).vitals();
		complete(owner, "MAGE", start(owner));
		assertEquals(before, queries.lobby(owner).vitals());
		assertNull(
				transactions.call(() -> combatVitalsRepository.get(Long.parseLong(characterQueries.find(owner).id()))));
	}

	@Test
	void swapPracticeUsesBorrowedRosterAndRollsBackWithoutChangingRealResources() {
		long owner = ready("WARRIOR", true);
		var before = characterQueries.find(owner);
		var roster = pets.list(owner);
		var bag = inventory.list(owner);
		var vitals = queries.lobby(owner).vitals();
		String request = UUID.randomUUID().toString();
		var selection = List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE");
		var view = training.startAcademy(owner, "BQ019", selection, request);
		assertEquals(view, training.startAcademy(owner, "BQ019", selection, request));
		var orders = List.of(new Order("trainee", "DEFEND", "trainee"), new Order("T_CAT", "SWAP", "T_SPROUT"),
				new Order("T_STONE", "DEFEND", "T_STONE"));
		long battleId = Long.parseLong(view.id());
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			battles.turn(owner, battleId, 1, orders, false);
			throw new IllegalStateException("模拟课程提交失败");
		}));
		assertEquals(1, queries.current(owner).round());
		var won = battles.turn(owner, battleId, 1, orders, false);
		assertEquals("WON", won.status());
		assertEquals(won, battles.turn(owner, battleId, 1, orders, false));
		assertEquals(roster, pets.list(owner));
		assertEquals(bag, inventory.list(owner));
		assertEquals(vitals, queries.lobby(owner).vitals());
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		assertEquals(before.experience(), characterQueries.find(owner).experience());
	}

	@Test
	void swapPracticeRejectsMissingQualificationAndEndsWithoutSuccessAtRoundLimit() {
		long owner = ready("WARRIOR", false);
		var selection = List.of("T_CAT", "T_STONE", "T_SPROUT", "T_GALE");
		assertThrows(BusinessException.class,
				() -> training.startAcademy(owner, "BQ019", selection, UUID.randomUUID().toString()));
		transactions.run(() -> facts.record(owner, "QUEST:BQ018"));
		assertThrows(BusinessException.class, () -> training.startAcademy(owner, "BQ019",
				List.of("T_CAT", "T_CAT", "T_STONE", "T_GALE"), UUID.randomUUID().toString()));
		var state = training.startAcademy(owner, "BQ019", selection, UUID.randomUUID().toString());
		for (int i = 0; i < 20 && state.status().equals("ACTIVE"); i++) {
			var orders = state.units()
				.stream()
				.filter(unit -> !unit.enemy() && unit.hp() > 0 && (unit.hero() || unit.position() < 2))
				.map(unit -> new Order(unit.id(), "DEFEND", unit.id()))
				.toList();
			state = battles.turn(owner, Long.parseLong(state.id()), state.round(), orders, false);
		}
		assertEquals("LOST", state.status());
		assertNull(characterQueries.find(owner).activeBattleId());
	}

	@Test
	void completionRollbackAndConcurrentReplayCreditOnlyOnce() throws Exception {
		long owner = ready("MAGE", true);
		String request = UUID.randomUUID().toString();
		var state = training.startAcademy(owner, "BQ023", List.of(), request);
		assertEquals(state.id(), training.startAcademy(owner, "BQ023", List.of(), request).id());
		var before = characterQueries.find(owner);
		long id = Long.parseLong(state.id());
		var orders = correct(state, "MAGE");
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			battles.turn(owner, id, 1, orders, false);
			throw new IllegalStateException("模拟奖励提交失败");
		}));
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		assertEquals(1, queries.current(owner).round());
		var start = new CountDownLatch(1);
		try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = pool.submit(() -> {
				start.await();
				return battles.turn(owner, id, 1, orders, false);
			});
			var second = pool.submit(() -> {
				start.await();
				return battles.turn(owner, id, 1, orders, false);
			});
			start.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		assertEquals(before.experience(), characterQueries.find(owner).experience());
	}

}
