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

	/** 查询真实背包的全部堆叠，防止只检查第一堆而漏掉历史积压。 */
	private int quantity(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

}
