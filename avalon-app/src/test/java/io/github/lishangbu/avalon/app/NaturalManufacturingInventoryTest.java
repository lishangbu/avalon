package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 最小容量回归：夹具只构造历史旅程中低档材料积压的背包，实际整理必须经过正式出售命令。 该测试不作为从零成长证据，也不为自然旅程注入金币、材料或专业熟练度。
 */
@Import(StoryOpeningJourney.class)
class NaturalManufacturingInventoryTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService queries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	LifeProductionCatalog production;

	@Autowired
	JdbcClient jdbc;

	@Autowired
	StoryOpeningJourney journey;

	@Autowired
	io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService facts;

	@Test
	void rareCollectionOnlyPurchasesMissingSealsInOccupiedBag() {
		String username = "rare_bag_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(username, "Inventory-test-1234", "测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "收藏补给库存回归", "WARRIOR");
		transactions.run(() -> {
			// 只为局部夹具开放商品并准备资金；自然长路线仍实际完成解锁任务。
			jdbc.sql("UPDATE player_character SET location='CITY_SHOP',gold=10000 WHERE account_id=:owner")
				.param("owner", owner)
				.update();
			facts.record(owner, "QUEST:U025");
			// 已有90个封印球占一格，再带59堆材料填满背包；额外采购会跨堆叠上限而失败。
			inventory.grantStrict(owner, "SEAL", "SEAL_V", 90, false, true);
			for (int index = 0; index < 59; index++)
				inventory.grantStrict(owner, "WRAP:" + index, "LIFE_WRAP_1", 99, false, false);
		});
		long beforeGold = queries.find(owner).gold();
		journey.prepareRareCollectionSupplies(owner);
		assertEquals(90, quantity(owner, "SEAL_V"));
		assertEquals(beforeGold, queries.find(owner).gold());
		// 同一阶段再次准备时复用现有补给，不能继续花钱堆满背包。
		journey.prepareRareCollectionSupplies(owner);
		assertEquals(90, quantity(owner, "SEAL_V"));
		assertEquals(beforeGold, queries.find(owner).gold());
	}

	@Test
	void stageCleanupPreservesReservesToolsPotionsAndBlueprintAndPaysConfiguredPrices() {
		String username = "bag_stage_0" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(username, "Inventory-test-1234", "测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "制造库存回归", "WARRIOR");
		transactions.run(() -> {
			// 只在最小回归夹具中定位到可出售地点；自然旅程仍通过正式道路行走。
			jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE account_id=:owner")
				.param("owner", owner)
				.update();
			for (int index = 0; index < 30; index++)
				inventory.grantStrict(owner, "OLD_WRAP:" + index, "LIFE_WRAP_1", 99, false, false);
			for (int index = 0; index < 24; index++)
				inventory.grantStrict(owner, "OLD_RARE:" + index, "RARE_HUNTING_2", 99, false, false);
			inventory.grantStrict(owner, "RESERVED_HIDE", "LIFE_HIDE_3", 24, false, false);
			inventory.grantStrict(owner, "RESERVED_TOOL", "TOOL_SILVER_NEEDLE", 3, false, false);
			inventory.grantStrict(owner, "RESERVED_POTION", "LIFE_HEALTH_POTION_4", 40, false, false);
			inventory.grantStrict(owner, "RESERVED_BLUEPRINT", "E_FORGE_BLUEPRINT", 1, false, false);
		});
		assertTrue(items.bag(owner).usedSlots() >= 54, "夹具必须复现历史积压占用，不能在空背包上验证整理");
		long beforeGold = queries.find(owner).gold();
		journey.prepareManufacturingInventory(owner, Map.of("LIFE_HIDE_3", 24));
		assertEquals(20, quantity(owner, "LIFE_WRAP_1"));
		assertEquals(20, quantity(owner, "RARE_HUNTING_2"));
		assertEquals(24, quantity(owner, "LIFE_HIDE_3"));
		assertEquals(3, quantity(owner, "TOOL_SILVER_NEEDLE"));
		assertEquals(40, quantity(owner, "LIFE_HEALTH_POTION_4"));
		assertEquals(1, quantity(owner, "E_FORGE_BLUEPRINT"));
		long expectedGold = beforeGold + (2970L - 20) * production.sellPrices().get("LIFE_WRAP_1")
				+ (2376L - 20) * production.sellPrices().get("RARE_HUNTING_2");
		assertEquals(expectedGold, queries.find(owner).gold());
		assertTrue(items.bag(owner).usedSlots() <= 10, "应为后续普通产出、随机稀有物和备用工具留出空间");
		// 重复整理不再出售储备，也不应凭空增加金币。
		journey.prepareManufacturingInventory(owner, Map.of("LIFE_HIDE_3", 24));
		assertEquals(expectedGold, queries.find(owner).gold());
	}

	@Test
	void jointCleanupFreesHistoricalMaterialSlotsAndKeepsDeliverySupplies() {
		String username = "joint_bag_0" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(username, "Inventory-test-1234", "测试");
		long owner = accounts.accountId(username);
		characters.create(owner, "联合物资库存回归", "WARRIOR");
		transactions.run(() -> {
			// 夹具构造每堆不足20份的历史库存，复现普通制造整理无法释放格子的情况。
			jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE account_id=:owner")
				.param("owner", owner)
				.update();
			for (String code : java.util.List.of("LIFE_WRAP_1", "LIFE_WRAP_2", "LIFE_WRAP_3", "LIFE_HIDE_3"))
				inventory.grantStrict(owner, "OLD:" + code, code, 20, false, false);
			inventory.grantStrict(owner, "JOINT_WOOD", "LIFE_WOOD_0", 8, false, false);
			inventory.grantStrict(owner, "JOINT_PART", "LIFE_PART_3", 4, false, false);
			inventory.grantStrict(owner, "KEEP_TOOL", "TOOL_SILVER_NEEDLE", 1, false, false);
			inventory.grantStrict(owner, "KEEP_BLUEPRINT", "E_FORGE_BLUEPRINT", 1, false, false);
		});
		int beforeSlots = items.bag(owner).usedSlots();
		long beforeGold = queries.find(owner).gold();
		journey.prepareJointInventory(owner);
		assertEquals(beforeSlots - 4, items.bag(owner).usedSlots());
		assertEquals(0, quantity(owner, "LIFE_WRAP_3"));
		assertEquals(8, quantity(owner, "LIFE_WOOD_0"));
		assertEquals(4, quantity(owner, "LIFE_PART_3"));
		assertEquals(1, quantity(owner, "TOOL_SILVER_NEEDLE"));
		assertEquals(1, quantity(owner, "E_FORGE_BLUEPRINT"));
		long expectedIncome = java.util.List.of("LIFE_WRAP_1", "LIFE_WRAP_2", "LIFE_WRAP_3", "LIFE_HIDE_3")
			.stream()
			.mapToLong(code -> 20L * production.sellPrices().get(code))
			.sum();
		assertEquals(beforeGold + expectedIncome, queries.find(owner).gold());
	}

	/** 查询真实背包的全部堆叠，防止只检查第一堆而漏掉历史积压。 */
	private int quantity(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

}
