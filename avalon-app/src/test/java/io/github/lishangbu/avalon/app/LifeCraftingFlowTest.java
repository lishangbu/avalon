package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.life.LifeProductionCatalog;
import io.github.lishangbu.avalon.content.life.LifeSkillRules;
import io.github.lishangbu.avalon.content.life.LifeSkillRulesSkill;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import io.github.lishangbu.avalon.world.application.command.LifeCraftingService;
import io.github.lishangbu.avalon.world.application.command.LifeShopService;
import io.github.lishangbu.avalon.world.application.command.LifeSkillCommandService;
import io.github.lishangbu.avalon.world.application.model.LifeCraftingRequest;
import io.github.lishangbu.avalon.world.application.model.LifeProductionRecipeView;
import io.github.lishangbu.avalon.world.application.model.LifeShopRequest;
import io.github.lishangbu.avalon.world.application.model.LifeSkillRequest;
import io.github.lishangbu.avalon.world.application.query.LifeProductionQueryService;
import io.github.lishangbu.avalon.world.domain.LifeToolRandom;
import io.github.lishangbu.avalon.world.persistence.repository.LifeSkillRepository;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** 真事务验证当前件产出后才报废、选定备用耗尽截断及请求重放。 */
class LifeCraftingFlowTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	LifeSkillCommandService learning;

	@Autowired
	LifeSkillRepository skills;

	@Autowired
	LifeCraftingService crafting;

	@Autowired
	LifeShopService shop;

	@Autowired
	LifeProductionCatalog catalog;

	@Autowired
	LifeProductionQueryService productionQueries;

	@Autowired
	JdbcClient jdbc;

	@MockitoBean
	LifeToolRandom random;

	private String accessToken;

	private long ready() {
		String name = "craft_" + UUID.randomUUID().toString().substring(0, 8);
		accessToken = registrations.register(name, "Life-test-12345", "测试").accessToken();
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, 7380));
		learning.change(owner, new LifeSkillRequest(UUID.randomUUID().toString(), LifeSkillRulesSkill.ARTISAN, false),
				false);
		jdbc.sql("UPDATE player_character SET location='GNOME_FORGE' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		transactions.run(() -> {
			inventory.grantWithBinding(owner, "ORE", "LIFE_ORE_0", 9, false, false);
			inventory.grantWithBinding(owner, "SPARE", "TOOL_WOOD_HAMMER", 3, false, false);
		});
		return owner;
	}

	@Test
	void recipePreviewUsesActualDecayAndCapRatherThanBaseReward() {
		long owner = ready();
		assertEquals(3.0, recipeView(owner, "LIFE_PART_0").proficiencyGain());
		assertNull(recipeView(owner, "LIFE_PART_1").proficiencyGain());
		for (int level : List.of(2, 3, 10)) {
			transactions.run(() -> {
				characters.lock(owner);
				skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN,
						LifeSkillRules.threshold(level) * 2);
			});
			var preview = recipeView(owner, "LIFE_PART_0");
			assertEquals(level, preview.skillLevel());
			assertEquals(level == 2 ? 1.5 : 0.0, preview.proficiencyGain());
			assertTrue(preview.available(), "低级配方仍可制造，只是不再增长熟练度");
		}
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN,
					LifeSkillRules.MAX_HALF_POINTS - 1);
		});
		var lastRecipe = catalog.recipes()
			.stream()
			.filter(r -> r.skill().equals("ARTISAN") && r.level() == 9)
			.findFirst()
			.orElseThrow();
		assertEquals(0.5, recipeView(owner, lastRecipe.id()).proficiencyGain());
	}

	private LifeProductionRecipeView recipeView(long owner, String id) {
		return productionQueries.view(owner)
			.recipes()
			.stream()
			.filter(r -> r.id().equals(id))
			.findFirst()
			.orElseThrow();
	}

	@Test
	void batchPromotionImmediatelyDecaysLaterRewardsAndReplayKeepsTheSameTotal() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(false);
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN, 254);
		});
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 3,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_0", false), 9));
		var result = crafting.craft(owner, request);
		assertEquals(3, result.completed());
		assertEquals(133.0, result.proficiency(), "127 + 3 + 1.5 + 1.5，不能整批沿用升级前奖励");
		assertEquals(1.5, recipeView(owner, request.recipe()).proficiencyGain());
		assertEquals(result, crafting.craft(owner, request));
		assertEquals(266L, skills.owned(characterQueries.requireId(owner)).get(LifeSkillRulesSkill.ARTISAN));
		verify(random, times(3)).breaks(anyInt());
	}

	@Test
	void equipmentMaterialsCanSpanStacksAndExactBudgetIsConsumedOnlyOnce() {
		long owner = ready();
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN, 768);
			inventory.grantStrict(owner, "GEAR_ORE_A", "LIFE_ORE_1", 99, false, false);
			inventory.grantStrict(owner, "GEAR_ORE_B", "LIFE_ORE_1", 93, false, true);
			inventory.grantStrict(owner, "GEAR_WOOD", "LIFE_WOOD_1", 96, false, false);
			inventory.grantStrict(owner, "GEAR_TOOL", "TOOL_COPPER_HAMMER", 1, false, false);
		});
		var materials = Map.of(code(owner, "LIFE_ORE_1", false), 99, code(owner, "LIFE_ORE_1", true), 93,
				code(owner, "LIFE_WOOD_1", false), 96);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "CRAFT_3_SWORD", 1,
				code(owner, "TOOL_COPPER_HAMMER", false), 1, materials);
		var result = crafting.craft(owner, request);
		assertEquals(1, result.produced());
		assertEquals(result, crafting.craft(owner, request));
		assertEquals(0, quantity(owner, "LIFE_ORE_1"));
		assertEquals(0, quantity(owner, "LIFE_WOOD_1"));
		assertEquals(1, quantity(owner, "CRAFT_3_SWORD"));
		assertNotNull(code(owner, "CRAFT_3_SWORD", true));
	}

	@Test
	void equipmentAcceptsMaterialsFromHundredsOfGatheringReceipts() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(false);
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN, 768);
			for (int index = 0; index < 192; index++)
				inventory.grantStrict(owner, "ORE_ROUND:" + index, "LIFE_ORE_1", 1, false, false);
			for (int index = 0; index < 96; index++)
				inventory.grantStrict(owner, "WOOD_ROUND:" + index, "LIFE_WOOD_1", 1, false, false);
			inventory.grantStrict(owner, "MANY_ROUNDS_TOOL", "TOOL_COPPER_HAMMER", 1, false, false);
		});
		var materials = new TreeMap<String, Integer>();
		items.list(owner)
			.stream()
			.filter(i -> List.of("LIFE_ORE_1", "LIFE_WOOD_1").contains(i.code()))
			.forEach(i -> materials.put(i.id(), i.quantity()));
		assertEquals(288, materials.size());
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "CRAFT_3_SWORD", 1,
				code(owner, "TOOL_COPPER_HAMMER", false), 1, materials);
		for (int attempt = 0; attempt < 2; attempt++)
			client.post()
				.uri("/api/life/production")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody()
				.jsonPath("$.completed")
				.isEqualTo(1);
		assertEquals(0, quantity(owner, "LIFE_ORE_1"));
		assertEquals(0, quantity(owner, "LIFE_WOOD_1"));
		assertEquals(1, quantity(owner, "CRAFT_3_SWORD"));
	}

	@Test
	void advancedAuxiliaryRejectsPreviousTierToolWithoutConsumingMaterials() {
		long owner = ready();
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN, 768);
			inventory.grantStrict(owner, "COPPER_ORE", "LIFE_ORE_1", 3, false, false);
			inventory.grantStrict(owner, "COPPER_HAMMER", "TOOL_COPPER_HAMMER", 1, false, false);
		});
		var before = items.list(owner);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_1", 1,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_1", false), 3));
		assertThrows(BusinessException.class, () -> crafting.craft(owner, request));
		assertEquals(before, items.list(owner));
		verifyNoInteractions(random);
		var valid = new LifeCraftingRequest(UUID.randomUUID().toString(), request.recipe(), 1,
				code(owner, "TOOL_COPPER_HAMMER", false), 1, request.materials());
		assertEquals(1, crafting.craft(owner, valid).produced());
		assertEquals(0, quantity(owner, "LIFE_ORE_1"));
		assertEquals(1, quantity(owner, "LIFE_PART_1"));
	}

	@Test
	void sellingMaterialsIsAtomicAndReplayDoesNotMintGold() {
		long owner = ready();
		jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		long before = characterQueries.find(owner).gold();
		var request = new LifeShopRequest(UUID.randomUUID().toString(), code(owner, "LIFE_ORE_0", false), 3);
		assertEquals(6, shop.sell(owner, request));
		assertEquals(6, shop.sell(owner, request));
		assertEquals(before + 6, characterQueries.find(owner).gold());
		assertEquals(6, quantity(owner, "LIFE_ORE_0"));
		assertThrows(BusinessException.class,
				() -> shop.sell(owner, new LifeShopRequest(UUID.randomUUID().toString(), request.item(), 7)));
		assertEquals(before + 6, characterQueries.find(owner).gold());
		transactions.run(() -> inventory.grantStrict(owner, "UNSELLABLE", "SEED_0_0", 1, false, false));
		assertThrows(BusinessException.class, () -> shop.sell(owner,
				new LifeShopRequest(UUID.randomUUID().toString(), code(owner, "SEED_0_0", false), 1)));
	}

	@Test
	void toolsPotionsAndUnequippedCraftedGearSellOnceButProtectedGearIsRetained() {
		long owner = ready();
		jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		long expectedGold = characterQueries.find(owner).gold();
		for (String product : List.of("TOOL_IRON_HAMMER", "LIFE_HEALTH_POTION_1", "CRAFT_1_SWORD")) {
			transactions.run(() -> inventory.grantStrict(owner, "SALE:" + product, product, 1,
					product.startsWith("CRAFT_"), true));
			var request = new LifeShopRequest(UUID.randomUUID().toString(), code(owner, product, true), 1);
			int price = catalog.sellPrices().get(product);
			assertEquals(price, shop.sell(owner, request));
			assertEquals(price, shop.sell(owner, request));
			expectedGold += price;
			assertEquals(expectedGold, characterQueries.find(owner).gold());
			assertEquals(0, quantity(owner, product));
		}
		transactions.run(() -> inventory.grantStrict(owner, "PROTECTED", "CRAFT_1_SWORD", 1, true, true));
		String gear = code(owner, "CRAFT_1_SWORD", true);
		var request = new LifeShopRequest(UUID.randomUUID().toString(), gear, 1);
		jdbc.sql("UPDATE owned_item SET equipped=true WHERE id=:id").param("id", Long.parseLong(gear)).update();
		assertThrows(BusinessException.class, () -> shop.sell(owner, request));
		jdbc.sql("UPDATE owned_item SET equipped=false, enhancement_level=1 WHERE id=:id")
			.param("id", Long.parseLong(gear))
			.update();
		assertThrows(BusinessException.class, () -> shop.sell(owner, request));
		assertEquals(expectedGold, characterQueries.find(owner).gold());
		assertEquals(1, quantity(owner, "CRAFT_1_SWORD"));
	}

	private String code(long owner, String code, boolean bound) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code) && item.bound() == bound)
			.findFirst()
			.orElseThrow()
			.id();
	}

	@Test
	void refinementConsumesOldEquipmentPreservesBindingAndCanMakeAnotherProfessionWeapon() {
		long owner = ready();
		transactions.run(() -> {
			characters.lock(owner);
			skills.progress(characterQueries.requireId(owner), LifeSkillRulesSkill.ARTISAN, 256);
			inventory.grant(owner, "OLD_STAFF", "CRAFT_1_STAFF", 1, true);
			inventory.grantWithBinding(owner, "REFINE_ORE", "LIFE_ORE_0", 24, false, false);
			inventory.grantWithBinding(owner, "REFINE_WOOD", "LIFE_WOOD_0", 12, false, false);
		});
		String ore = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_0") && item.quantity() == 24)
			.findFirst()
			.orElseThrow()
			.id();
		String old = code(owner, "CRAFT_1_STAFF", true);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "CRAFT_2_STAFF", 1,
				code(owner, "TOOL_WOOD_HAMMER", true), 1,
				Map.of(ore, 24, code(owner, "LIFE_WOOD_0", false), 12, old, 1));
		var result = crafting.craft(owner, request);
		assertEquals(1, result.produced());
		assertEquals(result, crafting.craft(owner, request));
		assertEquals(0, quantity(owner, "CRAFT_1_STAFF"));
		var gear = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("CRAFT_2_STAFF"))
			.findFirst()
			.orElseThrow();
		assertTrue(gear.bound());
		assertEquals("MAGE", gear.requiredProfession());
		assertEquals("史诗", gear.quality());
		assertEquals(10, gear.requiredLevel());
		assertEquals(16, gear.stats().get("magicAttack"));
		assertFalse(gear.stats().containsKey("physicalAttack"));
	}

	private int quantity(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

	@Test
	void breakEachPieceStopsAtSelectedToolBudgetAndReplayDoesNotReroll() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(true);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 3,
				code(owner, "TOOL_WOOD_HAMMER", false), 2, Map.of(code(owner, "LIFE_ORE_0", false), 9));
		var result = crafting.craft(owner, request);
		assertEquals(2, result.completed());
		assertEquals(2, result.broken());
		assertEquals("TOOLS_EXHAUSTED", result.stop());
		assertEquals(3, quantity(owner, "LIFE_ORE_0"));
		assertEquals(2, quantity(owner, "TOOL_WOOD_HAMMER"));
		assertEquals(2, quantity(owner, "LIFE_PART_0"));
		assertEquals(6, result.proficiency());
		assertEquals(result, crafting.craft(owner, request));
		assertEquals(2, quantity(owner, "LIFE_PART_0"));
		assertThrows(BusinessException.class, () -> crafting.craft(owner, new LifeCraftingRequest(request.requestId(),
				request.recipe(), 2, request.tool(), 2, request.materials())));
	}

	@Test
	void lastOwnedToolBreakStopsBatchAndRetryCannotConsumeRemainingMaterials() {
		long owner = ready();
		transactions.run(() -> inventory.consume(owner, Long.parseLong(code(owner, "TOOL_WOOD_HAMMER", false)),
				"TOOL_WOOD_HAMMER", 3));
		when(random.breaks(anyInt())).thenReturn(true);
		String tool = code(owner, "TOOL_WOOD_HAMMER", true);
		String ore = code(owner, "LIFE_ORE_0", false);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 3, tool, 1, Map.of(ore, 9));
		var result = crafting.craft(owner, request);
		assertEquals("TOOLS_EXHAUSTED", result.stop());
		assertEquals(1, result.completed());
		assertEquals(1, result.broken());
		assertEquals(0, quantity(owner, "TOOL_WOOD_HAMMER"));
		assertEquals(result, crafting.craft(owner, request));
		assertThrows(BusinessException.class, () -> crafting.craft(owner,
				new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 2, tool, 1, Map.of(ore, 6))));
		assertEquals(6, quantity(owner, "LIFE_ORE_0"));
		assertEquals(1, quantity(owner, "LIFE_PART_0"));
		verify(random, times(1)).breaks(100);
	}

	@Test
	void boundMaterialsBindProductsButToolBindingDoesNot() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(false);
		transactions.run(() -> inventory.grant(owner, "BOUND_ORE", "LIFE_ORE_0", 3, false));
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 1,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_0", true), 3));
		assertEquals(1, crafting.craft(owner, request).completed());
		assertNotNull(code(owner, "LIFE_PART_0", true));
		var unbound = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 1, request.tool(), 1,
				Map.of(code(owner, "LIFE_ORE_0", false), 3));
		assertEquals(1, crafting.craft(owner, unbound).completed());
		assertNotNull(code(owner, "LIFE_PART_0", false));
	}

	@Test
	void fullBagDoesNotConsumeMaterialsOrRollToolAndLastStackCanFreeSpace() {
		long owner = ready();
		transactions.run(() -> {
			for (int index = 0; index < 57; index++)
				inventory.grant(owner, "FILL:" + index, "STARTER_WEAPON", 1, true);
		});
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 1,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_0", false), 3));
		var result = crafting.craft(owner, request);
		assertEquals(0, result.completed());
		assertEquals("BAG_FULL", result.stop());
		assertEquals(9, quantity(owner, "LIFE_ORE_0"));
		verifyNoInteractions(random);
		transactions
			.run(() -> inventory.consume(owner, Long.parseLong(code(owner, "LIFE_ORE_0", false)), "LIFE_ORE_0", 6));
		var retry = new LifeCraftingRequest(UUID.randomUUID().toString(), request.recipe(), 1, request.tool(), 1,
				request.materials());
		assertEquals(1, crafting.craft(owner, retry).completed());
		assertEquals(0, quantity(owner, "LIFE_ORE_0"));
		verify(random).breaks(100);
	}

	@Test
	void batchStopsWhenOutputStackFillsAndCanResumeAfterMakingRoom() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(false);
		transactions.run(() -> {
			inventory.grantWithBinding(owner, "EXISTING_PARTS", "LIFE_PART_0", 98, false, false);
			for (int index = 0; index < 56; index++)
				inventory.grant(owner, "FILL:" + index, "STARTER_WEAPON", 1, true);
		});
		String ore = code(owner, "LIFE_ORE_0", false);
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 3,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(ore, 9));
		var result = crafting.craft(owner, request);
		assertEquals(1, result.completed());
		assertEquals("BAG_FULL", result.stop());
		assertEquals(99, quantity(owner, "LIFE_PART_0"));
		assertEquals(6, quantity(owner, "LIFE_ORE_0"));
		assertEquals(3.0, result.proficiency());
		assertEquals(result, crafting.craft(owner, request));
		verify(random, times(1)).breaks(100);
		transactions.run(() -> inventory.consumeForSale(owner, Long.parseLong(code(owner, "STARTER_WEAPON", true)),
				"STARTER_WEAPON", 1));
		// 原请求保持原回执；腾出位置后由新的请求继续剩余两件。
		assertEquals(result, crafting.craft(owner, request));
		var resumed = crafting.craft(owner, new LifeCraftingRequest(UUID.randomUUID().toString(), request.recipe(), 2,
				request.tool(), 1, Map.of(ore, 6)));
		assertEquals(2, resumed.completed());
		assertEquals("COMPLETED", resumed.stop());
		assertEquals(101, quantity(owner, "LIFE_PART_0"));
		assertEquals(0, quantity(owner, "LIFE_ORE_0"));
		assertEquals(9.0, resumed.proficiency());
		verify(random, times(3)).breaks(100);
	}

	@Test
	void unexpectedFailureRollsBackWholeBatchAndAllowsSameRequestRetry() {
		long owner = ready();
		when(random.breaks(anyInt())).thenReturn(false).thenThrow(new IllegalStateException("模拟结算异常"));
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 2,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_0", false), 6));
		assertThrows(IllegalStateException.class, () -> crafting.craft(owner, request));
		assertEquals(9, quantity(owner, "LIFE_ORE_0"));
		assertEquals(0, quantity(owner, "LIFE_PART_0"));
		assertEquals(0L, skills.owned(characterQueries.requireId(owner)).get(LifeSkillRulesSkill.ARTISAN));
		doReturn(false).when(random).breaks(anyInt());
		assertEquals(2, crafting.craft(owner, request).completed());
		assertEquals(3, quantity(owner, "LIFE_ORE_0"));
	}

	@Test
	void woodToolsCanBeBoughtOncePerRequestAndInsufficientGoldRollsBack() {
		long owner = ready();
		jdbc.sql("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		var request = new LifeShopRequest(UUID.randomUUID().toString(), "TOOL_WOOD_HAMMER", 2);
		assertThrows(BusinessException.class, () -> shop.buy(owner, request));
		assertEquals(4, quantity(owner, "TOOL_WOOD_HAMMER"));
		transactions.run(() -> characters.changeGold(owner, 40));
		shop.buy(owner, request);
		shop.buy(owner, request);
		assertEquals(6, quantity(owner, "TOOL_WOOD_HAMMER"));
		assertEquals(0, characterQueries.find(owner).gold());
		assertThrows(BusinessException.class,
				() -> shop.buy(owner, new LifeShopRequest(request.requestId(), "TOOL_WOOD_HOE", 2)));
	}

	@Test
	void locationAndSkillGatesDoNotConsumeAnything() {
		long owner = ready();
		var request = new LifeCraftingRequest(UUID.randomUUID().toString(), "LIFE_PART_0", 1,
				code(owner, "TOOL_WOOD_HAMMER", true), 1, Map.of(code(owner, "LIFE_ORE_0", false), 3));
		jdbc.sql("UPDATE player_character SET location='VILLAGE_HOME' WHERE id=:id")
			.param("id", characterQueries.requireId(owner))
			.update();
		assertThrows(BusinessException.class, () -> crafting.craft(owner, request));
		assertEquals(9, quantity(owner, "LIFE_ORE_0"));
		assertEquals(0L, skills.owned(characterQueries.requireId(owner)).get(LifeSkillRulesSkill.ARTISAN));
	}

}
