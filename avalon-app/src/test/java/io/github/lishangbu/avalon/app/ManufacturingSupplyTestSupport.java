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
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 阶段夹具的跨专业供料助手。只执行真实采集、制造和市场交付；等级、资金与地图资格由调用者准备。 */
class ManufacturingSupplyTestSupport {

	@Autowired
	ManufacturingTestSupport journey;

	@Autowired
	LifeProductionCatalog production;

	@Autowired
	GatheringCatalog gathering;

	/**
	 * 以真实制造和交易验证工具补造。调用者必须准备足够的专业等级；缺少资格时立即失败，不自动刷级。
	 */
	void supply(long leatherworker, long alchemist, long artisan, long cook, long recipient, String code,
			int required) {
		ensure(new Party(leatherworker, alchemist, artisan, cook), recipient, code, required, 0);
	}

	/**
	 * 只补足目标角色缺额。供应者由专业分工决定，跨角色交付经过正式市场扣费和幂等回执， 同一角色直接使用自己的库存，不制造自买挂单。递归深度与总操作数用来暴露意外依赖环。
	 */
	private void ensure(Party party, long recipient, String code, int required, int depth) {
		assertTrue(depth < 64 && party.operations++ < 10000, "专项供料依赖或执行预算异常：" + code);
		party.reserves.computeIfAbsent(recipient, ignored -> new HashMap<>()).merge(code, required, Math::max);
		// 采集报废后的递归补造也会接收中间材料，不能等整趟采集结束才释放库存。
		// 留出八格供不同原料与随机产物周转；只出售非计划余料，工具和图纸仍由正式规则保留。
		var bag = journey.items.bag(recipient);
		if (bag.usedSlots() >= bag.capacity() - 8)
			journey.prepareSupplyInventory(recipient, party.reserves.get(recipient));
		if (quantity(recipient, code) >= required)
			return;
		/*
		 * 必须先解析正式配方，再决定是否购买。部分中间产物同时存在商店报价和制造配方（例如
		 * LIFE_PART_3）。优先制造才能验证真实依赖链，而不是绕过工具与材料消耗。
		 */
		var recipe = production.recipe(code);
		if (recipe != null) {
			long maker = manufacturingOwner(party, recipe.skill());
			int deficit = required - quantity(recipient, code);
			// 跨角色只需备齐待交付缺额，供应者已有的成品可以直接使用；不能再加一遍现存数量。
			int target = maker == recipient ? required : deficit;
			if (quantity(maker, code) < target)
				requireLevel(maker, recipe.skill(), recipe.level());
			while (quantity(maker, code) < target)
				craft(party, maker, recipe, depth + 1);
			transfer(maker, recipient, code, deficit);
			return;
		}
		// 没有正式配方的基础物品才允许从商店补足，并优先选择城市商店。
		var offer = production.offers().stream().filter(value -> value.item().equals(code)).findFirst();
		if (offer.isPresent()) {
			String shop = offer.get().locations().contains("CITY_SHOP") ? "CITY_SHOP"
					: offer.get().locations().getFirst();
			journey.walk(recipient, shop);
			journey.buyDeficit(recipient, code, required);
			return;
		}
		// 普通及稀有产物均反查正式资源点；稀有物用实际产出计数，不把普通材料数量当作完成证据。
		var nodes = gathering.nodes()
			.stream()
			.filter(node -> code.equals(node.product()) || code.equals(node.rare()))
			.sorted(Comparator.comparingInt(GatheringNode::level))
			.toList();
		assertFalse(nodes.isEmpty(), "材料没有可执行的正式来源：" + code);
		String skill = nodes.getFirst().skill();
		long gatherer = gatheringOwner(party, skill);
		int deficit = required - quantity(recipient, code);
		int target = gatherer == recipient ? required : deficit;
		// 原料也先复用供应库存，已有材料足够时不应为交付额外训练采集或消耗工具。
		if (quantity(gatherer, code) >= target) {
			transfer(gatherer, recipient, code, deficit);
			return;
		}
		requireLevel(gatherer, skill, nodes.getFirst().level());
		var node = nodes.stream()
			.filter(value -> value.level() <= level(gatherer, skill))
			.max(Comparator.comparingInt(GatheringNode::level))
			.orElseThrow();
		gather(party, gatherer, node, code, target, depth + 1);
		transfer(gatherer, recipient, code, deficit);
	}

	/** 夹具必须显式提供当前配方或资源点的专业资格，避免专项回归意外退化成数小时成长。 */
	private void requireLevel(long owner, String skill, int required) {
		assertTrue(level(owner, skill) >= required, "供料夹具专业等级不足：" + skill + "，需要" + required);
	}

	/**
	 * 每次派遣只请求小批目标，保留真实空产出、资源收费与工具损坏；工具不足时递归执行正式补造。
	 * 阶段调用者在派遣结束后整理库存，不在活动进行中出售，也不把回收当作恢复角色资源的捷径。
	 */
	private void gather(Party party, long owner, GatheringNode node, String product, int target, int depth) {
		String tool = tool(node.skill(), LifeSkillRules.toolTier(node.level()));
		while (quantity(owner, product) < target) {
			ensure(party, owner, tool, 1, depth + 1);
			// 最终护甲可能一次需要192份原料；按24份组织多趟真实采集，避免把整批目标塞进单趟重试预算。
			// 分批只改变操作组织，空产出、工具损坏、回城恢复和地图移动仍由正式服务逐次执行。
			int batchTarget = Math.min(target, quantity(owner, product) + 24);
			journey.gatherMaterial(owner, batchTarget, node.skill(), tool, node.location(), node.id(), product, 0,
					() -> ensure(party, owner, tool, 1, depth + 1));
		}
	}

	/**
	 * 制造前先备齐对应工具，再逐项准备材料；高档工具的首把允许前一档工具，沿正式目录计算。 每次调用只制造一件，配方实际产出数量、幂等和报废行为由制造命令及重放断言验证。
	 */
	private void craft(Party party, long owner, LifeProductionRecipe recipe, int depth) {
		// 在逐项备料前保护整张配方，防止补第二种材料时把已经准备的第一种材料出售。
		reserveRecipe(party, owner, recipe);
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

	/** 储备按最大需求合并，递归工具补造不能覆盖外层最终装备已经登记的材料需求。 */
	private void reserveRecipe(Party party, long owner, LifeProductionRecipe recipe) {
		var reserves = party.reserves.computeIfAbsent(owner, ignored -> new HashMap<>());
		recipe.ingredients().forEach((code, count) -> reserves.merge(code, count, Math::max));
	}

	/** 仅通过市场交付缺额，保留购买扣费、卖方结算与请求重放的原有断言。 */
	private void transfer(long seller, long buyer, String code, int count) {
		if (seller != buyer)
			for (int index = 0; index < count; index++)
				journey.tradeManufacturedTool(seller, buyer, code);
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

	/** 每次调用独立持有供料角色、执行预算和保护储备，不在Spring单例中跨测试保存状态。 */
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
