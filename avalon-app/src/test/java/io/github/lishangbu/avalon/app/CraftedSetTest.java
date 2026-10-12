package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.item.EquipmentSetCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRulesPiece;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

class CraftedSetTest {

	@Test
	void singlePiecesStayWithinOrdinaryBudgetAndOnlyFullSetsExceedItsEffects() throws Exception {
		var items = new ItemCatalog(new WorldCatalog());
		var catalog = new EquipmentSetCatalog(items);
		var rules = new EquipmentLoadoutRules(catalog);
		var ordinary = catalog.sets().stream().filter(s -> s.id().equals("CRYSTAL_GUARD")).findFirst().orElseThrow();
		int referenceLevel = items.equipment(ordinary.pieces().getFirst()).level();
		var rows = new ArrayList<BudgetComparison>();
		for (int skill = 1; skill <= 10; skill++) {
			for (String slot : List.of("SWORD", "BOW", "STAFF", "WAND", "HEAD", "BODY", "HANDS", "LEGS", "FEET")) {
				var crafted = items.equipment("CRAFT_" + skill + "_" + slot);
				var reference = items.equipment("CRYSTAL_" + slot);
				assertEquals(reference.stats().keySet(), crafted.stats().keySet(), crafted.id());
				for (var stat : reference.stats().entrySet())
					assertEquals(Math.max(1, stat.getValue() * crafted.level() / reference.level()),
							crafted.stats().get(stat.getKey()), crafted.id() + " " + stat.getKey());
			}
		}
		for (int skill : List.of(6, 8, 10)) {
			int level = skill * 5;
			int healthBudget = ordinary.health() * level / referenceLevel;
			int shieldBudget = ordinary.shieldPerLevel() * level;
			for (String weapon : List.of("SWORD", "BOW", "STAFF", "WAND")) {
				var pieces = new ArrayList<EquipmentLoadoutRulesPiece>();
				for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET", weapon))
					pieces.add(new EquipmentLoadoutRulesPiece("CRAFT_" + skill + "_" + slot, true, 1, Map.of()));
				var full = rules.from(pieces, level);
				double healthPremium = (double) full.stats().get("health") / healthBudget - 1;
				double shieldPremium = (double) full.defendShield() / shieldBudget - 1;
				assertTrue(healthPremium >= .10 && healthPremium <= .15);
				assertTrue(shieldPremium >= .10 && shieldPremium <= .15);
				rows.add(new BudgetComparison(level, weapon, healthBudget, shieldBudget, full.stats().get("health"),
						full.defendShield(), healthPremium, shieldPremium));
			}
		}
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target/crafted-set-budget.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(Map.of("scope",
							"普通套装预算：以现有15级巡林者为参考，生命按穿戴等级同比例增长，护盾保持每级2点。"
									+ "这是同级设计预算，不是新增副本模板或实际掉落；10%～15%仅指套装效果，不指整体战力。90件单件属性另校验与普通装备预算一致。",
							"rows", rows)));
	}

	private record BudgetComparison(int level, String weapon, int ordinaryHealthBudget, int ordinaryShieldBudget,
			int craftedHealth, int craftedShield, double healthPremium, double shieldPremium) {
	}

	@Test
	void completeArmorAndMatchingWeaponUnlockBonusForEveryProfessionAndRemovingEitherDisablesIt() {
		var world = new WorldCatalog();
		var items = new ItemCatalog(world);
		var rules = new EquipmentLoadoutRules(new EquipmentSetCatalog(items));
		for (int level : List.of(6, 8, 10)) {
			for (String weapon : List.of("SWORD", "BOW", "STAFF", "WAND")) {
				var pieces = new ArrayList<EquipmentLoadoutRulesPiece>();
				for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET"))
					pieces.add(new EquipmentLoadoutRulesPiece("CRAFT_" + level + "_" + slot, true, 1, Map.of()));
				var base = rules.from(pieces, level * 5);
				assertEquals(level * 10, base.stats().get("health"));
				assertFalse(base.sets().getFirst().weaponActive());
				pieces.add(new EquipmentLoadoutRulesPiece("CRAFT_" + level + "_" + weapon, true, 1, Map.of()));
				var full = rules.from(pieces, level * 5);
				assertTrue(full.sets().getFirst().weaponActive());
				double healthRatio = (double) full.stats().get("health") / base.stats().get("health");
				double shieldRatio = (double) full.defendShield() / base.defendShield();
				assertTrue(healthRatio >= 1.10 && healthRatio <= 1.15);
				assertTrue(shieldRatio >= 1.10 && shieldRatio <= 1.15);
				pieces.removeFirst();
				assertFalse(rules.from(pieces, level * 5).sets().getFirst().weaponActive());
				pieces.removeLast();
				pieces.add(new EquipmentLoadoutRulesPiece("CRYSTAL_" + weapon, true, 1, Map.of()));
				assertFalse(rules.from(pieces, level * 5).sets().getFirst().weaponActive());
			}
		}
	}

}
