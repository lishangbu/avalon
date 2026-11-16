package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.life.GatheringCatalog;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.content.life.model.GatheringNode;
import io.github.lishangbu.avalon.content.life.model.LifeProductionRecipe;
import io.github.lishangbu.avalon.content.life.model.LifeProductionTool;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 银档之后的四角色自然供料助手。所有材料来自正式采集、制造、商店或角色间市场交易， 不写入金币、经验、熟练度、任务事实或角色位置；只把玩家可以执行的操作组织成可复现旅程。
 */
class NaturalEndgameCraftingJourney {

	@Autowired
	StoryOpeningJourney journey;

	@Autowired
	LifeProductionCatalog production;

	@Autowired
	GatheringCatalog gathering;

	/**
	 * 从同一组自然成长的角色续接E009，不能用新的满级供应夹具替代此前的工具和专业投入。
	 * 先补齐各采集者的剧情资格，再准备最终原料；保留接取后制造、图纸不消耗和重复交付断言。
	 */
	void complete(long leatherworker, long alchemist, long artisan, long cook) {
		var party = new Party(leatherworker, alchemist, artisan, cook);
		for (long owner : List.of(leatherworker, alchemist, artisan)) {
			cleanup(party, owner);
			if (!journey.questQueries.done(owner, "F001"))
				journey.unlockSilverGatheringRegion(owner, artisan);
			// 制皮与制药角色没有伐木名额；为火山F009准备真实交易木材，避免学习第四专业。
			ensure(party, owner, "LIFE_WOOD_0", 6, 0);
			journey.completeFire(owner, false);
			// 火山委托已消耗上一批木材；联合物资另需8份，仍由工匠正式供给。
			ensure(party, owner, "LIFE_WOOD_0", 8, 0);
			journey.completeJoint(owner);
			assertTrue(journey.questQueries.done(owner, "J025"), "晨金谷地必须由实际采集者本人解锁");
			cleanup(party, owner);
		}
		journey.completeResidual(leatherworker);
		journey.accept(leatherworker, "E009");
		var recipe = production.recipe("E_FORGED_BODY");
		assertEquals(1, quantity(leatherworker, recipe.blueprint()));
		// 高档部件、处理剂和口粮会递归推动各供应专业，不能只验收制皮9级便宣称供料闭环。
		for (var ingredient : recipe.ingredients().entrySet())
			ensure(party, leatherworker, ingredient.getKey(), ingredient.getValue(), 0);
		String tool = tool("LEATHERWORK", production.minimumToolTier(recipe));
		ensure(party, leatherworker, tool, 1, 0);
		// 工具补造可能也消耗中间材料；制造前再次按配方核对库存，缺额继续真实补造与交易。
		for (var ingredient : recipe.ingredients().entrySet())
			ensure(party, leatherworker, ingredient.getKey(), ingredient.getValue(), 0);
		long goldBefore = journey.queries.find(leatherworker).gold();
		journey.craftWithInventory(leatherworker, recipe.id(), tool, recipe.locations().getFirst());
		assertEquals(1, quantity(leatherworker, "E_FORGED_BODY"));
		assertEquals(1, quantity(leatherworker, recipe.blueprint()), "特殊图纸不应被制造消耗");
		journey.walk(leatherworker, "SALAMANDER_CITY");
		journey.deliver(leatherworker, "E009");
		assertTrue(journey.questQueries.done(leatherworker, "E009"));
		System.out.printf("NATURAL_E009_COMPLETED operations=%d goldBeforeCraft=%d goldAfterDelivery=%d%n",
				party.operations, goldBefore, journey.queries.find(leatherworker).gold());
	}

	/**
	 * 局部回归复用完整路线的同一供料入口，以真实制造和交易验证工具补造，不执行不相关的长剧情。 调用者负责明确其角色是否来自夹具；该入口不会为角色补设专业等级或地图资格。
	 */
	void supply(long leatherworker, long alchemist, long artisan, long cook, long recipient, String code,
			int required) {
		ensure(new Party(leatherworker, alchemist, artisan, cook), recipient, code, required, 0);
	}

	/**
	 * 只补足目标角色缺额。供应者由专业分工决定，跨角色交付经过正式市场扣费和幂等回执， 同一角色直接使用自己的库存，不制造自买挂单。递归深度与总操作数用来暴露意外依赖环。
	 */
	private void ensure(Party party, long recipient, String code, int required, int depth) {
		assertTrue(depth < 64 && party.operations++ < 250000, "自然供料依赖或执行预算异常：" + code);
		party.reserves.computeIfAbsent(recipient, ignored -> new HashMap<>()).merge(code, required, Math::max);
		if (quantity(recipient, code) >= required)
			return;
		var offer = production.offers().stream().filter(value -> value.item().equals(code)).findFirst();
		if (offer.isPresent()) {
			// 城市商店与村庄都出售的商品优先在城市购买，避免为补一把木工具反复返回开篇村庄。
			String shop = offer.get().locations().contains("CITY_SHOP") ? "CITY_SHOP"
					: offer.get().locations().getFirst();
			journey.walk(recipient, shop);
			journey.buyDeficit(recipient, code, required);
			return;
		}
		var recipe = production.recipe(code);
		if (recipe != null) {
			long maker = manufacturingOwner(party, recipe.skill());
			int deficit = required - quantity(recipient, code);
			int target = maker == recipient ? required : quantity(maker, code) + deficit;
			trainManufacturing(party, maker, recipe.skill(), recipe.level(), depth + 1);
			while (quantity(maker, code) < target)
				craft(party, maker, recipe, depth + 1);
			transfer(maker, recipient, code, deficit);
			return;
		}
		// 普通及稀有产物均反查正式资源点；稀有物用实际产出计数，不把普通材料数量当作完成证据。
		var nodes = gathering.nodes()
			.stream()
			.filter(node -> code.equals(node.product()) || code.equals(node.rare()))
			.sorted(Comparator.comparingInt(GatheringNode::level))
			.toList();
		assertFalse(nodes.isEmpty(), "材料没有可执行的自然来源：" + code);
		String skill = nodes.getFirst().skill();
		long gatherer = gatheringOwner(party, skill);
		trainGathering(party, gatherer, skill, nodes.getFirst().level(), depth + 1);
		var node = nodes.stream()
			.filter(value -> value.level() <= level(gatherer, skill))
			.max(Comparator.comparingInt(GatheringNode::level))
			.orElseThrow();
		int deficit = required - quantity(recipient, code);
		int target = gatherer == recipient ? required : quantity(gatherer, code) + deficit;
		gather(party, gatherer, node, code, target, depth + 1);
		transfer(gatherer, recipient, code, deficit);
	}

	/**
	 * 专业升级使用同档普通／精制基础材料配方，不以低档零收益配方硬刷，也不调整正式门槛。
	 * 每次成功制造仍由服务端结算熟练度和工具损耗；按128次记录进度，避免逐件日志淹没失败诊断。
	 */
	private void trainManufacturing(Party party, long owner, String skill, int target, int depth) {
		String prefix = switch (skill) {
			case "ARTISAN" -> "LIFE_PART_";
			case "ALCHEMY" -> "LIFE_REAGENT_";
			case "LEATHERWORK" -> "LIFE_WRAP_";
			case "COOKING" -> "LIFE_RATION_";
			default -> throw new AssertionError("不支持的制造专业：" + skill);
		};
		int crafts = 0;
		while (level(owner, skill) < target) {
			assertTrue(crafts++ < 12000, "高档制造成长超出预算：" + skill);
			int current = level(owner, skill);
			String id = prefix + LifeSkillRules.toolTier(current) + (current % 2 == 0 ? "_FINE" : "");
			craft(party, owner, production.recipe(id), depth + 1);
			if (crafts % 16 == 0)
				cleanup(party, owner);
			if (crafts % 128 == 0)
				System.out.printf("NATURAL_HIGH_MANUFACTURING skill=%s level=%d crafts=%d gold=%d%n", skill,
						level(owner, skill), crafts, journey.queries.find(owner).gold());
		}
	}

	/** 逐级训练实际采集资格，使用当前等级资源点；原料回收只在结束派遣之后执行。 */
	private void trainGathering(Party party, long owner, String skill, int target, int depth) {
		int trips = 0;
		while (level(owner, skill) < target) {
			assertTrue(trips++ < 4000, "高档采集资格成长超出预算：" + skill);
			var node = gathering.nodes()
				.stream()
				.filter(value -> value.skill().equals(skill) && value.level() <= level(owner, skill))
				.max(Comparator.comparingInt(GatheringNode::level).thenComparing(GatheringNode::id))
				.orElseThrow();
			gather(party, owner, node, node.product(), quantity(owner, node.product()) + 24, depth + 1);
			cleanup(party, owner);
			if (trips % 64 == 0)
				System.out.printf("NATURAL_HIGH_GATHERING skill=%s level=%d trips=%d%n", skill, level(owner, skill),
						trips);
		}
	}

	/**
	 * 每次派遣只请求小批目标，保留真实空产出、资源收费与工具损坏；工具不足时递归执行正式补造。
	 * 采集前按已登记储备整理，不在活动进行中出售，也不把回收当作恢复角色资源的捷径。
	 */
	private void gather(Party party, long owner, GatheringNode node, String product, int target, int depth) {
		String tool = tool(node.skill(), LifeSkillRules.toolTier(node.level()));
		ensure(party, owner, tool, 1, depth + 1);
		journey.gatherMaterial(owner, target, node.skill(), tool, node.location(), node.id(), product, 0,
				() -> ensure(party, owner, tool, 1, depth + 1));
	}

	/**
	 * 制造前先备齐对应工具，再逐项准备材料；高档工具的首把允许前一档工具，沿正式目录计算。 每次调用只制造一件，配方实际产出数量、幂等和报废行为由原自然旅程的制造命令验证。
	 */
	private void craft(Party party, long owner, LifeProductionRecipe recipe, int depth) {
		String tool = tool(recipe.skill(), production.minimumToolTier(recipe));
		ensure(party, owner, tool, 1, depth + 1);
		for (var ingredient : recipe.ingredients().entrySet())
			ensure(party, owner, ingredient.getKey(), ingredient.getValue(), depth + 1);
		// 准备材料期间可能使用同一工具加工中间产物，因此最终制造前再次检查工具及全部原料。
		ensure(party, owner, tool, 1, depth + 1);
		for (var ingredient : recipe.ingredients().entrySet())
			ensure(party, owner, ingredient.getKey(), ingredient.getValue(), depth + 1);
		journey.craftWithInventory(owner, recipe.id(), tool, recipe.locations().getFirst());
	}

	/** 仅通过市场交付缺额，保留购买扣费、卖方结算与请求重放的原有断言。 */
	private void transfer(long seller, long buyer, String code, int count) {
		if (seller != buyer)
			for (int index = 0; index < count; index++)
				journey.tradeManufacturedTool(seller, buyer, code);
	}

	/** 清理只回收超出计划储备的生活材料，不处理装备、工具、药品或图纸。 */
	private void cleanup(Party party, long owner) {
		journey.prepareManufacturingInventory(owner, party.reserves.getOrDefault(owner, Map.of()));
	}

	/** 同一档只使用与专业工具种类一致的正式工具，不根据字符串猜测工具代码。 */
	private String tool(String skill, int tier) {
		String kind = LifeSkillRulesSkill.valueOf(skill).tool;
		return production.tools()
			.stream()
			.filter(value -> value.kind().equals(kind) && value.tier() == tier)
			.map(LifeProductionTool::item)
			.findFirst()
			.orElseThrow();
	}

	/** 统一从真实背包聚合全部堆叠，绑定材料仍由正式制造或交易规则决定能否使用。 */
	private int quantity(long owner, String code) {
		return journey.items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

	/** 查询实际熟练度推导的等级，不缓存跨制造后可能发生变化的专业状态。 */
	private int level(long owner, String skill) {
		return journey.learnedLifeSkillLevel(owner, skill);
	}

	/** 制造分工固定为既有四角色，避免临时遗忘专业而丢失此前投入。 */
	private long manufacturingOwner(Party party, String skill) {
		return switch (skill) {
			case "ARTISAN" -> party.artisan;
			case "ALCHEMY" -> party.alchemist;
			case "LEATHERWORK" -> party.leatherworker;
			case "COOKING" -> party.cook;
			default -> throw new AssertionError("没有对应制造供应角色：" + skill);
		};
	}

	/** 采集分工使用已学习的专业：工匠供矿木、制药供药草、制皮供兽皮和猎肉。 */
	private long gatheringOwner(Party party, String skill) {
		return switch (skill) {
			case "MINING", "LOGGING" -> party.artisan;
			case "HERBALISM" -> party.alchemist;
			case "HUNTING" -> party.leatherworker;
			default -> throw new AssertionError("没有对应采集供应角色：" + skill);
		};
	}

	/** 每条旅程独立持有供料角色、执行预算和保护储备，不在Spring单例中跨测试保存状态。 */
	private static final class Party {

		final long leatherworker;

		final long alchemist;

		final long artisan;

		final long cook;

		final Map<Long, Map<String, Integer>> reserves = new HashMap<>();

		int operations;

		Party(long leatherworker, long alchemist, long artisan, long cook) {
			this.leatherworker = leatherworker;
			this.alchemist = alchemist;
			this.artisan = artisan;
			this.cook = cook;
		}

	}

}
