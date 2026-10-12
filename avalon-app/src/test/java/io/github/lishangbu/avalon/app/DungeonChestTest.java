package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.DungeonChestCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.DungeonChestQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.DungeonChestRules;
import io.github.lishangbu.avalon.inventory.domain.DungeonChestRulesExtra;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 使用真实数据库验证宝箱原子性；指定随机序列覆盖权重边界与重复项合并。 */
class DungeonChestTest extends SpringIntegrationTest {

	@Autowired
	EntityManager entranceFixture;

	@Autowired
	GrowthRules growth;

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	DungeonRunCommandService runs;

	@Autowired
	DungeonChestCommandService commands;

	@Autowired
	DungeonChestQueryService queries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	DungeonChestRules rules;

	private long owner() {
		String name = "chest_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Chest-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.grantExperience(owner, Math.toIntExact(growth.threshold(15)));
			facts.record(owner, "QUEST:BQ029");
			for (int index = 0; index < 4; index++)
				creatures.grant(owner, "PET:" + index, "CLAW_CAT");
		});
		transactions.run(() -> entranceFixture
			.createNativeQuery("UPDATE player_character SET location='FOREST_PEN' WHERE account_id=:owner")
			.setParameter("owner", owner)
			.executeUpdate());
		return owner;
	}

	@Test
	void claimIsAtomicOwnedAndIdempotentWithOverflow() {
		var owners = List.of(owner(), owner(), owner());
		long owner = owners.getFirst();
		long run = transactions.call(() -> runs.create(owner, owners, UUID.randomUUID().toString()));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.award(owner, run, 91);
			throw new IllegalStateException("模拟胜利事务失败");
		}));
		assertTrue(queries.list(owner).isEmpty());
		transactions.run(() -> {
			commands.award(owner, run, 91);
			commands.award(owner, run, 92);
		});
		long chest = Long.parseLong(queries.list(owner).getFirst().id());
		assertEquals(1, queries.list(owner).size());
		assertThrows(BusinessException.class, () -> commands.claim(owners.get(1), chest));
		assertThrows(BusinessException.class, () -> commands.claim(owner, chest));
		runs.leave(run, owner);
		transactions.run(() -> {
			for (int index = 0; index < 60; index++)
				inventory.grant(owner, "FILL:" + index, "STARTER_BODY", 1, true);
		});
		long before = characterQueries.find(owner).gold();
		var beforePending = items.pending(owner);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.claim(owner, chest);
			throw new IllegalStateException("模拟开箱后失败");
		}));
		assertEquals(1, queries.list(owner).size());
		assertEquals(beforePending, items.pending(owner));
		assertEquals(before, characterQueries.find(owner).gold());
		commands.claim(owner, chest);
		var pending = items.pending(owner);
		assertTrue(pending.size() > beforePending.size());
		assertTrue(queries.list(owner).isEmpty());
		long after = characterQueries.find(owner).gold();
		commands.claim(owner, chest);
		assertEquals(after, characterQueries.find(owner).gold());
		assertEquals(pending, items.pending(owner));
	}

	@Test
	void weaponsStayFixedAndEnforceProfession() {
		long owner = owner();
		transactions.run(() -> {
			inventory.grantRewardEquipment(owner, "TEST:STAFF", "CRYSTAL_STAFF", 24);
			inventory.grantRewardEquipment(owner, "TEST:SWORD", "CRYSTAL_SWORD", 24);
		});
		var staff = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("CRYSTAL_STAFF"))
			.findFirst()
			.orElseThrow();
		assertEquals("驯兽法杖", staff.name());
		assertEquals(24, staff.stats().get("magicAttack"));
		assertEquals("MAGE", staff.requiredProfession());
		assertEquals(24, staff.sellPrice());
		assertTrue(staff.bound());
		assertThrows(BusinessException.class, () -> inventory.equip(owner, Long.parseLong(staff.id())));
		var sword = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("CRYSTAL_SWORD"))
			.findFirst()
			.orElseThrow();
		inventory.equip(owner, Long.parseLong(sword.id()));
	}

	@Test
	void poolBoundariesAndDuplicateExtrasAreExact() {
		String[] codes = { "CRYSTAL_SWORD", "CRYSTAL_BOW", "CRYSTAL_STAFF", "CRYSTAL_WAND", "CRYSTAL_HEAD",
				"CRYSTAL_BODY", "CRYSTAL_HANDS", "CRYSTAL_LEGS", "CRYSTAL_FEET", "CRYSTAL_ACCESSORY" };
		for (int roll = 0; roll < 100; roll++) {
			var loot = rules.roll(sequence(roll, 0, 0));
			assertEquals(codes[roll / 10], loot.equipment());
			assertEquals(30, loot.gold());
		}
		var potions = rules.roll(sequence(0, 50, 40, 59));
		assertEquals(List.of(new DungeonChestRulesExtra("HEALTH_POTION", 4, true)), potions.extras());
		var books = rules.roll(sequence(0, 99, 80, 99));
		assertEquals(List.of(new DungeonChestRulesExtra("BOOK_FLAME_BURST", 1, false),
				new DungeonChestRulesExtra("BOOK_LIGHT_SCREEN", 1, false)), books.extras());
		assertEquals(60, rules.roll(sequence(0, 50, 0, 39)).gold());
	}

	private RandomGenerator sequence(int... values) {
		return new RandomGenerator() {
			private int index;

			@Override
			public long nextLong() {
				throw new AssertionError("不应使用未指定的随机值");
			}

			@Override
			public int nextInt(int bound) {
				return values[index++];
			}
		};
	}

}
