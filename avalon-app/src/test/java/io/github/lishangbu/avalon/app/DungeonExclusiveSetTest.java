package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRulesPiece;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DungeonExclusiveSetTest {

	@Test
	void exclusiveArmorActivatesShieldAtFourPiecesAndCompleteSetAddsMatchingWeaponBonus() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var rules = new EquipmentLoadoutRules(new EquipmentSetCatalog(items));
		for (String prefix : List.of("F_BASTION", "E_SENTINEL")) {
			int level = prefix.equals("F_BASTION") ? 40 : 45;
			var pieces = new ArrayList<EquipmentLoadoutRulesPiece>();
			for (String slot : List.of("HEAD", "BODY", "HANDS"))
				pieces.add(piece(prefix + "_" + slot));
			assertEquals(0, rules.from(pieces, level).defendShield());
			pieces.add(piece(prefix + "_LEGS"));
			assertEquals(level * 3, rules.from(pieces, level).defendShield());
			assertEquals(level * 3, rules.from(pieces, 50).defendShield());
			pieces.add(piece(prefix + "_FEET"));
			for (String weapon : List.of("SWORD", "BOW", "STAFF", "WAND")) {
				pieces.add(piece(prefix + "_" + weapon));
				assertEquals(level * 3 + level / 5 + 1, rules.from(pieces, level).defendShield());
				assertTrue(rules.from(pieces, level).sets().getFirst().weaponActive());
				pieces.removeLast();
			}
			pieces.add(piece("CRAFT_" + level / 5 + "_SWORD"));
			assertFalse(rules.from(pieces, level).sets().getFirst().weaponActive());
			for (var piece : pieces)
				assertEquals(level, items.equipment(piece.code()).level());
		}
		// 普通完整制造装备与图纸装备继续存在。
		var production = new LifeProductionCatalog(items, world);
		assertTrue(production.recipes()
			.stream()
			.noneMatch(
					recipe -> recipe.product().startsWith("F_BASTION_") || recipe.product().startsWith("E_SENTINEL_")));
		assertEquals("CRAFT_9_BODY", production.recipe("CRAFT_9_BODY").product());
		assertEquals("E_FORGED_BODY", production.recipe("E_FORGED_BODY").product());
	}

	private EquipmentLoadoutRulesPiece piece(String code) {
		return new EquipmentLoadoutRulesPiece(code, true, 1, Map.of());
	}

	@Test
	void eachDungeonRetainsCompleteOrdinaryGearAndAddsOnlyItsOwnNineExclusivePieces() {
		var fixture = new RuleTestFixture();
		var items = fixture.items;
		var chests = fixture.chests;
		for (String dungeon : List.of("F_ARMORY", "E_SEALED_TRIAL")) {
			String prefix = dungeon.equals("F_ARMORY") ? "F_BASTION_" : "E_SENTINEL_";
			int level = dungeon.equals("F_ARMORY") ? 40 : 45;
			var loot = chests.definition(dungeon).equipment();
			assertEquals(18, loot.size());
			assertEquals(45,
					loot.stream()
						.filter(entry -> entry.code().startsWith(prefix))
						.mapToInt(entry -> entry.weight())
						.sum());
			assertEquals(9, loot.stream().filter(entry -> entry.code().startsWith(prefix)).count());
			assertTrue(loot.stream().allMatch(entry -> items.equipment(entry.code()).level() == level));
			assertTrue(loot.stream()
				.allMatch(entry -> entry.code().startsWith(prefix) || entry.code().startsWith("CRAFT_")
						|| entry.code().equals("F_ARMORY_BODY") || entry.code().equals("E_FORGED_BODY")));
		}
	}

}
