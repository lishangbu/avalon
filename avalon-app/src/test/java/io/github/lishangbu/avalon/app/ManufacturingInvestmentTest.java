package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.BattleEngine;
import io.github.lishangbu.avalon.battle.domain.BattleRules;
import io.github.lishangbu.avalon.battle.domain.DungeonBattlePolicy;
import io.github.lishangbu.avalon.content.battle.BattleCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonCatalog;
import io.github.lishangbu.avalon.content.battle.DungeonChestCatalog;
import io.github.lishangbu.avalon.content.battle.model.DungeonChestEquipment;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 读取真实配方依赖并展开个人有效采集时间；稀有与普通同轮产出不能重复相加计时。 */
@Tag("rules")
class ManufacturingInvestmentTest {

	private final RuleTestFixture fixture = new RuleTestFixture();

	private final LifeProductionCatalog production = fixture.production;

	private final GatheringCatalog gathering = fixture.gathering;

	private final ItemCatalog items = fixture.items;

	private final ObjectMapper json = new ObjectMapper();

	private final DungeonChestCatalog chests = fixture.chests;

	private final BattleRules battleRules = fixture.rules;

	private final BattleEngine engine = fixture.engine;

	private final BattleCatalog battles = fixture.battles;

	private final CreatureCatalog creatures = fixture.creatures;

	private final DungeonCatalog dungeon = fixture.dungeon;

	private final DungeonBattlePolicy dungeonPolicy = fixture.dungeonPolicy;

	private final WorldCatalog world = fixture.world;

	private final QuestCatalog quests = fixture.quests;

	private static final Map<String, String> TOOLS = Map.of("MINING", "PICKAXE", "LOGGING", "AXE", "HERBALISM",
			"PINCERS", "HUNTING", "TRAP", "ARTISAN", "HAMMER", "LEATHERWORK", "NEEDLE", "ALCHEMY", "ROD", "COOKING",
			"POT");

	@Test
	void reportEveryEquipmentRecipeIncludingRefinementAndExpectedToolReplacement() throws Exception {
		var rows = new ArrayList<Investment>();
		for (int petLevel : List.of(15, 50)) {
			var toolCosts = new TreeMap<String, Cost>();
			for (var tool : production.tools())
				toolCosts.put(tool.item(), new Cost(0, 0, 0));
			boolean converged = false;
			for (int iteration = 0; iteration < 100; iteration++) {
				var next = new TreeMap<String, Cost>();
				double difference = 0;
				for (var tool : production.tools()) {
					var investment = investment(tool.item(), petLevel, toolCosts);
					var cost = new Cost(investment.seconds(), investment.totalGatheringHealth(),
							investment.totalGatheringMana());
					next.put(tool.item(), cost);
					var previous = toolCosts.get(tool.item());
					difference = Math.max(difference, Math.max(Math.abs(cost.seconds() - previous.seconds()), Math
						.max(Math.abs(cost.health() - previous.health()), Math.abs(cost.mana() - previous.mana()))));
				}
				toolCosts = next;
				if (difference < .000001) {
					converged = true;
					break;
				}
			}
			assertTrue(converged, "工具报废补造成本应收敛");
			for (var recipe : production.recipes()) {
				if (!items.hasEquipment(recipe.product()))
					continue;
				var cost = investment(recipe.product(), petLevel, toolCosts);
				assertTrue(Double.isFinite(cost.seconds()) && cost.seconds() > 0);
				rows.add(cost);
			}
		}
		assertEquals(186, rows.size(), "90种基础装备及3种副本图纸装备，各计算两档伙伴等级");
		for (var row : rows) {
			assertEquals(row.directGatheringHealth() * .4, row.directGatheringMana(), 1e-6);
			assertTrue(row.totalGatheringHealth() > row.directGatheringHealth());
			assertTrue(row.totalGatheringMana() > row.directGatheringMana());
			assertEquals(row.totalGatheringHealth() * .4, row.totalGatheringMana(), 1e-6);
			if (row.product().startsWith("CRAFT_3_")) {
				assertEquals(1440, row.directGatheringHealth(), 1e-6);
				assertEquals(576, row.directGatheringMana(), 1e-6);
			}
		}
		var comparisons = new ArrayList<Comparison>();
		var benchmark = new DungeonCombatSamples(battleRules, engine, battles, creatures, dungeon, dungeonPolicy);
		int runCommands = benchmark.personalRunCommands();
		assertEquals(14, runCommands, "四场准备发起、三次休整、入口休整往返及发起者开箱");
		var outward = route("CITY_SHOP", dungeon.definition().entrance());
		var returning = route(dungeon.definition().entrance(), "CITY_SHOP");
		int travelCommands = outward.size() + returning.size() - 2;
		var baseline = new TreeMap<String, Double>();
		for (String profession : List.of("WARRIOR", "RANGER", "MAGE", "PRIEST"))
			baseline.put(profession, benchmark.meanRounds(profession, 15, 128));
		int totalWeight = chests.definition().equipment().stream().mapToInt(DungeonChestEquipment::weight).sum();
		for (var row : rows) {
			if (!row.product().startsWith("CRAFT_3_"))
				continue;
			String equivalent = row.product().replace("CRAFT_3_", "CRYSTAL_");
			var drop = chests.definition()
				.equipment()
				.stream()
				.filter(gear -> gear.code().equals(equivalent))
				.findFirst()
				.orElseThrow();
			assertEquals(items.equipment(equivalent).stats(), items.equipment(row.product()).stats());
			for (var sample : baseline.entrySet()) {
				// 标准协议：每个队伍回合5秒，四场各2秒发起、三次休整各2秒；个人时间不乘队伍人数。
				double anySeconds = sample.getValue() * 5 + dungeon.definition().stages().size() * 2
						+ (dungeon.definition().stages().size() - 1) * 2;
				double specifiedSeconds = anySeconds * totalWeight / drop.weight();
				double withRunCommands = (sample.getValue() * 5 + runCommands * 2) * totalWeight / drop.weight();
				double probability = (double) drop.weight() / totalWeight;
				double expectedStartDays = 1 / (1 - Math.pow(1 - probability, dungeon.definition().dailyStarts()));
				// 发起者每次创建、确认、准备及出发四条命令；每天连续发起至成功或用完次数后回城。
				double hubSeconds = withRunCommands + 8 / probability + travelCommands * 2 * expectedStartDays;
				comparisons
					.add(new Comparison(row.product(), row.petLevel(), sample.getKey(), sample.getValue(), anySeconds,
							specifiedSeconds, (row.seconds() + 2) / anySeconds, (row.seconds() + 2) / specifiedSeconds,
							runCommands, withRunCommands, expectedStartDays, hubSeconds));
			}
		}
		Files.writeString(Path.of("target", "manufacturing-investment.json"), json.writerWithDefaultPrettyPrinter()
			.writeValueAsString(new Report(
					"专业均已10级；选择同档最省时资源点，使用配方最低允许档位工具；材料跨专业个人采集工时相加。包含精制旧装备、辅料、普通与稀有共同产出、随机报废工具递归补造；不计专业升级、首次置办工具、赶路、补药、交易及稀有波动。所有工具按制造补充，不把金币换算为时间。",
					rows, comparisons, outward, returning)));
		// 本报告只验证原料模型；完整投入目标由含物流、恢复和等待的协议报告评估。
		for (var comparison : comparisons) {
			assertTrue(comparison.specifiedSecondsFromTownAndBack() > comparison.specifiedSecondsWithRunCommands());
			assertTrue(comparison.specifiedSecondsWithRunCommands() > comparison.dungeonSpecifiedEquipmentSeconds());
			assertTrue(comparison.expectedStartDays() > 1);
		}
	}

	@Test
	void rareAndOrdinaryOutputsShareTheSameGatheringRounds() {
		assertEquals(20, expectedRounds(0, 1, .05), 1e-9);
		assertEquals(20.05, expectedRounds(2, 1, .05), 1e-9);
		assertEquals(40, expectedRounds(2, 2, .05), 1e-9);
		assertEquals(10, expectedRounds(10, 0, 0), 1e-9);
	}

	private Investment investment(String product, int petLevel, Map<String, Cost> toolCosts) {
		var raw = new TreeMap<String, Integer>();
		var breaks = new TreeMap<String, Double>();
		expand(product, 1, raw, breaks, new HashSet<>());
		var remaining = new TreeMap<>(raw);
		double rounds = 0, health = 0, mana = 0;
		for (String code : raw.keySet()) {
			if (!remaining.containsKey(code))
				continue;
			var candidates = gathering.nodes()
				.stream()
				.filter(node -> node.product().equals(code) || code.equals(node.rare()))
				.toList();
			assertFalse(candidates.isEmpty(), "配方原料没有采集来源：" + code);
			GatheringNode chosen = null;
			double best = Double.POSITIVE_INFINITY, chosenRounds = 0;
			for (var node : candidates) {
				int ordinary = remaining.getOrDefault(node.product(), 0);
				int rare = node.rare() == null ? 0 : remaining.getOrDefault(node.rare(), 0);
				double probability = rare == 0 ? 0 : GatheringRareRoll.chance(10, node.level()) / 100.0;
				double count = expectedRounds((ordinary + node.quantity() - 1) / node.quantity(), rare, probability);
				var tool = tool(node.skill(), (node.level() - 1) / 2);
				double cost = count * (GatheringRules.roundMillisForSkill(10) / 1000.0
						+ toolCosts.get(tool.item()).seconds() / tool.breakDenominator());
				if (cost < best) {
					best = cost;
					chosen = node;
					chosenRounds = count;
				}
			}
			assertNotNull(chosen);
			rounds += chosenRounds;
			var resources = GatheringRules.cost((chosen.level() - 1) / 2);
			health += chosenRounds * resources.health();
			mana += chosenRounds * resources.mana();
			var tool = tool(chosen.skill(), (chosen.level() - 1) / 2);
			breaks.merge(tool.item(), chosenRounds / tool.breakDenominator(), Double::sum);
			remaining.remove(chosen.product());
			if (chosen.rare() != null)
				remaining.remove(chosen.rare());
		}
		double rawSeconds = rounds * GatheringRules.roundMillisForSkill(10) / 1000.0;
		double replacement = breaks.entrySet()
			.stream()
			.mapToDouble(entry -> entry.getValue() * toolCosts.get(entry.getKey()).seconds())
			.sum();
		double replacementHealth = breaks.entrySet()
			.stream()
			.mapToDouble(entry -> entry.getValue() * toolCosts.get(entry.getKey()).health())
			.sum();
		double replacementMana = breaks.entrySet()
			.stream()
			.mapToDouble(entry -> entry.getValue() * toolCosts.get(entry.getKey()).mana())
			.sum();
		return new Investment(product, petLevel, raw, rounds, health, mana, breaks, rawSeconds, replacement,
				rawSeconds + replacement, health + replacementHealth, mana + replacementMana);
	}

	private void expand(String product, int amount, Map<String, Integer> raw, Map<String, Double> breaks,
			HashSet<String> visiting) {
		if (gathering.nodes()
			.stream()
			.anyMatch(node -> node.product().equals(product) || product.equals(node.rare()))) {
			raw.merge(product, amount, Math::addExact);
			return;
		}
		assertTrue(visiting.add(product), "配方原料循环：" + product);
		var recipe = production.recipe(product);
		assertNotNull(recipe, "需要明确基础制造配方：" + product);
		int crafts = (amount + recipe.quantity() - 1) / recipe.quantity();
		var tool = tool(recipe.skill(), production.minimumToolTier(recipe));
		breaks.merge(tool.item(), (double) crafts / tool.breakDenominator(), Double::sum);
		recipe.ingredients()
			.forEach((code, count) -> expand(code, Math.multiplyExact(count, crafts), raw, breaks, visiting));
		visiting.remove(product);
	}

	private LifeProductionTool tool(String skill, int tier) {
		return production.tools()
			.stream()
			.filter(tool -> tool.kind().equals(TOOLS.get(skill)) && tool.tier() == tier)
			.findFirst()
			.orElseThrow();
	}

	private List<String> route(String start, String target) {
		var facts = new HashSet<String>();
		quests.definition().steps().forEach(step -> {
			facts.add("QUEST:" + step.id());
			facts.addAll(step.objectives().stream().map(goal -> goal.code()).toList());
		});
		var pending = new ArrayDeque<List<String>>();
		var seen = new HashSet<String>();
		pending.add(List.of(start));
		while (!pending.isEmpty()) {
			var path = pending.remove();
			var place = world.place(path.getLast());
			if (!seen.add(place.id()) || !world.accessible(place, facts::contains))
				continue;
			if (place.id().equals(target))
				return path;
			for (String exit : place.exits()) {
				var next = new ArrayList<>(path);
				next.add(exit);
				pending.add(next);
			}
		}
		throw new AssertionError("缺少副本往返道路：" + start + " -> " + target);
	}

	/** E[max(n,T_r)]：先做n轮，再按其中已得到的稀有数量补足；T_r为负二项等待时间。 */
	private static double expectedRounds(int ordinaryRounds, int rares, double probability) {
		if (rares == 0)
			return ordinaryRounds;
		double chance = Math.pow(1 - probability, ordinaryRounds), extra = 0;
		for (int obtained = 0; obtained < rares && obtained <= ordinaryRounds; obtained++) {
			extra += chance * (rares - obtained) / probability;
			chance *= (ordinaryRounds - obtained) / (double) (obtained + 1) * probability / (1 - probability);
		}
		return ordinaryRounds + extra;
	}

	record Investment(String product, int petLevel, Map<String, Integer> rawMaterials, double gatheringRounds,
			double directGatheringHealth, double directGatheringMana, Map<String, Double> expectedBrokenTools,
			double rawSeconds, double replacementSeconds, double seconds, double totalGatheringHealth,
			double totalGatheringMana) {
	}

	record Cost(double seconds, double health, double mana) {
	}

	record Comparison(String product, int petLevel, String profession, double dungeonMeanRounds,
			double dungeonAnyEquipmentSeconds, double dungeonSpecifiedEquipmentSeconds, double ratioToAny,
			double ratioToSpecified, int personalRunCommands, double specifiedSecondsWithRunCommands,
			double expectedStartDays, double specifiedSecondsFromTownAndBack) {
	}

	record Report(String scope, List<Investment> rows, List<Comparison> comparisons, List<String> dungeonOutwardRoute,
			List<String> dungeonReturnRoute) {
	}

}
