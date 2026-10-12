package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.WorkshopCourseCommandService;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseAction;
import io.github.lishangbu.avalon.battle.application.model.WorkshopCourseView;
import io.github.lishangbu.avalon.battle.application.query.WorkshopCourseQueryService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureManagementService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

/** 正式数据库验证课程隔离、门禁、幂等和资源不外溢；不使用假学习结果绕过随机领域规则。 */
class WorkshopCourseFlowTest extends SpringIntegrationTest {

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
	CharacterFactQueryService factQueries;

	@Autowired
	WorkshopCourseCommandService commands;

	@Autowired
	WorkshopCourseQueryService queries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureManagementService management;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	JdbcTemplate jdbc;

	private long ready(String profession) {
		String name = "course_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Course-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			jdbc.update("UPDATE player_character SET location = 'ACADEMY' WHERE id = ?", id);
			facts.record(owner, "QUEST:BQ018");
		});
		return owner;
	}

	private WorkshopCourseView act(long owner, WorkshopCourseView view, String type, String target, Integer position) {
		return commands.act(owner, Long.parseLong(view.id()),
				new WorkshopCourseAction(UUID.randomUUID().toString(), view.revision(), type, target, position));
	}

	@Test
	void learningReconnectAndReplayCannotConsumeAgainOrTouchRealAssets() {
		long owner = ready("MAGE");
		var before = characterQueries.find(owner);
		String key = UUID.randomUUID().toString();
		var view = commands.start(owner, "BQ021", key);
		assertEquals(view.id(), commands.start(owner, "BQ021", key).id());
		assertThrows(BusinessException.class, () -> commands.start(owner, "BQ022", key));
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> inventory.grant(owner, "ILLEGAL", "SEAL_I", 1, false)));
		assertThrows(BusinessException.class, () -> characters.move(owner, "GNOME_CITY", before.revision()));
		view = act(owner, view, "VIEW_POOL", null, null);
		assertTrue(view.skillPool().contains("光幕"));
		var saved = view;
		assertThrows(BusinessException.class, () -> act(owner, saved, "LEARN", "BOOK_ROCK", null));
		assertThrows(BusinessException.class, () -> act(owner, saved, "LEARN", "BOOK_WATER", null));
		assertEquals(saved, queries.get(owner, Long.parseLong(view.id())));
		var request = new WorkshopCourseAction(UUID.randomUUID().toString(), view.revision(), "LEARN",
				"BOOK_LIGHT_SCREEN", null);
		view = commands.act(owner, Long.parseLong(view.id()), request);
		assertEquals(view, commands.act(owner, Long.parseLong(view.id()), request));
		assertEquals(view, queries.courses(owner).active());
		assertTrue(view.skills().contains("光幕"));
		assertTrue(view.learningResult().forgotten().size() <= 1);
		assertFalse(view.learningResult().forgotten().contains("光幕"));
		assertEquals("DONE", view.phase());
		assertFalse(factQueries.codes(owner).contains("TRAINING:BQ021"));
		view = act(owner, view, "FINISH", null, null);
		assertEquals("PASSED", view.status());
		assertTrue(view.items().isEmpty());
		assertTrue(view.skills().isEmpty());
		assertTrue(factQueries.codes(owner).contains("TRAINING:BQ021"));
		assertFalse(factQueries.codes(owner).contains("QUEST:BQ021"));
		assertNull(characterQueries.adventure(owner).course());
		assertEquals(before.gold(), characterQueries.find(owner).gold());
		assertEquals(before.experience(), characterQueries.find(owner).experience());
		long id = Long.parseLong(before.id());
		assertEquals(0,
				jdbc.queryForObject("SELECT count(*) FROM owned_item WHERE character_id = ?", Integer.class, id));
		assertEquals(0,
				jdbc.queryForObject("SELECT count(*) FROM owned_creature WHERE character_id = ?", Integer.class, id));
	}

	@Test
	void equipmentBagAndWarehouseUseRealRulesWithoutHealingOrDuplicatingPets() {
		long owner = ready("MAGE");
		var view = commands.start(owner, "BQ022", UUID.randomUUID().toString());
		assertEquals(306, view.maxHp());
		assertEquals(266, view.hp());
		view = act(owner, view, "INSPECT_EQUIPMENT", null, null);
		view = act(owner, view, "EQUIP", "B", null);
		assertEquals(366, view.maxHp());
		assertEquals(266, view.hp());
		assertEquals(73, view.mp());
		view = act(owner, view, "RESET_VITALS", null, null);
		assertEquals(366, view.hp());
		view = act(owner, view, "UNEQUIP", null, null);
		assertEquals(306, view.hp());
		assertEquals(85, view.mp());
		view = act(owner, view, "START_BAG", null, null);
		assertEquals(60, view.bagSlots());
		var full = view;
		assertThrows(BusinessException.class, () -> act(owner, full, "CLAIM_MANA", null, null));
		assertEquals(full, queries.get(owner, Long.parseLong(view.id())));
		view = act(owner, view, "USE_HEALTH", null, null);
		assertEquals(59, view.bagSlots());
		view = act(owner, view, "CLAIM_MANA", null, null);
		assertEquals(60, view.bagSlots());
		var storage = view;
		assertThrows(BusinessException.class, () -> act(owner, storage, "DEPOSIT", "T_CAT", null));
		var sprout = view.pets().get(2);
		view = act(owner, view, "DEPOSIT", "T_SPROUT", null);
		assertEquals(100, view.pets().get(2).position());
		view = act(owner, view, "WITHDRAW", "T_SPROUT", 2);
		assertEquals(sprout, view.pets().get(2));
		view = act(owner, view, "LOCK", "T_SPROUT", null);
		assertTrue(view.pets().get(2).locked());
		view = act(owner, view, "FINISH", null, null);
		assertEquals("PASSED", view.status());
		assertTrue(view.pets().isEmpty());
		assertTrue(factQueries.codes(owner).contains("TRAINING:BQ022"));
	}

	@Test
	void ownershipRevisionRollbackAndExitKeepSessionAtomic() {
		long owner = ready("WARRIOR");
		long other = ready("PRIEST");
		var view = commands.start(owner, "BQ021", UUID.randomUUID().toString());
		long id = Long.parseLong(view.id());
		assertThrows(BusinessException.class, () -> queries.get(other, id));
		assertThrows(BusinessException.class, () -> act(other, view, "VIEW_POOL", null, null));
		var request = new WorkshopCourseAction(UUID.randomUUID().toString(), view.revision(), "VIEW_POOL", null, null);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.act(owner, id, request);
			throw new IllegalStateException("rollback");
		}));
		assertEquals(view, queries.get(owner, id));
		var updated = commands.act(owner, id, request);
		assertThrows(BusinessException.class, () -> act(owner, view, "VIEW_POOL", null, null));
		var exited = act(owner, updated, "EXIT", null, null);
		assertEquals("EXITED", exited.status());
		assertNull(queries.courses(owner).active());
		assertFalse(factQueries.codes(owner).contains("TRAINING:BQ021"));
		transactions.run(() -> inventory.grant(owner, "AFTER_EXIT", "SEAL_I", 1, false));
	}

	@Test
	void realWarehouseAlsoRejectsActivePetsAndPreservesAnAllowedReserve() {
		long owner = ready("WARRIOR");
		transactions.run(() -> {
			long id = characters.lock(owner).id();
			jdbc.update("UPDATE player_character SET location = 'PET_CENTER' WHERE id = ?", id);
		});
		transactions.run(() -> creatures.grant(owner, "STORAGE_RULE", "SPROUT"));
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		assertThrows(BusinessException.class, () -> management.deposit(owner, pet));
		creatures.position(owner, pet, 2);
		management.deposit(owner, pet);
		assertEquals(100, pets.list(owner).getFirst().partyPosition());
		creatures.position(owner, pet, 2);
		assertEquals(2, pets.list(owner).getFirst().partyPosition());
	}

}
