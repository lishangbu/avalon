package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.SpeciesStatsCatalog;
import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.life.model.LifeProductionRecipe;
import io.github.lishangbu.avalon.content.quest.QuestCatalog;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.world.domain.GatheringRareRoll;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 长期批量生产的线性资源账：工具与补给均由同一生产网络补充，不把循环成本当作免费。 */
class LifeEconomyClosureTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final PotionRecoveryRules potions = new PotionRecoveryRules(items);

	private final LifeProductionCatalog production = new LifeProductionCatalog(items, world);

	private final GatheringCatalog gathering = new GatheringCatalog(items, world);

	private final FarmingCatalog farming = new FarmingCatalog(items, world);

	private final FishingCatalog fishing = new FishingCatalog(items, world);

	private static final Map<String, String> TOOL_KINDS = Map.of("MINING", "PICKAXE", "LOGGING", "AXE", "HERBALISM",
			"PINCERS", "HUNTING", "TRAP", "ARTISAN", "HAMMER", "LEATHERWORK", "NEEDLE", "ALCHEMY", "ROD", "COOKING",
			"POT");

	@Test
	void toolsPotionsFishingAndFarmingFormAFiniteProductionNetwork() throws Exception {
		var reports = new ArrayList<Scenario>();
		var progression = new ArrayList<ManufacturingStage>();
		var efficient = new ArrayList<ManufacturingStage>();
		var balanced = new ArrayList<ManufacturingStage>();
		for (String strategy : List.of("SAME_TIER", "BASIC", "RECOVERY_50", "RECOVERY_15")) {
			for (double success : List.of(1.0, .75, .5)) {
				var network = network(success, strategy);
				Map<String, Cost> costs = new TreeMap<>();
				network.keySet().forEach(code -> costs.put(code, Cost.ZERO));
				int iterations = 0;
				for (; iterations < 1000; iterations++) {
					var next = new TreeMap<String, Cost>();
					double delta = 0;
					for (var entry : network.entrySet()) {
						Cost value = entry.getValue().direct();
						for (var dependency : entry.getValue().inputs().entrySet()) {
							assertTrue(costs.containsKey(dependency.getKey()), "缺失生产来源：" + dependency.getKey());
							value = value.plus(costs.get(dependency.getKey()).times(dependency.getValue()));
						}
						assertTrue(Double.isFinite(value.plotSeconds()) && Double.isFinite(value.activeSeconds()));
						delta = Math.max(delta, value.distance(costs.get(entry.getKey())));
						next.put(entry.getKey(), value);
					}
					costs.clear();
					costs.putAll(next);
					if (delta < 1e-7)
						break;
				}
				assertTrue(iterations < 1000, "补给与工具递归成本不收敛");
				// 重算残差，避免迭代次数或仅无异常就被误认为闭环完成。
				for (var entry : network.entrySet()) {
					Cost evaluated = entry.getValue().direct();
					for (var dependency : entry.getValue().inputs().entrySet())
						evaluated = evaluated.plus(costs.get(dependency.getKey()).times(dependency.getValue()));
					assertTrue(evaluated.distance(costs.get(entry.getKey())) < 1e-6);
				}
				if (!strategy.startsWith("RECOVERY")) {
					assertTrue(costs.get("LIFE_ORE_4").gold() > 0, "采矿也须承担补药所需种子和鱼饵");
					assertTrue(costs.get("TOOL_GOLD_HAMMER").plotSeconds() > 0, "工具成本须递归包含补给种植");
				}
				reports.add(new Scenario(strategy, success, iterations + 1, costs));
				progression.addAll(manufacturingProgression(strategy, success, costs, Route.MAX_REWARD));
				efficient.addAll(manufacturingProgression(strategy, success, costs, Route.ACTIVE_TIME));
				balanced.addAll(manufacturingProgression(strategy, success, costs, Route.FARM_AND_ACTIVE));
			}
		}
		assertEquals(12, reports.size());
		for (int offset : List.of(0, 3, 6, 9)) {
			for (String code : reports.getFirst().costs().keySet()) {
				assertTrue(reports.get(offset + 1).costs().get(code).activeSeconds() >= reports.get(offset)
					.costs()
					.get(code)
					.activeSeconds());
				assertTrue(reports.get(offset + 2).costs().get(code).gold() >= reports.get(offset + 1)
					.costs()
					.get(code)
					.gold());
			}
		}
		assertEquals(reports.get(3).costs().get("CRAFT_3_SWORD"), reports.get(5).costs().get("CRAFT_3_SWORD"),
				"基础药与铜装备不依赖钓鱼");
		Files.createDirectories(Path.of("target"));
		assertEquals(432, progression.size());
		assertEquals(432, efficient.size());
		assertEquals(432, balanced.size());
		Files.writeString(Path.of("target/manufacturing-balanced-progression.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(Map.of("scope", "逐级按 max(主动工时, 农田格秒/24) 除以实际熟练度排序，含低一级半收益。"
							+ "这是两类资源瓶颈的排序指标，不是实际排程或墙钟时间；金币独立展示、不参与排序。" + "原料来自满级合作网络，不抵扣产物出售复用；不含首次工具、交易、原料转场和背包操作。",
							"stages", balanced)));
		Files.writeString(Path.of("target/manufacturing-efficient-progression.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(Map.of("scope", "逐级选择单位实际熟练度主动工时最低的已解锁配方，含低一级半收益；不是全局最优排程。"
							+ "原料来自满级合作网络，保留升级溢出，不抵扣产物出售或复用；农田占用及金币不参与排序。" + "不含首次工具、原料转场、交易、背包整理或农田排程；不能当作完整真人升级耗时。",
							"stages", efficient)));
		Files.writeString(Path.of("target/manufacturing-progression.json"), new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(Map.of("scope",
					"四制造专业逐级选择单次熟练度最高配方，同分取编号最小者；保留溢出，按实际材料和工具预算计完整投入，不抵扣成品售价或后续复用。"
							+ "原料由满级合作生产网络提供，伙伴及钓鱼假设随情景；不是单人自给，也不是最低成本升级路线。"
							+ "长期均值含工具补造与补给，不含首次工具、交易、原料转场、背包操作。农田占用秒数独立于在线时间，不能直接相加。",
					"stages", progression)));
		Files.writeString(Path.of("target/life-economy-closure.json"), new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(Map.of("scope", "专业10级、伙伴50级、长期批量共享余料；单位均值可为小数。"
					+ "采集选择最高单次产量节点，稀有独立采集且不抵扣伴生产物；分别使用同档药、基础药或利爪猫15/50级免费恢复路线（移动每步2秒、停止/恢复/重派各2秒；假定已完成正式任务），按每瓶最多损失一轮扣费的保守有效恢复量。"
					+ "假定伙伴上限容纳整瓶药效。钓鱼每次尝试20秒，成功率为敏感性假设；每次制造2秒，播种浇水收获每格共6秒。"
					+ "工具永久报废和补给递归已计入，种子鱼饵按商店购买；农田占用与金币分列，不换算为主动时间。" + "不含首次工具、成长、赶路、交易、背包操作和农田排程，不能直接当作单件墙钟时间。",
					"scenarios", reports, "recoveryRoutes", routes)));
	}

	private List<ManufacturingStage> manufacturingProgression(String strategy, double success, Map<String, Cost> costs,
			Route route) {
		var rows = new ArrayList<ManufacturingStage>();
		for (String skill : List.of("ARTISAN", "LEATHERWORK", "ALCHEMY", "COOKING")) {
			long points = 0;
			for (int level = 1; level < 10; level++) {
				int currentLevel = level;
				boolean efficient = route != Route.MAX_REWARD;
				long threshold = LifeSkillRules.threshold(level) * 2;
				Comparator<LifeProductionRecipe> ordering = efficient
						? Comparator.comparingDouble(r -> routeTime(craftCost(r, costs), route)
								/ (LifeSkillRules.gain(threshold, r.level(), r.points()) - threshold))
						: Comparator.comparingInt(LifeProductionRecipe::points).reversed();
				var recipe = production.recipes()
					.stream()
					.filter(r -> r.skill().equals(skill)
							&& (r.level() == currentLevel || efficient && r.level() == currentLevel - 1))
					.sorted(ordering.thenComparing(LifeProductionRecipe::id))
					.findFirst()
					.orElseThrow();
				Cost perCraft = craftCost(recipe, costs);
				int crafts = 0;
				while (LifeSkillRules.level(points) == level) {
					points = LifeSkillRules.gain(points, recipe.level(), recipe.points());
					assertTrue(++crafts <= 65536);
				}
				assertEquals(level + 1, LifeSkillRules.level(points));
				rows.add(new ManufacturingStage(strategy, success, skill, level, recipe.id(), crafts,
						crafts * recipe.quantity(), perCraft.times(crafts)));
			}
		}
		return rows;
	}

	private enum Route {

		MAX_REWARD, ACTIVE_TIME, FARM_AND_ACTIVE

	}

	private double routeTime(Cost cost, Route route) {
		return route == Route.FARM_AND_ACTIVE ? Math.max(cost.activeSeconds(), cost.plotSeconds() / 24)
				: cost.activeSeconds();
	}

	private Cost craftCost(LifeProductionRecipe recipe, Map<String, Cost> costs) {
		var inputs = new TreeMap<String, Double>();
		recipe.ingredients().forEach((code, count) -> inputs.put(code, count.doubleValue()));
		wear(inputs, TOOL_KINDS.get(recipe.skill()), production.minimumToolTier(recipe), 1);
		Cost cost = new Cost(2, 0, 0);
		for (var input : inputs.entrySet())
			cost = cost.plus(costs.get(input.getKey()).times(input.getValue()));
		return cost;
	}

	private record ManufacturingStage(String supplyStrategy, double assumedFishingSuccess, String skill, int fromLevel,
			String recipe, int crafts, int produced, Cost totalCost) {
	}

	private Map<String, Equation> network(double success, String strategy) {
		var equations = new TreeMap<String, Equation>();
		for (var recipe : production.recipes()
			.stream()
			.sorted(Comparator.comparingInt(LifeProductionRecipe::level).thenComparing(LifeProductionRecipe::id))
			.toList()) {
			var inputs = new TreeMap<String, Double>();
			recipe.ingredients().forEach((code, count) -> inputs.put(code, (double) count / recipe.quantity()));
			wear(inputs, TOOL_KINDS.get(recipe.skill()), production.minimumToolTier(recipe), 1.0 / recipe.quantity());
			equations.putIfAbsent(recipe.product(), new Equation(new Cost(2.0 / recipe.quantity(), 0, 0), inputs));
		}
		for (var crop : farming.definition().crops()) {
			var inputs = new TreeMap<String, Double>();
			wear(inputs, "HOE", (crop.level() - 1) / 2, 1.0 / crop.quantity());
			equations.put(crop.product(),
					new Equation(new Cost(6.0 / crop.quantity(), (double) crop.growthSeconds() / crop.quantity(),
							(double) price(crop.seed()) / crop.quantity()), inputs));
		}
		for (var fish : fishing.definition().fish()) {
			var inputs = new TreeMap<String, Double>();
			wear(inputs, "FISHING_ROD", (fish.level() - 1) / 2, 1);
			equations.put(fish.id(),
					new Equation(new Cost(20 / success, 0, price(fishing.definition().bait()) / success), inputs));
		}
		for (var node : gathering.nodes()
			.stream()
			.sorted(Comparator.comparingInt(GatheringNode::quantity).reversed().thenComparingInt(GatheringNode::level))
			.toList()) {
			equations.putIfAbsent(node.product(), gatheringEquation(node, 1.0 / node.quantity(), strategy));
			if (node.rare() != null)
				equations.putIfAbsent(node.rare(),
						gatheringEquation(node, 100.0 / GatheringRareRoll.chance(10, node.level()), strategy));
		}
		return equations;
	}

	private Equation gatheringEquation(GatheringNode node, double rounds, String strategy) {
		int tier = (node.level() - 1) / 2;
		var resources = GatheringRules.cost(tier);
		String hp = strategy.equals("BASIC") || tier == 0 ? "HEALTH_POTION" : "LIFE_HEALTH_POTION_" + tier;
		String mp = strategy.equals("BASIC") || tier == 0 ? "MANA_POTION" : "LIFE_MANA_POTION_" + tier;
		var inputs = new TreeMap<String, Double>();
		if (strategy.startsWith("RECOVERY")) {
			int petLevel = strategy.equals("RECOVERY_15") ? 15 : 50;
			var stats = new SpeciesStatsCatalog().atLevel("CLAW_CAT", petLevel);
			int capacity = Math.min((stats.health() - 1) / resources.health(), stats.mana() / resources.mana());
			assertTrue(capacity > 0);
			var outward = route(node.location(), null);
			var returning = route(outward.getLast(), node.location());
			// 每步移动2秒；停止派遣、恢复、重新派遣各2秒。按长期满资源循环摊销。
			double recoverySeconds = (outward.size() + returning.size() - 2) * 2 + 6;
			wear(inputs, TOOL_KINDS.get(node.skill()), tier, rounds);
			return new Equation(
					new Cost(rounds * (GatheringRules.roundMillis(petLevel) / 1000.0 + recoverySeconds / capacity), 0,
							0),
					inputs);
		}
		inputs.put(hp, rounds * resources.health() / (potions.recovery(hp).health() - resources.health()));
		inputs.put(mp, rounds * resources.mana() / (potions.recovery(mp).mana() - resources.mana()));
		wear(inputs, TOOL_KINDS.get(node.skill()), tier, rounds);
		return new Equation(new Cost(rounds * GatheringRules.roundMillis(50) / 1000.0, 0, 0), inputs);
	}

	private final Map<String, List<String>> routes = new TreeMap<>();

	private List<String> route(String start, String target) {
		String key = start + ":" + target;
		if (routes.containsKey(key))
			return routes.get(key);
		var facts = new HashSet<String>();
		new QuestCatalog(world, items, new CreatureCatalog()).definition().steps().forEach(step -> {
			facts.add("QUEST:" + step.id());
			facts.addAll(step.facts());
		});
		var pending = new ArrayDeque<List<String>>();
		var seen = new HashSet<String>();
		pending.add(List.of(start));
		while (!pending.isEmpty()) {
			var path = pending.remove();
			var place = world.place(path.getLast());
			if (!world.accessible(place, facts::contains) || !seen.add(place.id()))
				continue;
			if (target == null ? world.provides(place.id(), "RECOVERY") : place.id().equals(target)) {
				routes.put(key, path);
				return path;
			}
			for (String exit : place.exits()) {
				var next = new ArrayList<>(path);
				next.add(exit);
				pending.add(next);
			}
		}
		throw new AssertionError("缺少恢复往返道路：" + key);
	}

	private void wear(Map<String, Double> inputs, String kind, int tier, double operations) {
		var tool = production.tools()
			.stream()
			.filter(t -> t.kind().equals(kind) && t.tier() == tier)
			.findFirst()
			.orElseThrow();
		inputs.merge(tool.item(), operations / tool.breakDenominator(), Double::sum);
	}

	private int price(String code) {
		return production.offers().stream().filter(o -> o.item().equals(code)).findFirst().orElseThrow().price();
	}

	private record Cost(double activeSeconds, double plotSeconds, double gold) {

		static final Cost ZERO = new Cost(0, 0, 0);

		Cost plus(Cost other) {
			return new Cost(activeSeconds + other.activeSeconds, plotSeconds + other.plotSeconds, gold + other.gold);
		}

		Cost times(double count) {
			return new Cost(activeSeconds * count, plotSeconds * count, gold * count);
		}

		double distance(Cost other) {
			return Math.max(Math.abs(activeSeconds - other.activeSeconds),
					Math.max(Math.abs(plotSeconds - other.plotSeconds), Math.abs(gold - other.gold)));
		}
	}

	private record Equation(Cost direct, Map<String, Double> inputs) {
	}

	private record Scenario(String supplyStrategy, double assumedFishingSuccess, int iterations,
			Map<String, Cost> costs) {
	}

}
