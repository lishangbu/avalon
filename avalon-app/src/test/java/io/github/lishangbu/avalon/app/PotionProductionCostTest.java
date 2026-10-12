package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.item.ItemCatalog;
import io.github.lishangbu.avalon.content.item.PotionRecoveryRules;
import io.github.lishangbu.avalon.content.life.FarmingCatalog;
import io.github.lishangbu.avalon.content.life.FishingCatalog;
import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.life.model.LifeProductionRecipe;
import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.world.domain.GatheringRules;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;

/** 分开累计主动采集、钓鱼尝试、农田占用和金币，避免将离线并行生长串行计时。 */
class PotionProductionCostTest {

	private final WorldCatalog world = new WorldCatalog();

	private final ItemCatalog items = new ItemCatalog(world);

	private final PotionRecoveryRules potions = new PotionRecoveryRules(items);

	private final LifeProductionCatalog production = new LifeProductionCatalog(items, world);

	private final GatheringCatalog gathering = new GatheringCatalog(items, world);

	private final FarmingCatalog farming = new FarmingCatalog(items, world);

	private final FishingCatalog fishing = new FishingCatalog(items, world);

	@Test
	void expandAllTenPotionsIntoGatheringFishingAndFarmingCosts() throws Exception {
		var rows = new ArrayList<Cost>();
		for (String potion : potions.usablePotions()) {
			for (int bottles : new int[] { 1, 99 }) {
				var raw = new TreeMap<String, Integer>();
				var crafts = new TreeMap<String, Integer>();
				expand(potion, bottles, raw, crafts);
				int gatherRounds = 0, health = 0, mana = 0, fish = 0, plots = 0, seedGold = 0;
				long plotSeconds = 0;
				var cropPlots = new TreeMap<String, Integer>();
				for (var entry : raw.entrySet()) {
					var crop = farming.definition()
						.crops()
						.stream()
						.filter(c -> c.product().equals(entry.getKey()))
						.findFirst();
					if (crop.isPresent()) {
						var c = crop.get();
						int count = (entry.getValue() + c.quantity() - 1) / c.quantity();
						plots += count;
						plotSeconds += (long) count * c.growthSeconds();
						seedGold += count * price(c.seed());
						cropPlots.put(c.id(), count);
					}
					else if (fishing.fish(entry.getKey()) != null) {
						fish += entry.getValue();
					}
					else {
						var node = gathering.nodes()
							.stream()
							.filter(n -> n.product().equals(entry.getKey()))
							.max(Comparator.comparingInt(GatheringNode::quantity))
							.orElseThrow();
						int count = (entry.getValue() + node.quantity() - 1) / node.quantity();
						var resources = GatheringRules.cost((node.level() - 1) / 2);
						gatherRounds += count;
						health += count * resources.health();
						mana += count * resources.mana();
					}
				}
				// 当前药剂每种仅需要一种作物，可给出6格／24格无空闲种植的精确生长轮数。
				assertEquals(1, cropPlots.size(), "混合作物须重新进行农田排程，不能沿用单作物计算");
				var crop = farming.crop(cropPlots.firstKey());
				for (double success : new double[] { 1, .75, .5 }) {
					rows.add(new Cost(potion, bottles, success, raw, crafts, gatherRounds, health, mana,
							gatherRounds * GatheringRules.roundMillis(50) / 1000.0, fish, fish / success,
							fish * 10 + (fish / success - fish) * 20, fish / success * 20,
							fish / success * price(fishing.definition().bait()), plots, seedGold, plotSeconds,
							((plots + 5) / 6) * crop.growthSeconds(), ((plots + 23) / 24) * crop.growthSeconds()));
				}
				assertEquals(health * 2, mana * 5);
			}
		}
		assertEquals(60, rows.size());
		var goldMana = rows.stream()
			.filter(r -> r.potion().equals("LIFE_MANA_POTION_4") && r.bottles() == 99
					&& r.assumedFishingSuccess() == .75)
			.findFirst()
			.orElseThrow();
		assertEquals(Map.of("LIFE_HERB_4", 297, "FISH_4_2", 99, "CROP_4_1", 198), goldMana.rawMaterials());
		assertEquals(149, goldMana.gatheringRounds(), "批量共享采药余料，不按每瓶2轮放大成198轮");
		assertEquals(50, goldMana.plantedPlots(), "每格4份作物，保留末格2份余料");
		assertEquals(10800, goldMana.growthSecondsWithTwentyFourPlots());
		assertEquals(132, goldMana.expectedFishingAttempts());
		assertEquals(264, goldMana.baitGold());
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target/potion-production-cost.json"),
				new ObjectMapper().writerWithDefaultPrettyPrinter()
					.writeValueAsString(Map.of("scope",
							"专业资格及初始工具已满足；选同档最高产量药草节点、同产物按配方编号排序的首个鱼油来源。"
									+ "成功钓鱼10～20秒，失败按超时20秒，成功率100%／75%／50%是敏感性假设而非真人测量。"
									+ "按批次向上取整保留余料，作物满格并行、成熟立即收获重种。未含工具报废补造、补药递归、路程、操作间隔或扩地费用。",
							"rows", rows)));
	}

	private void expand(String product, int amount, Map<String, Integer> raw, Map<String, Integer> crafts) {
		var recipe = production.recipes()
			.stream()
			.filter(r -> r.product().equals(product))
			.sorted(Comparator.comparing(LifeProductionRecipe::id))
			.findFirst();
		if (recipe.isEmpty()) {
			raw.merge(product, amount, Math::addExact);
			return;
		}
		var r = recipe.get();
		int count = (amount + r.quantity() - 1) / r.quantity();
		crafts.merge(r.id(), count, Math::addExact);
		r.ingredients().forEach((code, quantity) -> expand(code, count * quantity, raw, crafts));
	}

	private int price(String code) {
		return production.offers().stream().filter(o -> o.item().equals(code)).findFirst().orElseThrow().price();
	}

	private record Cost(String potion, int bottles, double assumedFishingSuccess, Map<String, Integer> rawMaterials,
			Map<String, Integer> craftingOperations, int gatheringRounds, int gatheringHealth, int gatheringMana,
			double gatheringSecondsAtPet50, int fishNeeded, double expectedFishingAttempts,
			double fishingSecondsMinimum, double fishingSecondsMaximum, double baitGold, int plantedPlots, int seedGold,
			long plotSeconds, long growthSecondsWithSixPlots, long growthSecondsWithTwentyFourPlots) {
	}

}
