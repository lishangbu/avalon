package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.battle.application.query.BattleQueryService;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 真实库存、资源存档与事务测试，避免只验证属性公式却漏掉卸装穿回恢复资源的漏洞。 */
class EquipmentFlowTest extends SpringIntegrationTest {

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
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	BattleQueryService battles;

	@Autowired
	BattleRules rules;

	@Autowired
	EntityManager em;

	@Autowired
	PartyFactory partyFactory;

	@Autowired
	BattleEngine engine;

	@Autowired
	EquipmentSetCatalog sets;

	@Autowired
	CreatureCommandService creatures;

	@Test
	void craftedSetsReachBattleAndWeaponRemovalClampsResourcesForEveryProfession() {
		var weapons = Map.of("WARRIOR", "SWORD", "RANGER", "BOW", "MAGE", "STAFF", "PRIEST", "WAND");
		for (int skill : List.of(6, 8, 10)) {
			for (var profession : weapons.entrySet()) {
				int level = skill * 5;
				long owner = ready(profession.getKey());
				transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(level))));
				for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET"))
					inventory.equip(owner, grant(owner, "CRAFT_" + skill + "_" + slot));
				int base = battles.lobby(owner).vitals().getFirst().maxHp();
				long weapon = grant(owner, "CRAFT_" + skill + "_" + profession.getValue());
				inventory.equip(owner, weapon);
				var definition = sets.sets()
					.stream()
					.filter(set -> set.id().equals("CRAFT_SET_" + skill))
					.findFirst()
					.orElseThrow();
				assertEquals(base + definition.weaponHealth(), battles.lobby(owner).vitals().getFirst().maxHp());
				long pet = transactions
					.call(() -> creatures.capture(owner, "SET_PARTNER", "CLAW_CAT", level, "A", "完好防护", List.of("水弹")));
				creatures.position(owner, pet, 0);
				var party = partyFactory.create(owner, false);
				var hero = party.stream().filter(BattleState.Unit::isHero).findFirst().orElseThrow();
				assertEquals(battles.lobby(owner).vitals().getFirst().maxHp(), hero.getStats().health());
				assertEquals(level * 2 + definition.weaponShield(), hero.getDefendShield());
				var state = new BattleState();
				state.setEncounter("VILLAGE");
				state.getUnits().addAll(party);
				var enemy = new BattleState.Unit();
				enemy.setId("enemy");
				enemy.setName("护盾测试目标");
				enemy.setSpecies("CLAW_CAT");
				enemy.setEnemy(true);
				enemy.setLevel(level);
				enemy.setStats(rules.stats("CLAW_CAT", level, false, Map.of()));
				enemy.setHp(enemy.getStats().health());
				enemy.setSkills(List.of());
				state.getUnits().add(enemy);
				state.setEnemyOrders(List.of(new BattleState.Order("enemy", "DEFEND", "enemy")));
				engine.resolve(state,
						party.stream()
							.map(unit -> new BattleState.Order(unit.getId(), "DEFEND", unit.getId()))
							.toList(),
						false, new BattleEngine.Resources() {
							public int count(String code) {
								return 0;
							}

							public boolean room(BattleState.Unit target) {
								return false;
							}

							public void take(String code) {
								fail("防御不消耗物品");
							}

							public void capture(BattleState.Unit target) {
								fail("防御不捕获");
							}
						});
				assertTrue(
						state.getLog().stream().anyMatch(line -> line.contains("套装护盾，可吸收 " + hero.getDefendShield())));
				long character = characterQueries.requireId(owner);
				transactions.run(() -> em.createNativeQuery("UPDATE combat_vitals SET hp=:hp WHERE id=:id")
					.setParameter("hp", hero.getStats().health())
					.setParameter("id", character)
					.executeUpdate());
				inventory.unequip(owner, weapon);
				assertEquals(base, battles.lobby(owner).vitals().getFirst().hp());
				assertEquals(level * 2, partyFactory.create(owner, false).getFirst().getDefendShield());
				inventory.equip(owner, weapon);
				assertEquals(base, battles.lobby(owner).vitals().getFirst().hp(), "重新激活套装不能免费治疗");
			}
		}
	}

	private long ready(String profession) {
		String name = "gear_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Equipment-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, profession);
		return owner;
	}

	private long grant(long owner, String code) {
		transactions.run(() -> inventory.grant(owner, code, code, 1, true));
		return Long
			.parseLong(items.list(owner).stream().filter(i -> i.code().equals(code)).findFirst().orElseThrow().id());
	}

	@Test
	void crystalSetCountsOnlyWornPiecesAndDoesNotHealWhenActivated() {
		long owner = ready("WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(15))));
		int base = battles.lobby(owner).vitals().getFirst().maxHp();
		long head = grant(owner, "CRYSTAL_HEAD");
		long body = grant(owner, "CRYSTAL_BODY");
		long hands = grant(owner, "CRYSTAL_HANDS");
		long feet = grant(owner, "CRYSTAL_FEET");
		assertTrue(items.loadout(owner).sets().isEmpty());
		inventory.equip(owner, head);
		assertEquals(base + 30, battles.lobby(owner).vitals().getFirst().maxHp());
		inventory.equip(owner, body);
		assertEquals(base + 120, battles.lobby(owner).vitals().getFirst().maxHp());
		assertEquals(base, battles.lobby(owner).vitals().getFirst().hp());
		inventory.equip(owner, hands);
		assertEquals(0, items.loadout(owner).defendShield());
		inventory.equip(owner, feet);
		assertEquals(30, items.loadout(owner).defendShield());
		inventory.unequip(owner, body);
		assertEquals(0, items.loadout(owner).defendShield());
		inventory.unequip(owner, hands);
		inventory.unequip(owner, feet);
		assertEquals(base + 30, battles.lobby(owner).vitals().getFirst().maxHp());
		assertEquals(base, battles.lobby(owner).vitals().getFirst().hp());
	}

	@Test
	void sevenSlotsMatchDocumentedBudgetsForEveryProfession() {
		for (String job : List.of("WARRIOR", "MAGE", "PRIEST", "RANGER")) {
			long owner = ready(job);
			transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(15))));
			for (String code : List.of("GUARD_WEAPON", "GUARD_HEAD", "GUARD_BODY", "GUARD_HANDS", "GUARD_LEGS",
					"GUARD_FEET", "GUARD_ACCESSORY"))
				inventory.equip(owner, grant(owner, code));
			var equipped = items.list(owner).stream().filter(i -> i.equipped()).toList();
			assertEquals(7, equipped.size());
			assertEquals(7, equipped.stream().map(i -> i.slot()).distinct().count());
			var bare = rules.stats(job, 15, true, Map.of());
			var vitals = battles.lobby(owner).vitals().getFirst();
			assertEquals(bare.health() + 160, vitals.maxHp());
			assertEquals(bare.mana() + 12, vitals.maxMp());
			assertEquals(bare.health(), vitals.hp(), "提高装备上限不补充当前生命");
			assertEquals(bare.mana(), vitals.mp());
			String attack = List.of("WARRIOR", "RANGER").contains(job) ? "physicalAttack" : "magicAttack";
			assertEquals(24, equipped.stream().mapToInt(i -> i.stats().getOrDefault(attack, 0)).sum());
			assertEquals(24, equipped.stream().mapToInt(i -> i.stats().getOrDefault("physicalDefense", 0)).sum());
			assertEquals(18, equipped.stream().mapToInt(i -> i.stats().getOrDefault("magicDefense", 0)).sum());
			assertEquals(6, equipped.stream().mapToInt(i -> i.stats().getOrDefault("speed", 0)).sum());
		}
	}

	@Test
	void removingAndReequippingClampsStoredResourcesWithoutHealingOrReviving() {
		long owner = ready("MAGE");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(15))));
		long body = grant(owner, "GUARD_BODY");
		long accessory = grant(owner, "GUARD_ACCESSORY");
		inventory.equip(owner, body);
		inventory.equip(owner, accessory);
		long character = Long.parseLong(characterQueries.find(owner).id());
		transactions.run(() -> em.createNativeQuery("UPDATE combat_vitals SET hp = 9999, mp = 9999 WHERE id = :id")
			.setParameter("id", character)
			.executeUpdate());
		inventory.unequip(owner, body);
		inventory.unequip(owner, accessory);
		var bare = battles.lobby(owner).vitals().getFirst();
		assertEquals(bare.maxHp(), bare.hp());
		assertEquals(bare.maxMp(), bare.mp());
		inventory.equip(owner, body);
		inventory.equip(owner, accessory);
		var reequipped = battles.lobby(owner).vitals().getFirst();
		assertEquals(bare.hp(), reequipped.hp());
		assertEquals(bare.mp(), reequipped.mp());
		transactions.run(() -> em.createNativeQuery("UPDATE combat_vitals SET hp = 0, mp = 0 WHERE id = :id")
			.setParameter("id", character)
			.executeUpdate());
		inventory.unequip(owner, body);
		inventory.equip(owner, body);
		assertEquals(0, battles.lobby(owner).vitals().getFirst().hp());
		assertEquals(0, battles.lobby(owner).vitals().getFirst().mp());
	}

	@Test
	void ownershipBattleLevelAndRollbackProtectEquipmentAndVitals() {
		long owner = ready("WARRIOR");
		long body = grant(owner, "STARTER_BODY");
		long high = grant(owner, "GUARD_BODY");
		assertThrows(BusinessException.class, () -> inventory.equip(owner, high));
		assertThrows(BusinessException.class, () -> inventory.unequip(ready("WARRIOR"), body));
		inventory.equip(owner, body);
		var before = battles.lobby(owner).vitals();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			inventory.unequip(owner, body);
			throw new IllegalStateException("模拟换装后的外层提交失败");
		}));
		assertTrue(items.list(owner)
			.stream()
			.filter(i -> i.id().equals(Long.toString(body)))
			.findFirst()
			.orElseThrow()
			.equipped());
		assertEquals(before, battles.lobby(owner).vitals());
		transactions.run(() -> characters.setBattle(owner, 123L, false));
		assertThrows(BusinessException.class, () -> inventory.unequip(owner, body));
		transactions.run(() -> characters.setBattle(owner, null, false));
		inventory.unequip(owner, body);
		inventory.unequip(owner, body);
		assertFalse(items.list(owner).stream().anyMatch(i -> i.equipped()));
	}

}
