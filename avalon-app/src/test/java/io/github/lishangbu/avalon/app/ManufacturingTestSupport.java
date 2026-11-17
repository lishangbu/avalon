package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 制造与库存专项测试的正式命令助手。调用者显式准备角色阶段；这里只组织移动、采集、制造和交易，
 * 不授予经验、任务事实或最终产物，也不执行从开局到终章的重复成长。每次资产命令保留归属、扣费与幂等校验。
 */
class ManufacturingTestSupport {

	@Autowired
	io.github.lishangbu.avalon.content.world.WorldCatalog world;

	@Autowired
	io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService facts;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService lifeSkills;

	@Autowired
	io.github.lishangbu.avalon.world.application.query.LifeSkillQueryService lifeSkillQueries;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeShopService lifeShop;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.GatheringCommandService gathering;

	@Autowired
	io.github.lishangbu.avalon.world.application.command.LifeCraftingService crafting;

	@Autowired
	io.github.lishangbu.avalon.content.life.LifeProductionCatalog production;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	io.github.lishangbu.avalon.content.item.ItemCatalog itemCatalog;

	@Autowired
	io.github.lishangbu.avalon.inventory.application.command.EquipmentCommerceService equipmentCommerce;

	@Autowired
	BattleCommandService battles;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketListingCommandService listings;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService purchases;

	@Autowired
	io.github.lishangbu.avalon.market.application.command.MarketAccess marketAccess;

	/** 聚合真实背包中的所有同类堆叠，避免只统计首个堆叠。 */
	private int quantity(long owner, String code) {
		return items.list(owner).stream().filter(i -> i.code().equals(code)).mapToInt(i -> i.quantity()).sum();
	}

	/** 供专项供料助手读取正式专业等级；不写入熟练度或替代学习资格。 */
	int learnedLifeSkillLevel(long owner, String skill) {
		return lifeSkillQueries.list(owner)
			.stream()
			.filter(s -> s.code().equals(skill) && s.learned())
			.mapToInt(s -> s.level())
			.findFirst()
			.orElseThrow();
	}

	/** 通过正式商店出售超出保留量的材料，不直接删除库存。 */
	private void sellManufacturedSurplus(long owner, String code, int reserve) {
		if (quantity(owner, code) <= reserve)
			return;
		walk(owner, "CITY_SHOP");
		for (var item : items.list(owner)) {
			if (!item.code().equals(code) || quantity(owner, code) <= reserve)
				continue;
			int count = Math.min(item.quantity(), quantity(owner, code) - reserve);
			lifeShop.sell(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
					UUID.randomUUID().toString(), item.id(), count));
		}
	}

	/** 制造阶段切换时通过正式生活商店整理材料；调用者显式指定需要保留的特殊储备。 */
	void prepareManufacturingInventory(long owner, java.util.Map<String, Integer> reserves) {
		prepareMaterialInventory(owner, reserves, 20);
	}

	/** 高阶递归补造临近满包时只保留明确储备，避免每种历史材料保留20份仍占满格子。 */
	void prepareSupplyInventory(long owner, java.util.Map<String, Integer> reserves) {
		// 供应角色也亲自推进地图剧情，会积累不属于生活材料的旧装备。
		// 先按既有规则穿戴更合适的装备，再正式回收落后十级且未穿戴的装备，
		// 避免材料整理后仍然满包；现用装备、工具、图纸和显式原料储备均保留。
		// 城市商店同样支持装备回收，高阶供料不必为整理背包专程返回开篇村庄。
		clearUnusedEquipment(owner, 10, "CITY_SHOP");
		prepareMaterialInventory(owner, reserves, 0);
		// 主线战斗掉落的进化材料不属于生活商品，也没有回收价格；通过公开丢弃命令释放历史占用。
		// 仅处理无显式储备的普通材料，图纸、任务物品、技能书、药剂与工具均不在此范围。
		for (var item : items.list(owner)) {
			if (item.slot() != null || reserves.getOrDefault(item.code(), 0) > 0)
				continue;
			var definition = itemCatalog.item(item.code());
			if (definition.task()
					|| definition.category() != io.github.lishangbu.avalon.content.item.model.ItemCategory.MATERIAL
					|| production.sellPrices().containsKey(item.code()) || item.code().equals("E_FORGE_BLUEPRINT"))
				continue;
			var request = new io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest(
					UUID.randomUUID().toString(), "DISCARD", null, item);
			equipmentCommerce.execute(owner, request);
			equipmentCommerce.execute(owner, request);
		}
	}

	/**
	 * 联合任务需要同时携带多种物资，不能继续为每一种历史制造材料保留20份。 只保留本阶段提交所需的材料，其余可回收材料按正式价格出售；工具、药剂和任务物品仍保留。
	 * 后续制造缺料时重新采集或向队友采购，让清理产生的收入和再备料成本都进入正式结算。
	 */
	void prepareJointInventory(long owner) {
		prepareMaterialInventory(owner,
				java.util.Map.of("LIFE_ORE_0", 12, "LIFE_WOOD_0", 8, "LIFE_HERB_0", 12, "LIFE_REAGENT_0", 2,
						"LIFE_PART_0", 3, "LIFE_WRAP_0", 3, "LIFE_ORE_3", 6, "LIFE_PART_3", 4, "LIFE_REAGENT_3", 4),
				0);
	}

	/** 根据阶段储备出售真实余料；默认储备由阶段决定，不能影响不可出售的资产。 */
	private void prepareMaterialInventory(long owner, java.util.Map<String, Integer> reserves, int defaultReserve) {
		// 只整理生活材料及其稀有物，不出售药剂、工具、图纸、任务物品或人物装备。
		// 各类默认保留20份供跨专业补造使用；最终装备原料由调用者提供更高的显式储备。
		for (String code : items.list(owner).stream().map(InventoryItemView::code).distinct().toList()) {
			if (production.sellPrices().containsKey(code)
					&& (code.startsWith("LIFE_") && !code.contains("POTION") || code.startsWith("RARE_")))
				sellManufacturedSurplus(owner, code, reserves.getOrDefault(code, defaultReserve));
		}
	}

	/** 以固定测试价格交付一件物品，并验证买方扣费、卖方结算及重复请求幂等。 */
	void tradeManufacturedTool(long seller, long buyer, String code) {
		walk(seller, "CITY_SHOP");
		walk(buyer, "CITY_SHOP");
		marketAccess.teach(seller, true);
		marketAccess.teach(buyer, true);
		var item = items.list(seller)
			.stream()
			.filter(i -> i.code().equals(code) && !i.bound())
			.findFirst()
			.orElseThrow();
		int before = quantity(buyer, code);
		long buyerGoldBefore = queries.find(buyer).gold();
		var listing = listings.listBatch(seller,
				new io.github.lishangbu.avalon.market.application.model.MarketListingBatchRequest(
						UUID.randomUUID().toString(),
						List.of(new io.github.lishangbu.avalon.market.application.model.MarketListingSelection(
								Long.parseLong(item.id()), 1, item)),
						10));
		var request = new io.github.lishangbu.avalon.market.application.model.MarketPurchaseRequest(
				UUID.randomUUID().toString(), Long.parseLong(listing.listingId()), 1, 10);
		var result = purchases.buy(buyer, request);
		assertEquals(before + 1, quantity(buyer, code));
		assertEquals(buyerGoldBefore - 10, queries.find(buyer).gold());
		long sellerGold = queries.find(seller).gold();
		assertEquals(result, purchases.buy(buyer, request));
		assertEquals(sellerGold, queries.find(seller).gold());
		assertEquals(buyerGoldBefore - 10, queries.find(buyer).gold());
		assertEquals(before + 1, quantity(buyer, code));
	}

	/** 自然制造按真实堆叠选料，保留工具损耗及同请求重放校验。 */
	void craftWithInventory(long owner, String recipeId, String toolCode, String workshop) {
		assertEquals(1, craftWithInventory(owner, recipeId, toolCode, workshop, 1));
	}

	/** 批量制造按材料及升级边界收缩；工具提前耗尽时返回真实完成数，交由原流程补造。 */
	int craftWithInventory(long owner, String recipeId, String toolCode, String workshop, int maximum) {
		assertTrue(maximum > 0 && maximum <= 16);

		var recipe = production.recipe(recipeId);
		// 单次准备只读取一份背包快照；购买会改变库存，因此购买成功后必须重新读取。
		// 快照不跨制造命令复用，避免工具报废或材料扣除后继续使用过期数量。
		var owned = items.list(owner);
		if (owned.stream().noneMatch(item -> item.code().equals(toolCode))) {
			walk(owner, "CITY_SHOP");
			buyDeficit(owner, toolCode, 1);
			owned = items.list(owner);
		}
		int batch = maximum;
		for (var ingredient : recipe.ingredients().entrySet()) {
			int available = owned.stream()
				.filter(item -> item.code().equals(ingredient.getKey()))
				.mapToInt(item -> item.quantity())
				.sum();
			batch = Math.min(batch, available / ingredient.getValue());
		}
		assertTrue(batch > 0, "缺少真实采集材料：" + recipeId);
		if (batch > 1) {
			var skill = lifeSkillQueries.list(owner)
				.stream()
				.filter(value -> value.code().equals(recipe.skill()))
				.findFirst()
				.orElseThrow();
			long points = Math.round(skill.proficiency() * 2);
			// 仅读取熟练度并按正式奖励规则预估边界；实际熟练度及工具损耗仍逐件结算。
			for (int count = 1; count <= batch; count++) {
				points = io.github.lishangbu.avalon.content.life.LifeSkillRules.gain(points, recipe.level(),
						recipe.points());
				if (io.github.lishangbu.avalon.content.life.LifeSkillRules.level(points) != skill.level()) {
					batch = count;
					break;
				}
			}
		}
		var selected = new java.util.TreeMap<String, Integer>();
		for (var ingredient : recipe.ingredients().entrySet()) {
			int remaining = ingredient.getValue() * batch;
			for (var item : owned) {
				if (!item.code().equals(ingredient.getKey()))
					continue;
				int take = Math.min(remaining, item.quantity());
				if (take > 0)
					selected.put(item.id(), take);
				remaining -= take;
			}
			assertEquals(0, remaining, "缺少真实采集材料：" + ingredient.getKey());
		}
		var tool = owned.stream().filter(i -> i.code().equals(toolCode)).findFirst().orElseThrow();
		walk(owner, workshop);

		var request = new io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest(
				UUID.randomUUID().toString(), recipeId, batch, tool.id(), Math.min(tool.quantity(), batch), selected);

		var result = crafting.craft(owner, request);

		assertTrue(result.completed() > 0 && result.completed() <= batch);
		assertTrue(result.stop().equals("COMPLETED") || result.stop().equals("TOOLS_EXHAUSTED"));
		assertEquals(result, crafting.craft(owner, request));

		return result.completed();
	}

	/** 收藏准备必须同时释放历史余料并返回接取城市，避免商店整理改变后续任务地点。 */
	void prepareRareCollectionInventory(long owner) {
		// 收藏不需要生活材料；后续制造仍须真实重新备料，不删除库存或扩大背包容量。
		clearUnusedEquipment(owner, 10, "WEAPON_SHOP");
		prepareMaterialInventory(owner, java.util.Map.of(), 0);
		walk(owner, "SALAMANDER_CITY");
	}

	/** 稀有收藏补给入口，供局部回归通过真实购买验证背包和金币变化。 */
	void prepareRareCollectionSupplies(long owner) {
		// 前序捕捉可能已持有足量封印球；20是目标库存，不能每次额外购买20个。
		while (quantity(owner, "SEAL_V") < 20)
			battles.purchase(owner, "SEAL_V", UUID.randomUUID().toString());
	}

	/** 通过正式商店仅购买缺额；不能为商店未出售的材料或高档工具补发资产。 */
	void buyDeficit(long owner, String code, int required) {
		int missing = required - quantity(owner, code);
		if (missing <= 0)
			return;
		lifeShop.buy(owner, new io.github.lishangbu.avalon.world.application.model.LifeShopRequest(
				UUID.randomUUID().toString(), code, missing));
		assertTrue(quantity(owner, code) >= required);
	}

	/** 调用阶段选择已开放且支持装备回收的商店，装备筛选及正式出售规则保持一致。 */
	private void clearUnusedEquipment(long owner, int levelGap, String shop) {
		walk(owner, shop);
		equip(owner);

		for (var item : items.list(owner)) {
			if (item.slot() == null || item.equipped() || item.requiredLevel() > queries.find(owner).level() - levelGap)
				continue;
			equipmentCommerce.execute(owner,
					new io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest(
							UUID.randomUUID().toString(), item.sellPrice() > 0 ? "SELL" : "DESTROY", null, item));

		}
	}

	/** 按真实道路和任务事实寻找路线；专项供料助手不能直接修改角色位置。 */
	void walk(long owner, String target) {
		String current = queries.find(owner).location();
		if (current.equals(target))
			return;
		var completed = facts.codes(owner);
		var queue = new java.util.ArrayDeque<List<String>>();
		var visited = new java.util.HashSet<String>();
		queue.add(List.of(current));
		visited.add(current);
		while (!queue.isEmpty()) {
			var path = queue.remove();
			if (path.getLast().equals(target)) {
				for (String next : path.subList(1, path.size()))
					move(owner, next);
				return;
			}
			for (String next : world.place(path.getLast()).exits()) {
				if (!world.accessible(world.place(next), completed::contains) || !visited.add(next))
					continue;
				var extended = new java.util.ArrayList<>(path);
				extended.add(next);
				queue.add(extended);
			}
		}
		throw new AssertionError("没有已开放路线：" + current + " -> " + target);
	}

	/** 有限重试真实派遣；耗尽后先停止，再恢复资源并补造工具。测试时钟由调用者提供。 */
	void gatherMaterial(long owner, int required, String skillCode, String toolCode, String location, String node,
			String material, int retries, Runnable replenishTools) {
		assertTrue(retries < 10, "采集补购工具超过重试预算");
		if (quantity(owner, material) >= required)
			return;
		walk(owner, "CITY_SHOP");
		if (lifeSkillQueries.list(owner).stream().noneMatch(skill -> skill.code().equals(skillCode) && skill.learned()))
			lifeSkills.change(owner,
					new io.github.lishangbu.avalon.world.application.model.LifeSkillRequest(
							UUID.randomUUID().toString(),
							io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill.valueOf(skillCode), true),
					false);
		replenishTools.run();
		walk(owner, "CITY_PETS");
		battles.recover(owner);
		walk(owner, location);
		var tool = items.list(owner)
			.stream()
			.filter(i -> i.code().equals(toolCode))
			.max(Comparator.comparingInt(i -> i.quantity()))
			.orElseThrow();
		var pet = pets.list(owner).stream().filter(p -> p.partyPosition() == 0).findFirst().orElseThrow();

		var session = gathering.begin(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStart(
				UUID.randomUUID().toString(), node, pet.id(), tool.id(), tool.quantity()));
		int owned = quantity(owner, material);

		for (int second = 0; owned < required && second < 240; second++) {
			if (session.status().equals("TOOLS_EXHAUSTED") || session.status().equals("RESOURCES_LOW")) {
				// 正式停止派遣后递归回城：入口会执行幻兽中心恢复和按缺额补造，保留实际损耗与路程。
				// 背包满等其他停止原因仍直接暴露，不能用重派掩盖库存问题。
				gathering.stop(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStop(
						UUID.randomUUID().toString(), session.id()));
				gatherMaterial(owner, required, skillCode, toolCode, location, node, material, retries + 1,
						replenishTools);
				return;
			}
			assertEquals("ACTIVE", session.status(), () -> "采集停止，应保留真实资源约束：" + material + " bag=" + items.bag(owner));
			// 测试时钟每次推进一秒LifeClock；心跳仍执行正式间隔和资源扣费，无需真实等待。
			long completedBefore = session.completed();

			session = gathering.heartbeat(owner, new io.github.lishangbu.avalon.world.application.model.GatheringBeat(
					session.id(), session.revision(), true));

			// 只有完成回合才可能结算产物；空心跳仍正式执行，但无需重复读取完整背包。
			if (session.completed() != completedBefore) {

				owned = quantity(owner, material);

			}
		}
		gathering.stop(owner, new io.github.lishangbu.avalon.world.application.model.GatheringStop(
				UUID.randomUUID().toString(), session.id()));
		if (quantity(owner, material) < required) {
			// 空产出仍支付资源和工具消耗；单趟预算结束后恢复再采，不要求随机产出必然足量。
			assertEquals("ACTIVE", session.status(), () -> "采集结束仍缺少材料：" + material + " bag=" + items.bag(owner));
			gatherMaterial(owner, required, skillCode, toolCode, location, node, material, retries + 1, replenishTools);
			return;
		}
		walk(owner, "CITY_SHOP");
	}

	/** 按角色等级和职业选择现有最优装备，不注入装备或突破穿戴资格。 */
	private void equip(long owner) {
		var character = queries.find(owner);
		var selected = new java.util.LinkedHashMap<String, InventoryItemView>();
		items.list(owner)
			.stream()
			.filter(item -> item.slot() != null && item.requiredLevel() <= character.level())
			.filter(item -> item.requiredProfession() == null
					|| item.requiredProfession().equals(character.profession()))
			.sorted(Comparator.comparingInt(item -> item.stats().values().stream().mapToInt(Integer::intValue).sum()))
			.forEach(item -> selected.put(item.slot(), item));
		for (var item : selected.values())
			if (!item.equipped())
				inventory.equip(owner, Long.parseLong(item.id()));
	}

	/** 逐段执行正式移动，使用最新版本号保留并发校验。 */
	private void move(long owner, String... route) {
		for (String location : route)
			characters.move(owner, location, queries.find(owner).revision());
	}

}
