package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRules;
import io.github.lishangbu.avalon.inventory.domain.EquipmentLoadoutRulesPiece;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 使用真实单件属性隔离套装贡献，避免把套装数值的百分比误称整个人物的战斗收益。 */
class CraftedSetContributionTest extends SpringIntegrationTest {

	@Autowired
	ItemCatalog items;

	@Autowired
	EquipmentLoadoutRules loadouts;

	@Autowired
	BattleRules battles;

	@Autowired
	ObjectMapper json;

	@Test
	void isolateSetHealthFromBaseEquipmentAndCompareOnlyTheExistingSameLevelDungeon() throws Exception {
		var rows = new ArrayList<Contribution>();
		for (var profession : Map.of("WARRIOR", "SWORD", "RANGER", "BOW", "MAGE", "STAFF", "PRIEST", "WAND")
			.entrySet()) {
			for (int skill : List.of(3, 6, 8, 10)) {
				int level = skill * 5;
				var pieces = new ArrayList<EquipmentLoadoutRulesPiece>();
				var raw = new TreeMap<String, Integer>();
				for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET", profession.getValue())) {
					var gear = items.equipment("CRAFT_" + skill + "_" + slot);
					pieces.add(new EquipmentLoadoutRulesPiece(gear.id(), true, 1, gear.stats()));
					gear.stats().forEach((stat, value) -> raw.merge(stat, value, Integer::sum));
				}
				var full = loadouts.from(pieces, level);
				var withoutSets = battles.stats(profession.getKey(), level, true, raw);
				var withSets = battles.stats(profession.getKey(), level, true, full.stats());
				// 相同武器属性，单独解除配套资格；这个反事实仅隔离套装，不冒充可获得的副本装备。
				var weapon = pieces.removeLast();
				pieces.add(new EquipmentLoadoutRulesPiece("ISOLATED_WEAPON", true, 1, weapon.stats()));
				var armorOnly = loadouts.from(pieces, level);
				int armorHealth = battles.stats(profession.getKey(), level, true, armorOnly.stats()).health();
				assertEquals(withoutSets.physicalAttack(), withSets.physicalAttack());
				assertEquals(withoutSets.magicAttack(), withSets.magicAttack());
				assertEquals(withoutSets.physicalDefense(), withSets.physicalDefense());
				assertEquals(withoutSets.magicDefense(), withSets.magicDefense());
				Integer actualDungeonHealth = null;
				if (skill == 3) {
					var dungeon = new ArrayList<EquipmentLoadoutRulesPiece>();
					for (String slot : List.of("HEAD", "BODY", "HANDS", "LEGS", "FEET", profession.getValue())) {
						var gear = items.equipment("CRYSTAL_" + slot);
						assertEquals(gear.stats(), items.equipment("CRAFT_3_" + slot).stats());
						dungeon.add(new EquipmentLoadoutRulesPiece(gear.id(), true, 1, gear.stats()));
					}
					actualDungeonHealth = battles
						.stats(profession.getKey(), level, true, loadouts.from(dungeon, level).stats())
						.health();
					assertEquals(withSets.health() + 30, actualDungeonHealth);
				}
				rows.add(new Contribution(profession.getKey(), level, withoutSets.health(), armorHealth,
						withSets.health(), (double) withSets.health() / armorHealth - 1, full.defendShield(),
						actualDungeonHealth));
			}
		}
		Files.writeString(Path.of("target", "crafted-set-contribution.json"), json.writerWithDefaultPrettyPrinter()
			.writeValueAsString(new Report(
					"真实单件属性与正式职业成长；不含饰品、强化、伙伴生命。武器资格反事实只隔离效果。只有15级存在同级普通副本，其他等级副本生命为null，不外推或生成假想副本。生命百分比不是整体战斗收益。",
					rows)));
	}

	record Contribution(String profession, int level, int healthWithoutSet, int healthWithArmorSet,
			int healthWithFullSet, double weaponBonusFractionOfHeroHealth, int defendShield,
			Integer existingDungeonHealth) {
	}

	record Report(String scope, List<Contribution> rows) {
	}

}
