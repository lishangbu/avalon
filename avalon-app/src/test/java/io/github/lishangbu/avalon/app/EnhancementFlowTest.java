package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.model.PartyFactory;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementCommandService;
import io.github.lishangbu.avalon.inventory.application.command.EnhancementPreviewService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialMode;
import io.github.lishangbu.avalon.inventory.application.model.EnhancementRequest;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.domain.EnhancementRoll;
import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService;
import io.github.lishangbu.avalon.market.application.model.MarketListingRequest;
import io.github.lishangbu.avalon.market.application.model.MarketPurchaseRequest;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import static org.junit.jupiter.api.Assertions.*;

/** 隔离等级和随机边界，保留真实库存、金币、角色锁与数据库事务，不修改生产等级曲线。 */
class EnhancementFlowTest extends SpringIntegrationTest {

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
	EnhancementPreviewService previews;

	@Autowired
	EnhancementCommandService commands;

	@Autowired
	EntityManager em;

	@Autowired
	PartyFactory party;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@Autowired
	CombatVitalsRepository combatVitalsRepository;

	@Autowired
	MarketAccess marketAccess;

	@Autowired
	MarketListingCommandService marketListings;

	@Autowired
	MarketPurchaseCommandService marketPurchases;

	private final AtomicInteger rolls = new AtomicInteger();

	private int outcome;

	private final Map<Long, String> tokens = new HashMap<>();

	@BeforeEach
	void setup() {
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 30;
			}
		}, GrowthRules.class);
		TestDoubles.install(new EnhancementRoll() {
			@Override
			public int next() {
				rolls.incrementAndGet();
				return outcome;
			}
		}, EnhancementRoll.class);
	}

	@AfterEach
	void restore() {
		TestDoubles.install(new GrowthRules(), GrowthRules.class);
		TestDoubles.install(new EnhancementRoll(), EnhancementRoll.class);
	}

	private long ready(boolean boundStone) {
		String name = "enh_" + UUID.randomUUID().toString().substring(0, 8);
		var token = registrations.register(name, "Enhance-test-1234", "测试").accessToken();
		long owner = accounts.accountId(name);
		tokens.put(owner, token);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, 1000);
			inventory.grantStrict(owner, "GEAR", "STARTER_WEAPON", 1, true, false);
			inventory.grantStrict(owner, "STONE", "ENHANCEMENT_STONE_I", 20, false, boundStone);
			long character = characters.lock(owner).id();
			em.flush();
			em.clear();
			em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
				.setParameter("id", character)
				.executeUpdate();
		});
		return owner;
	}

	private long gear(long owner) {
		return Long.parseLong(items.list(owner).stream().filter(i -> i.slot() != null).findFirst().orElseThrow().id());
	}

	private int stones(long owner) {
		return items.list(owner)
			.stream()
			.filter(i -> i.code().equals("ENHANCEMENT_STONE_I"))
			.mapToInt(i -> i.quantity())
			.sum();
	}

	private EnhancementRequest request(long owner, long id, CultivationMaterialMode mode) {
		return new EnhancementRequest(UUID.randomUUID().toString(), previews.preview(owner, id, mode));
	}

	@Test
	void successReplayAndFailureUseOneResultAndDoNotChangeResalePrice() {
		long owner = ready(false), id = gear(owner);
		long gold = characterQueries.find(owner).gold();
		var request = request(owner, id, null);
		assertEquals(0, rolls.get());
		var result = commands.enhance(owner, id, request);
		assertTrue(result.success());
		assertEquals(1, result.item().enhancementLevel());
		assertFalse(result.item().bound());
		assertEquals(request.expected().before().sellPrice(), result.item().sellPrice());
		assertEquals(gold - 10, characterQueries.find(owner).gold());
		assertEquals(19, stones(owner));
		outcome = 99;
		assertEquals(result, commands.enhance(owner, id, request));
		assertEquals(1, rolls.get());
		var failure = commands.enhance(owner, id, request(owner, id, null));
		assertFalse(failure.success());
		assertEquals(0, failure.item().enhancementLevel());
		assertEquals(request.expected().before().stats(), failure.item().stats());
		assertEquals(gold - 30, characterQueries.find(owner).gold());
		assertEquals(17, stones(owner));
	}

	@Test
	void boundMaterialsRequireExplicitSelectionAndBindEvenWhenAttemptFails() {
		long owner = ready(true), id = gear(owner);
		assertThrows(BusinessException.class, () -> previews.preview(owner, id, null));
		assertEquals(0, rolls.get());
		var request = request(owner, id, CultivationMaterialMode.BOUND_FIRST);
		assertTrue(request.expected().resultBound());
		outcome = 99;
		var result = commands.enhance(owner, id, request);
		assertFalse(result.success());
		assertEquals(0, result.item().enhancementLevel());
		assertTrue(result.item().bound());
		assertEquals(19, stones(owner));
		assertEquals(CultivationMaterialMode.BOUND_FIRST, previews.preview(owner, id, null).mode());
	}

	@Test
	void rollbackRestoresGoldMaterialsBindingLevelAndAllowsSameRequestRetry() {
		long owner = ready(true), id = gear(owner);
		var request = request(owner, id, CultivationMaterialMode.BOUND_FIRST);
		long gold = characterQueries.find(owner).gold();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.enhance(owner, id, request);
			throw new IllegalStateException("模拟最终提交失败");
		}));
		assertEquals(gold, characterQueries.find(owner).gold());
		assertEquals(20, stones(owner));
		assertEquals(request.expected().before(),
				items.list(owner).stream().filter(i -> i.slot() != null).findFirst().orElseThrow());
		assertTrue(commands.enhance(owner, id, request).success());
		assertEquals(19, stones(owner));
	}

	@Test
	void staleForeignAndBattleRequestsCannotConsumeOrDraw() {
		long owner = ready(false), other = ready(false), id = gear(owner);
		var request = request(owner, id, null);
		assertThrows(BusinessException.class, () -> commands.enhance(other, id, request));
		inventory.equip(owner, id);
		assertThrows(BusinessException.class, () -> commands.enhance(owner, id, request));
		var worn = request(owner, id, null);
		transactions.run(() -> characters.setBattle(owner, 123L, false));
		assertThrows(BusinessException.class, () -> commands.enhance(owner, id, worn));
		assertEquals(0, rolls.get());
		assertEquals(20, stones(owner));
	}

	@Test
	void insufficientGoldChangedMaterialsAndMaximumLevelRejectBeforeDrawing() {
		long owner = ready(false), id = gear(owner);
		var request = request(owner, id, null);
		long initialGold = characterQueries.find(owner).gold();
		transactions.run(() -> characters.changeGold(owner, -initialGold));
		assertThrows(BusinessException.class, () -> commands.enhance(owner, id, request));
		assertEquals(20, stones(owner));
		assertEquals(0, rolls.get());
		transactions.run(() -> {
			characters.changeGold(owner, initialGold);
			inventory.take(owner, "ENHANCEMENT_STONE_I", 20);
			inventory.grantStrict(owner, "NEW_STONES", "ENHANCEMENT_STONE_I", 20, false, false);
		});
		// 总数相同但实际扣除实例已变，旧确认不能悄悄换用另一批材料。
		assertThrows(BusinessException.class, () -> commands.enhance(owner, id, request));
		assertEquals(initialGold, characterQueries.find(owner).gold());
		assertEquals(20, stones(owner));
		assertEquals(0, rolls.get());
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET enhancement_level=7 WHERE id=:id")
			.setParameter("id", id)
			.executeUpdate());
		assertThrows(BusinessException.class, () -> previews.preview(owner, id, null));
		assertEquals(0, rolls.get());
	}

	@Test
	void concurrentRequestDrawsAndChargesExactlyOnce() throws Exception {
		long owner = ready(false), id = gear(owner);
		var request = request(owner, id, null);
		long gold = characterQueries.find(owner).gold();
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				start.await();
				return commands.enhance(owner, id, request);
			});
			var second = executor.submit(() -> {
				start.await();
				return commands.enhance(owner, id, request);
			});
			start.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(1, rolls.get());
		assertEquals(19, stones(owner));
		assertEquals(gold - 10, characterQueries.find(owner).gold());
	}

	@Test
	void enhancedUnboundEquipmentTradesAndBuyerCanContinueWhileBoundFailureCannotList() {
		long seller = ready(false), buyer = ready(false), id = gear(seller);
		marketAccess.teach(seller, true);
		marketAccess.teach(buyer, true);
		var first = commands.enhance(seller, id, request(seller, id, null));
		var listing = marketListings.listItem(seller,
				new MarketListingRequest(UUID.randomUUID().toString(), id, 1, 100, first.item()));
		assertThrows(BusinessException.class, () -> previews.preview(seller, id, null));
		long sellerGold = characterQueries.find(seller).gold(), buyerGold = characterQueries.find(buyer).gold();
		var receipt = marketPurchases.buy(buyer,
				new MarketPurchaseRequest(UUID.randomUUID().toString(), Long.parseLong(listing.listingId()), 1, 100));
		assertEquals(Long.toString(id), receipt.itemId());
		assertEquals(sellerGold + 95, characterQueries.find(seller).gold());
		assertEquals(buyerGold - 100, characterQueries.find(buyer).gold());
		var next = commands.enhance(buyer, id, request(buyer, id, null));
		assertEquals(2, next.item().enhancementLevel());
		assertFalse(next.item().bound());
		assertEquals(18, stones(buyer));
		assertEquals(first.item().sellPrice(), next.item().sellPrice());
		assertThrows(BusinessException.class, () -> previews.preview(seller, id, null));

		long boundOwner = ready(true), boundId = gear(boundOwner);
		marketAccess.teach(boundOwner, true);
		outcome = 99;
		var failed = commands.enhance(boundOwner, boundId,
				request(boundOwner, boundId, CultivationMaterialMode.BOUND_FIRST));
		assertTrue(failed.item().bound());
		assertFalse(failed.success());
		assertThrows(BusinessException.class, () -> marketListings.listItem(boundOwner,
				new MarketListingRequest(UUID.randomUUID().toString(), boundId, 1, 100, failed.item())));
		assertEquals(19, stones(boundOwner));
	}

	@Test
	void httpPreviewRoundTripPreservesMaterialIdsAndRejectsAnonymousOrForgedRequests() {
		long owner = ready(false), id = gear(owner);
		client.get().uri("/api/enhancement/access").exchange().expectStatus().isEqualTo(401);
		client.post()
			.uri("/api/enhancement/" + id)
			.contentType(MediaType.APPLICATION_JSON)
			.body("{}")
			.exchange()
			.expectStatus()
			.isEqualTo(401);
		var token = tokens.get(owner);
		client.get()
			.uri("/api/enhancement/access")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.available")
			.isEqualTo(true);
		String preview = client.get()
			.uri("/api/enhancement/" + id)
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.returnResult(String.class)
			.getResponseBody();
		String request = "{\"requestId\":\"" + UUID.randomUUID() + "\",\"expected\":" + preview + "}";
		String result = new String(client.post()
			.uri("/api/enhancement/" + id)
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.exchange()
			.expectStatus()
			.isEqualTo(200)
			.expectBody()
			.jsonPath("$.success")
			.isEqualTo(true)
			.jsonPath("$.item.id")
			.isEqualTo(Long.toString(id))
			.returnResult()
			.getResponseBody(), StandardCharsets.UTF_8);
		assertEquals(result,
				client.post()
					.uri("/api/enhancement/" + id)
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.body(request)
					.exchange()
					.expectStatus()
					.isEqualTo(200)
					.returnResult(String.class)
					.getResponseBody());
		assertEquals(1, rolls.get());
		client.post()
			.uri("/api/enhancement/" + id)
			.header("Authorization", "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.body("null")
			.exchange()
			.expectStatus()
			.isEqualTo(400);
		client.get()
			.uri("/api/enhancement/" + id + "?mode=FREE")
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(404);
		// 实际生产等级曲线仍是15级上限，直连接口也不能绕过开放条件。
		TestDoubles.install(new GrowthRules(), GrowthRules.class);
		client.get()
			.uri("/api/enhancement/" + id)
			.header("Authorization", "Bearer " + token)
			.exchange()
			.expectStatus()
			.isEqualTo(409);
	}

	@Test
	void wornSuccessDoesNotHealAndFailureClampsResourcesWithoutReviving() {
		long owner = ready(false);
		transactions.run(() -> inventory.grantStrict(owner, "BODY", "STARTER_BODY", 1, true, false));
		long id = Long.parseLong(
				items.list(owner).stream().filter(i -> i.code().equals("STARTER_BODY")).findFirst().orElseThrow().id());
		// 同时覆盖生命和魔力的上限变化；原始属性夹具不修改任何生产装备模板。
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET stats=:stats WHERE id=:id")
			.setParameter("stats", "{\"health\":100,\"mana\":100}")
			.setParameter("id", id)
			.executeUpdate());
		inventory.equip(owner, id);
		long character = Long.parseLong(characterQueries.find(owner).id());
		var before = party.create(owner, false).getFirst().getStats();
		transactions.run(() -> combatVitalsStore.saveVitals(character, character, before.health(), before.mana()));
		commands.enhance(owner, id, request(owner, id, null));
		transactions.run(() -> {
			assertEquals(before.health(), combatVitalsRepository.get(character).getHp());
			assertEquals(before.mana(), combatVitalsRepository.get(character).getMp());
		});
		var raised = party.create(owner, false).getFirst().getStats();
		assertEquals(before.health() + 5, raised.health());
		assertEquals(before.mana() + 5, raised.mana());
		transactions.run(() -> combatVitalsStore.saveVitals(character, character, raised.health(), raised.mana()));
		outcome = 99;
		var request = request(owner, id, null);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.enhance(owner, id, request);
			throw new IllegalStateException("模拟资源更新后事务失败");
		}));
		transactions.run(() -> assertEquals(raised.health(), combatVitalsRepository.get(character).getHp()));
		assertFalse(commands.enhance(owner, id, request).success());
		transactions.run(() -> {
			assertEquals(before.health(), combatVitalsRepository.get(character).getHp());
			assertEquals(before.mana(), combatVitalsRepository.get(character).getMp());
			combatVitalsStore.saveVitals(character, character, 0, 3);
		});
		outcome = 0;
		commands.enhance(owner, id, request(owner, id, null));
		transactions.run(() -> {
			assertEquals(0, combatVitalsRepository.get(character).getHp());
			assertEquals(3, combatVitalsRepository.get(character).getMp());
		});
	}

}
