package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.content.battle.AcademyTrainingCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import io.github.lishangbu.avalon.world.domain.GatheringSupplyRules;
import io.github.lishangbu.avalon.world.domain.GatheringSupplyRulesSupply;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 使用正式资源规划逐轮结算，保留药效溢出与99瓶选择额度，不假定药效全部有效。 */
class GatheringEnduranceTest extends SpringIntegrationTest {

	@Autowired
	PotionRecoveryRules potions;

	@Autowired
	BattleRules battles;

	@Autowired
	AcademyTrainingCatalog academy;

	@Autowired
	ObjectMapper json;

	@Test
	void measureOneHourSupplyConsumptionAcrossPetLevelsAndResourceTiers() throws Exception {
		var rows = new ArrayList<Endurance>();
		for (String species : List.of("CLAW_CAT", "SPROUT", "THUNDER_LIZARD", "GALE_SPARROW")) {
			for (int level : List.of(6, 15, 30, 50)) {
				var stats = battles.stats(species, level, false, Map.of());
				for (int tier = 0; tier < 5; tier++) {
					var cost = GatheringRules.cost(tier);
					String hpCode = tier == 0 ? "HEALTH_POTION" : "LIFE_HEALTH_POTION_" + tier;
					String mpCode = tier == 0 ? "MANA_POTION" : "LIFE_MANA_POTION_" + tier;
					int hpRestore = potions.recovery(hpCode).health();
					int mpRestore = potions.recovery(mpCode).mana();
					int hp = stats.health(), mp = stats.mana();
					int hpBottles = 99, mpBottles = 99, hpWaste = 0, mpWaste = 0, completed = 0;
					int target = (int) (3600000 / GatheringRules.roundMillisForSkill(10));
					while (completed < target) {
						var plan = GatheringSupplyRules.plan(hp, mp, stats.health(), stats.mana(), cost.health(),
								cost.mana(), List.of(new GatheringSupplyRulesSupply(1, hpBottles, hpRestore, 0),
										new GatheringSupplyRulesSupply(2, mpBottles, 0, mpRestore)));
						if (!plan.sufficient())
							break;
						hpBottles -= plan.consumed().getOrDefault(1L, 0);
						mpBottles -= plan.consumed().getOrDefault(2L, 0);
						hpWaste += Math.max(0, hp + plan.health() - stats.health());
						mpWaste += Math.max(0, mp + plan.mana() - stats.mana());
						hp = Math.min(stats.health(), hp + plan.health()) - cost.health();
						mp = Math.min(stats.mana(), mp + plan.mana()) - cost.mana();
						assertTrue(hp >= 1 && mp >= 0);
						completed++;
					}
					assertEquals(stats.health() + (99 - hpBottles) * hpRestore - hpWaste - completed * cost.health(),
							hp);
					assertEquals(stats.mana() + (99 - mpBottles) * mpRestore - mpWaste - completed * cost.mana(), mp);
					assertTrue(completed > 0, species + " " + level + " " + tier);
					rows.add(new Endurance(species, level, tier, stats.health(), stats.mana(), target, completed,
							99 - hpBottles, 99 - mpBottles, hpWaste, mpWaste, hp, mp));
				}
			}
		}
		assertEquals(80, rows.size());
		Files.writeString(Path.of("target/gathering-endurance.json"), json.writerWithDefaultPrettyPrinter()
			.writeValueAsString(Map.of("scope", "四种正式幻兽、四个人物可携带等级、五档资源；满资源开始，"
					+ "各选择99瓶同档生命／魔法药，按正式补给规则模拟一小时有效采集。忽略工具报废、背包、离线及材料获取，" + "仅隔离资源续航与药效溢出，不代表挂机可连续一小时，也不是制造药品成本。",
					"rows", rows)));
	}

	private record Endurance(String species, int petLevel, int tier, int maxHealth, int maxMana, int targetRounds,
			int completedRounds, int healthPotions, int manaPotions, int wastedHealth, int wastedMana, int finalHealth,
			int finalMana) {
	}

}
