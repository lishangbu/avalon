package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.market.application.command.MarketAccess;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService.Request;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService.BatchRequest;
import io.github.lishangbu.avalon.market.application.command.MarketListingCommandService.Selection;
import io.github.lishangbu.avalon.market.application.command.MarketPurchaseCommandService;
import io.github.lishangbu.avalon.market.application.command.MarketReturnCommandService;
import io.github.lishangbu.avalon.market.application.query.MarketQueryService;
import io.github.lishangbu.avalon.market.persistence.entity.MarketListing;
import io.github.lishangbu.avalon.market.persistence.repository.MarketListingRepository;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/** 人物等级使用隔离替身，实际库存、挂单、请求凭证与并发角色锁不替换。 */
class MarketListingTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	MarketAccess access;

	@Autowired
	MarketListingCommandService market;

	@Autowired
	MarketListingRepository listings;

	@Autowired
	EntityManager em;

	@Autowired
	MarketPurchaseCommandService purchases;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	MarketReturnCommandService returns;

	@Autowired
	MarketQueryService queries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	CombatVitalsStore combatVitalsStore;

	@BeforeEach
	void level() {
		TestDoubles.install(new GrowthRules() {
			@Override
			public int level(long experience) {
				return 16;
			}

			// 只为市场隔离夹具提供16级携带空间，不外推生产经验曲线。
			@Override
			public int carryingCapacity(int characterLevel) {
				return 6;
			}
		}, GrowthRules.class);
	}

	@AfterEach
	void restore() {
		TestDoubles.install(new GrowthRules(), GrowthRules.class);
	}

	private long ready() {
		String name = "list_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Market-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			em.createNativeQuery("UPDATE player_character SET location='CITY_SHOP' WHERE id=:id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate();
			inventory.grantStrict(owner, "DROP", "WIND_FEATHER", 99, false, false);
		});
		access.teach(owner, true);
		return owner;
	}

	private Request request(long owner) {
		var item = items.list(owner).getFirst();
		return new Request(UUID.randomUUID().toString(), Long.parseLong(item.id()), 1, 2, item);
	}

	@Test
	void enhancementFilterUsesVerifiedEquipmentInstanceAndDoesNotMatchMaterialsAtZero() {
		long owner = ready();
		transactions.run(() -> {
			inventory.grantStrict(owner, "PLUS_ZERO", "STARTER_WEAPON", 1, true, false);
			inventory.grantStrict(owner, "PLUS_SEVEN", "STARTER_WEAPON", 1, true, false);
		});
		var gear = items.list(owner).stream().filter(i -> i.slot() != null).toList();
		var zero = gear.getFirst();
		var original = gear.getLast();
		// 只建立强化存档夹具；正式强化事务由EnhancementFlowTest独立覆盖。
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET enhancement_level=7 WHERE id=:id")
			.setParameter("id", Long.parseLong(original.id()))
			.executeUpdate());
		var enhanced = items.list(owner).stream().filter(i -> i.id().equals(original.id())).findFirst().orElseThrow();
		var first = market.listItem(owner,
				new Request(UUID.randomUUID().toString(), Long.parseLong(zero.id()), 1, 2, zero));
		var second = market.listItem(owner,
				new Request(UUID.randomUUID().toString(), Long.parseLong(enhanced.id()), 1, 3, enhanced));
		var material = market.listItem(owner, request(owner));
		var plusSeven = queries.search(owner, "", "ALL", null, null, null, null, "NEWEST", 0, 50, null, 7);
		assertTrue(plusSeven.listings()
			.stream()
			.anyMatch(i -> i.id().equals(second.listingId()) && i.enhancementLevel() == 7));
		assertFalse(plusSeven.listings()
			.stream()
			.anyMatch(i -> i.id().equals(first.listingId()) || i.id().equals(material.listingId())));
		var plusZero = queries.search(owner, "", "ALL", null, null, null, null, "NEWEST", 0, 50, null, 0);
		assertTrue(plusZero.listings().stream().anyMatch(i -> i.id().equals(first.listingId())));
		assertFalse(plusZero.listings()
			.stream()
			.anyMatch(i -> i.id().equals(material.listingId()) || i.id().equals(second.listingId())));
		var detail = queries.detail(owner, Long.parseLong(second.listingId()));
		assertEquals(7, detail.item().enhancementLevel());
		assertEquals(enhanced.stats(), detail.item().stats());
		assertThrows(BusinessException.class,
				() -> queries.search(owner, "", "ALL", null, null, null, null, "NEWEST", 0, 20, null, 8));
	}

	private MarketListingCommandService.CreatureRequest creatureRequest(long owner) {
		// 卖家保留可用出战伙伴，交易标的只负责验证托管和成交，不绕过最后伙伴保护。
		long retained = transactions
			.call(() -> creatures.capture(owner, "WILD:RETAINED", "CLAW_CAT", 1, "A", "完好防护", List.of("水弹")));
		creatures.position(owner, retained, 0);
		long id = transactions.call(() -> creatures.capture(owner, "WILD:" + UUID.randomUUID(), "STONE_GOLEM", 1, "H",
				"守护", List.of("碎岩击")));
		creatures.position(owner, id, 4);
		var pet = pets.list(owner).stream().filter(p -> p.id().equals(Long.toString(id))).findFirst().orElseThrow();
		return new MarketListingCommandService.CreatureRequest(UUID.randomUUID().toString(), id, 100, pet);
	}

	@Test
	void petPurchaseReplaysOnceAndPaysTaxWithoutRepeatingCollectionReward() throws Exception {
		long seller = ready(), buyer = ready();
		var request = creatureRequest(seller);
		var listed = market.listCreature(seller, request);
		assertEquals(listed, market.listCreature(seller, request));
		assertTrue(pets.list(seller).stream().noneMatch(p -> p.id().equals(Long.toString(request.creatureId()))));
		transactions.run(() -> characters.changeGold(buyer, 1000));
		long sellerGold = characterQueries.find(seller).gold();
		long buyerGold = characterQueries.find(buyer).gold();
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 100);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				return purchases.buy(buyer, buy);
			});
			var b = executor.submit(() -> {
				start.await();
				return purchases.buy(buyer, buy);
			});
			start.countDown();
			assertEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(sellerGold + 95, characterQueries.find(seller).gold());
		// 首次图鉴奖励10金币与成交在同一事务，幂等重试不能重发。
		assertEquals(buyerGold - 100 + 10, characterQueries.find(buyer).gold());
		assertEquals(1, pets.list(buyer).size());
		assertEquals(Long.toString(request.creatureId()), pets.list(buyer).getFirst().id());
		assertEquals(100, pets.list(buyer).getFirst().partyPosition());
		var provenance = pets.provenance(buyer, request.creatureId());
		assertFalse(provenance.bound());
		assertEquals("CAPTURE", provenance.originKind());
		assertEquals(characterQueries.find(seller).name(), provenance.captorName());
		assertThrows(BusinessException.class, () -> pets.provenance(seller, request.creatureId()));
		transactions.run(() -> assertEquals("SOLD", listings.get(Long.parseLong(listed.listingId())).getState()));
	}

	@Test
	void petCancellationAndReturnPreserveIdentityAndSharedPendingGate() {
		long owner = ready();
		var request = creatureRequest(owner);
		var listed = market.listCreature(owner, request);
		long listing = Long.parseLong(listed.listingId());
		returns.cancel(owner, new MarketReturnCommandService.Request(UUID.randomUUID().toString(), listing));
		assertTrue(pets.list(owner).stream().noneMatch(p -> p.id().equals(Long.toString(request.creatureId()))));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		var claim = new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), listing, 1);
		var received = returns.claim(owner, claim);
		assertEquals(received, returns.claim(owner, claim));
		var returned = pets.list(owner)
			.stream()
			.filter(p -> p.id().equals(Long.toString(request.creatureId())))
			.findFirst()
			.orElseThrow();
		assertEquals(100, returned.partyPosition());
		market.listItem(owner, request(owner));
	}

	@Test
	void petSearchMatchesExactSkillAndDetailsExposeOriginalIdentityAndDeadResources() {
		long seller = ready(), buyer = ready();
		var request = creatureRequest(seller);
		assertTrue(queries.eligibleCreatures(seller).stream().anyMatch(p -> p.id().equals(request.expected().id())));
		long character = Long.parseLong(characterQueries.find(seller).id());
		transactions.run(() -> combatVitalsStore.saveVitals(character, request.creatureId(), 0, 2));
		var listed = market.listCreature(seller, request);
		assertTrue(queries.eligibleCreatures(seller).isEmpty());
		var filter = new MarketQueryService.CreatureFilter("STONE_GOLEM", true, "碎岩击");
		var page = queries.search(buyer, "", "CREATURE", null, null, 1, 1, "PRICE_ASC", 0, 50, filter);
		assertTrue(page.listings().stream().anyMatch(row -> row.id().equals(listed.listingId())));
		var partial = queries.search(buyer, "", "CREATURE", null, null, null, null, "NEWEST", 0, 50,
				new MarketQueryService.CreatureFilter("STONE_GOLEM", true, "碎岩"));
		assertTrue(partial.listings().isEmpty());
		var normal = queries.search(buyer, "", "CREATURE", null, null, null, null, "NEWEST", 0, 50,
				new MarketQueryService.CreatureFilter("STONE_GOLEM", false, null));
		assertFalse(normal.listings().stream().anyMatch(row -> row.id().equals(listed.listingId())));
		var detail = queries.detail(buyer, Long.parseLong(listed.listingId()));
		assertNull(detail.item());
		assertFalse(detail.own());
		assertEquals(request.expected().name(), detail.creature().name());
		assertEquals("H", detail.creature().abilitySlot());
		assertEquals(request.expected().skills(), detail.creature().skills());
		assertEquals(Long.toString(character), detail.creature().captorId());
		assertEquals(characterQueries.find(seller).name(), detail.creature().captorName());
		assertEquals(0, detail.creature().resources().hp());
		assertEquals(2, detail.creature().resources().mp());
		assertTrue(detail.creature().resources().health() > 0);
		returns.cancel(seller, new MarketReturnCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId())));
		assertThrows(BusinessException.class, () -> queries.detail(buyer, Long.parseLong(listed.listingId())));
		assertTrue(queries.detail(seller, Long.parseLong(listed.listingId())).own());
	}

	@Test
	void concurrentReplayCreatesOneListingAndOneCustodySplit() throws Exception {
		long owner = ready();
		var request = request(owner);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				return market.listItem(owner, request);
			});
			var b = executor.submit(() -> {
				start.await();
				return market.listItem(owner, request);
			});
			start.countDown();
			assertEquals(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(98, items.list(owner).getFirst().quantity());
		transactions.run(() -> {
			assertEquals(1, listings.active(owner));
			var listing = em.createQuery("from MarketListing where sellerAccountId = :owner", MarketListing.class)
				.setParameter("owner", owner)
				.setMaxResults(1)
				.getSingleResult();
			assertEquals(Duration.ofHours(48), Duration.between(listing.getCreatedAt(), listing.getExpiresAt()));
		});
		assertThrows(BusinessException.class, () -> market.listItem(owner,
				new Request(request.requestId(), request.itemId(), 2, 2, request.expected())));
	}

	@Test
	void limitPendingReturnsAndOuterRollbackPreventInventoryLoss() {
		long owner = ready();
		var request = request(owner);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			market.listItem(owner, request);
			throw new IllegalStateException("模拟上架最终失败");
		}));
		assertEquals(99, items.list(owner).getFirst().quantity());
		for (int i = 0; i < 20; i++)
			market.listItem(owner, request(owner));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		assertEquals(79, items.list(owner).getFirst().quantity());
		transactions.run(() -> em.createQuery("from MarketListing where sellerAccountId = :owner", MarketListing.class)
			.setParameter("owner", owner)
			.setMaxResults(1)
			.getSingleResult()
			.setState("RETURN_PENDING"));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		assertEquals(79, items.list(owner).getFirst().quantity());
	}

	@Test
	void concurrentPurchaseReplaysOneDeliveryAndBalancesGoldIncludingTax() throws Exception {
		long seller = ready(), buyer = ready();
		var original = request(seller);
		var listed = market.listItem(seller,
				new Request(original.requestId(), original.itemId(), 5, 7, original.expected()));
		transactions.run(() -> characters.changeGold(buyer, 100));
		var request = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 2, 7);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				return purchases.buy(buyer, request);
			});
			var b = executor.submit(() -> {
				start.await();
				return purchases.buy(buyer, request);
			});
			start.countDown();
			var result = a.get(15, TimeUnit.SECONDS);
			assertEquals(result, b.get(15, TimeUnit.SECONDS));
			assertEquals("14", result.gross());
			assertEquals("1", result.tax());
			assertEquals("13", result.sellerNet());
		}
		assertEquals(86, characterQueries.find(buyer).gold());
		assertEquals(13, characterQueries.find(seller).gold());
		assertEquals(101, items.list(buyer).stream().mapToInt(item -> item.quantity()).sum());
		assertTrue(items.list(buyer).stream().anyMatch(item -> item.source().equals("交易所购入")));
		transactions.run(() -> {
			var listing = listings.get(Long.parseLong(listed.listingId()));
			assertEquals(3, listing.getRemaining());
			assertEquals(listed.expiresAt().toEpochMilli(), listing.getExpiresAt().toEpochMilli());
		});
		assertThrows(BusinessException.class, () -> purchases.buy(seller, request));
		purchases.buy(buyer,
				new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(), request.listingId(), 3, 7));
		assertEquals(65, characterQueries.find(buyer).gold());
		assertEquals(32, characterQueries.find(seller).gold());
		assertEquals(3, 100 - characterQueries.find(buyer).gold() - characterQueries.find(seller).gold());
	}

	@Test
	void fullBagAndOuterFailureRollBackPaymentDeliveryAndRemaining() {
		long seller = ready(), buyer = ready();
		var original = request(seller);
		var listed = market.listItem(seller, original);
		transactions.run(() -> {
			characters.changeGold(buyer, 100);
			for (int i = 0; i < 59; i++)
				inventory.grantStrict(buyer, "GEAR:" + i, "STARTER_WEAPON", 1, true, true);
		});
		var request = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 2);
		assertThrows(BusinessException.class, () -> purchases.buy(buyer, request));
		assertEquals(100, characterQueries.find(buyer).gold());
		assertEquals(0, characterQueries.find(seller).gold());
		assertTrue(items.pending(buyer).isEmpty());
		transactions.run(() -> inventory.take(buyer, "WIND_FEATHER", 1));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			purchases.buy(buyer, request);
			throw new IllegalStateException("模拟成交提交失败");
		}));
		assertEquals(100, characterQueries.find(buyer).gold());
		assertEquals(0, characterQueries.find(seller).gold());
		purchases.buy(buyer, request);
		assertEquals(98, characterQueries.find(buyer).gold());
		assertEquals(1, characterQueries.find(seller).gold());
	}

	/** 超出金币存储上限时买方扣款也必须回滚；随后仍可用同一请求完成正常成交。 */
	@Test
	void sellerBalanceOverflowDoesNotChargeBuyerOrConsumeListing() {
		long seller = ready(), buyer = ready();
		var listed = market.listItem(seller, request(seller));
		transactions.run(() -> {
			characters.changeGold(seller, Long.MAX_VALUE);
			characters.changeGold(buyer, 10);
		});
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 2);
		assertThrows(BusinessException.class, () -> purchases.buy(buyer, buy));
		assertEquals(10, characterQueries.find(buyer).gold());
		assertEquals(Long.MAX_VALUE, characterQueries.find(seller).gold());
		assertEquals(1, queries.detail(buyer, buy.listingId()).listing().remaining());
		transactions.run(() -> characters.changeGold(seller, -1));
		purchases.buy(buyer, buy);
		assertEquals(8, characterQueries.find(buyer).gold());
		assertEquals(Long.MAX_VALUE, characterQueries.find(seller).gold());
	}

	/** 高等级商品允许购入，等级限制必须留在出战／替补操作，不能通过交易降级。 */
	@Test
	void higherLevelPetCanBePurchasedButCannotJoinBattleRoster() {
		long seller = ready(), buyer = ready();
		var initial = creatureRequest(seller);
		transactions.run(() -> {
			em.createNativeQuery("UPDATE owned_creature SET level=20, experience=19000 WHERE id=:id")
				.setParameter("id", initial.creatureId())
				.executeUpdate();
			characters.changeGold(buyer, 1000);
		});
		var actual = pets.list(seller)
			.stream()
			.filter(p -> p.id().equals(Long.toString(initial.creatureId())))
			.findFirst()
			.orElseThrow();
		var listed = market.listCreature(seller, new MarketListingCommandService.CreatureRequest(
				UUID.randomUUID().toString(), initial.creatureId(), 100, actual));
		purchases.buy(buyer, new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 100));
		var received = pets.list(buyer).getFirst();
		assertEquals(20, received.level());
		assertEquals(19000, received.experience());
		assertEquals(100, received.partyPosition());
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='CITY_PETS' WHERE id=:id")
			.setParameter("id", characters.lock(buyer).id())
			.executeUpdate());
		for (int position = 0; position < 4; position++) {
			int target = position;
			assertThrows(BusinessException.class, () -> creatures.position(buyer, initial.creatureId(), target));
		}
		assertEquals(100, pets.list(buyer).getFirst().partyPosition());
	}

	@Test
	void sellerInBattleCanReceivePaymentButBuyerCannotTradeInBattle() {
		long seller = ready(), buyer = ready();
		var listed = market.listItem(seller, request(seller));
		transactions.run(() -> {
			characters.changeGold(buyer, 10);
			characters.setBattle(seller, 810001L, false);
			characters.setBattle(buyer, 810002L, false);
		});
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
				Long.parseLong(listed.listingId()), 1, 2);
		assertThrows(BusinessException.class, () -> purchases.buy(buyer, buy));
		assertEquals(10, characterQueries.find(buyer).gold());
		transactions.run(() -> characters.setBattle(buyer, null, false));
		purchases.buy(buyer, buy);
		assertEquals(1, characterQueries.find(seller).gold());
		assertEquals(810001L, characterQueries.adventure(seller).battle());
		assertEquals(8, characterQueries.find(buyer).gold());
	}

	/** 两名玩家相互购买时锁序相反最容易死锁；两笔真实事务同时启动并限定完成时间。 */
	@Test
	void oppositeDirectionTradesUseConsistentParticipantLockOrder() throws Exception {
		long first = ready(), second = ready();
		var a = market.listItem(first, request(first));
		var b = market.listItem(second, request(second));
		transactions.run(() -> {
			characters.changeGold(first, 10);
			characters.changeGold(second, 10);
		});
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var one = executor.submit(() -> {
				start.await();
				return purchases.buy(second, new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
						Long.parseLong(a.listingId()), 1, 2));
			});
			var two = executor.submit(() -> {
				start.await();
				return purchases.buy(first, new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(),
						Long.parseLong(b.listingId()), 1, 2));
			});
			start.countDown();
			assertEquals("1", one.get(15, TimeUnit.SECONDS).tax());
			assertEquals("1", two.get(15, TimeUnit.SECONDS).tax());
		}
		assertEquals(9, characterQueries.find(first).gold());
		assertEquals(9, characterQueries.find(second).gold());
		assertEquals(99, items.list(first).stream().mapToInt(i -> i.quantity()).sum());
		assertEquals(99, items.list(second).stream().mapToInt(i -> i.quantity()).sum());
	}

	@Test
	void twoBuyersCompetingForLastItemOnlyChargeTheWinner() throws Exception {
		long seller = ready(), first = ready(), second = ready();
		var listing = market.listItem(seller, request(seller));
		long id = Long.parseLong(listing.listingId());
		transactions.run(() -> {
			characters.changeGold(first, 10);
			characters.changeGold(second, 10);
			// 挂单成交不要求卖方离开战斗，也不能更新卖方队伍或资源。
			characters.setBattle(seller, 123L, false);
		});
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				return attempt(first, id);
			});
			var b = executor.submit(() -> {
				start.await();
				return attempt(second, id);
			});
			start.countDown();
			assertEquals(1, a.get(15, TimeUnit.SECONDS) + b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(18, characterQueries.find(first).gold() + characterQueries.find(second).gold());
		assertEquals(1, characterQueries.find(seller).gold());
		assertEquals(199, items.list(first).stream().mapToInt(item -> item.quantity()).sum()
				+ items.list(second).stream().mapToInt(item -> item.quantity()).sum());
		transactions.run(() -> {
			assertEquals("SOLD", listings.get(id).getState());
			assertEquals(0, listings.get(id).getRemaining());
		});
	}

	@Test
	void cancellationAndPartialClaimsKeepNewListingBlockedUntilReturnIsEmpty() {
		long owner = ready(), other = ready();
		var original = request(owner);
		var listed = market.listItem(owner,
				new Request(original.requestId(), original.itemId(), 5, 2, original.expected()));
		long id = Long.parseLong(listed.listingId());
		var cancel = new MarketReturnCommandService.Request(UUID.randomUUID().toString(), id);
		assertThrows(BusinessException.class, () -> returns.cancel(other, cancel));
		assertEquals(returns.cancel(owner, cancel), returns.cancel(owner, cancel));
		transactions.run(() -> assertEquals(0, listings.active(owner)));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		var partial = new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 2);
		var result = returns.claim(owner, partial);
		assertEquals(3, result.remaining());
		assertEquals(result, returns.claim(owner, partial));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		returns.claim(owner, new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 3));
		assertEquals(99, items.list(owner).stream().mapToInt(item -> item.quantity()).sum());
		market.listItem(owner, request(owner));
		assertEquals(0, characterQueries.find(owner).gold());
	}

	@Test
	void expiredListingsRecoverIdempotentlyAndCannotBePurchased() {
		long owner = ready(), buyer = ready();
		var listed = market.listItem(owner, request(owner));
		long id = Long.parseLong(listed.listingId());
		transactions.run(() -> {
			var listing = listings.get(id);
			listing.setCreatedAt(Instant.now().minus(Duration.ofDays(3)));
			listing.setExpiresAt(Instant.now().minusSeconds(1));
			characters.changeGold(buyer, 10);
		});
		assertThrows(BusinessException.class, () -> purchases.buy(buyer,
				new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(), id, 1, 2)));
		assertThrows(BusinessException.class, () -> market.listItem(owner, request(owner)));
		returns.expire(id);
		returns.expire(id);
		transactions.run(() -> assertEquals("RETURN_PENDING", listings.get(id).getState()));
		returns.claim(owner, new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 1));
		returns.expire(id);
		transactions.run(() -> assertEquals("RETURNED", listings.get(id).getState()));
		assertEquals(99, items.list(owner).stream().mapToInt(item -> item.quantity()).sum());
		assertEquals(10, characterQueries.find(buyer).gold());
	}

	@Test
	void purchaseAndCancelRaceCannotBothDeliverTheSameAsset() throws Exception {
		long seller = ready(), buyer = ready();
		long id = Long.parseLong(market.listItem(seller, request(seller)).listingId());
		transactions.run(() -> characters.changeGold(buyer, 10));
		var start = new CountDownLatch(1);
		int bought, cancelled;
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var purchase = executor.submit(() -> {
				start.await();
				return attempt(buyer, id);
			});
			var cancel = executor.submit(() -> {
				start.await();
				try {
					returns.cancel(seller, new MarketReturnCommandService.Request(UUID.randomUUID().toString(), id));
					return 1;
				}
				catch (BusinessException error) {
					assertEquals("挂单已结束", error.getMessage());
					return 0;
				}
			});
			start.countDown();
			bought = purchase.get(15, TimeUnit.SECONDS);
			cancelled = cancel.get(15, TimeUnit.SECONDS);
		}
		assertEquals(1, bought + cancelled);
		if (cancelled == 1)
			returns.claim(seller, new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 1));
		assertEquals(198, items.list(seller).stream().mapToInt(item -> item.quantity()).sum()
				+ items.list(buyer).stream().mapToInt(item -> item.quantity()).sum());
		assertEquals(10 - 2 * bought, characterQueries.find(buyer).gold());
		assertEquals(bought, characterQueries.find(seller).gold());
	}

	@Test
	void fullReturnBagPreservesCustodyUntilSpaceIsAvailable() {
		long owner = ready();
		long id = Long.parseLong(market.listItem(owner, request(owner)).listingId());
		returns.cancel(owner, new MarketReturnCommandService.Request(UUID.randomUUID().toString(), id));
		transactions.run(() -> {
			inventory.grantStrict(owner, "FILL_STACK", "WIND_FEATHER", 1, false, false);
			for (int i = 0; i < 59; i++)
				inventory.grantStrict(owner, "RETURN_FULL:" + i, "STARTER_WEAPON", 1, true, true);
		});
		var claim = new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 1);
		assertThrows(BusinessException.class, () -> returns.claim(owner, claim));
		transactions.run(() -> {
			assertEquals("RETURN_PENDING", listings.get(id).getState());
			assertEquals(1, listings.get(id).getRemaining());
		});
		assertTrue(items.pending(owner).isEmpty());
		transactions.run(() -> inventory.take(owner, "WIND_FEATHER", 1));
		assertEquals(0, returns.claim(owner, claim).remaining());
		assertEquals(99,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("WIND_FEATHER"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void ownPagesAreIsolatedAndExpiredProjectionDoesNotMutateState() {
		long owner = ready(), other = ready();
		market.listItem(other, request(other));
		var first = market.listItem(owner, request(owner));
		market.listItem(owner, request(owner));
		market.listItem(owner, request(owner));
		var page = queries.mine(owner, "ACTIVE", 0, 2);
		assertEquals(2, page.listings().size());
		assertTrue(page.hasMore());
		var second = queries.mine(owner, "ACTIVE", 1, 2);
		assertEquals(1, second.listings().size());
		assertFalse(second.hasMore());
		assertFalse(page.listings()
			.stream()
			.anyMatch(a -> second.listings().stream().anyMatch(b -> a.id().equals(b.id()))));
		long id = Long.parseLong(first.listingId());
		transactions.run(() -> {
			var listing = listings.get(id);
			listing.setCreatedAt(Instant.now().minus(Duration.ofDays(3)));
			listing.setExpiresAt(Instant.now().minusSeconds(1));
		});
		assertEquals(2, queries.access(owner).activeListings());
		assertTrue(queries.access(owner).pendingReturns());
		assertEquals(first.listingId(), queries.mine(owner, "RETURN_PENDING", 0, 20).listings().getFirst().id());
		transactions.run(() -> assertEquals("ACTIVE", listings.get(id).getState(), "查询不写到期状态"));
		assertThrows(BusinessException.class, () -> queries.mine(owner, "ALL", -1, 20));
		assertThrows(BusinessException.class, () -> queries.mine(owner, "ALL", Integer.MAX_VALUE, 50));
		assertThrows(BusinessException.class, () -> queries.mine(owner, "ANY", 0, 20));
	}

	@Test
	void publicSearchFiltersAndSortsInstancesAndShowsActualEquipmentDetail() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> {
			inventory.grantStrict(seller, "SEARCH_GEAR", "STARTER_WEAPON", 1, true, false);
			inventory.grantStrict(seller, "SEARCH_GEAR2", "STARTER_WEAPON", 1, true, false);
		});
		var gear = items.list(seller).stream().filter(item -> item.slot() != null).toList();
		// 专用价格与名字组合避免其他测试挂单干扰，验证同名不同实例不会合并。
		var a = market.listItem(seller,
				new Request(UUID.randomUUID().toString(), Long.parseLong(gear.get(0).id()), 1, 54321, gear.get(0)));
		var b = market.listItem(seller,
				new Request(UUID.randomUUID().toString(), Long.parseLong(gear.get(1).id()), 1, 54322, gear.get(1)));
		var results = queries.search(buyer, gear.getFirst().name(), "EQUIPMENT", "WEAPON", "普通", 1, 1, "PRICE_DESC", 0,
				50);
		assertEquals(b.listingId(), results.listings().get(0).id());
		assertEquals(a.listingId(), results.listings().get(1).id());
		assertTrue(
				queries.search(buyer, "不存在的商品", "ALL", null, null, null, null, "NEWEST", 0, 20).listings().isEmpty());
		assertTrue(queries.search(buyer, "%", "ALL", null, null, null, null, "NEWEST", 0, 20).listings().isEmpty(),
				"名称不是SQL通配模式");
		var detail = queries.detail(buyer, Long.parseLong(a.listingId()));
		assertFalse(detail.own());
		assertEquals(gear.getFirst().stats(), detail.item().stats());
		assertEquals("WEAPON", detail.item().slot());
		assertEquals(1, detail.item().quantity());
		assertFalse(detail.item().bound());
		returns.cancel(seller,
				new MarketReturnCommandService.Request(UUID.randomUUID().toString(), Long.parseLong(a.listingId())));
		assertThrows(BusinessException.class, () -> queries.detail(buyer, Long.parseLong(a.listingId())));
		assertTrue(queries.detail(seller, Long.parseLong(a.listingId())).own());
		assertThrows(BusinessException.class,
				() -> queries.search(buyer, "", "ALL", null, null, 9, 1, "NEWEST", 0, 20));
		assertThrows(BusinessException.class,
				() -> queries.search(buyer, "", "ALL", null, null, null, null, "INVALID", 0, 20));
	}

	@Test
	void craftedWeaponAndArmorDetailsBothExposeMatchingSetBonus() {
		long seller = ready(), buyer = ready();
		for (String code : List.of("CRAFT_6_SWORD", "CRAFT_6_BODY")) {
			transactions.run(() -> inventory.grantStrict(seller, "SET:" + code, code, 1, true, false));
			var item = items.list(seller).stream().filter(owned -> owned.code().equals(code)).findFirst().orElseThrow();
			var listed = market.listItem(seller,
					new Request(UUID.randomUUID().toString(), Long.parseLong(item.id()), 1, 100, item));
			var detail = queries.detail(buyer, Long.parseLong(listed.listingId()));
			assertEquals(1, detail.sets().size());
			var set = detail.sets().getFirst();
			assertEquals("CRAFT_SET_6", set.id());
			assertTrue(set.weapons().contains("CRAFT_6_SWORD"));
			assertTrue(set.pieces().contains("CRAFT_6_BODY"));
			assertTrue(set.weaponHealth() > 0);
			assertTrue(set.weaponShield() > 0);
		}
	}

	private int attempt(long buyer, long listing) {
		try {
			purchases.buy(buyer, new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(), listing, 1, 2));
			return 1;
		}
		catch (BusinessException error) {
			assertTrue(error.getMessage().contains("挂单已结束"));
			return 0;
		}
	}

	@Test
	void materialBatchSupportsHundredsOfSourcesPartialPurchaseAndReturnWithoutRegrant() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> {
			for (int i = 0; i < 288; i++)
				inventory.grantStrict(seller, "BATCH:" + i, "LIFE_ORE_1", 1, false, false);
			characters.changeGold(buyer, 1000);
		});
		var selections = items.list(seller)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_1"))
			.map(item -> new Selection(Long.parseLong(item.id()), 1, item))
			.toList();
		var request = new BatchRequest(UUID.randomUUID().toString(), selections, 2);
		var result = market.listBatch(seller, request);
		assertEquals(result, market.listBatch(seller, request));
		long id = Long.parseLong(result.listingId());
		assertEquals(288, queries.detail(buyer, id).item().quantity());
		transactions.run(() -> assertEquals(1, listings.active(seller)));
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(), id, 150, 2);
		assertEquals(purchases.buy(buyer, buy), purchases.buy(buyer, buy));
		assertEquals(700, characterQueries.find(buyer).gold());
		assertEquals(285, characterQueries.find(seller).gold());
		assertEquals(138, queries.detail(buyer, id).item().quantity());
		returns.cancel(seller, new MarketReturnCommandService.Request(UUID.randomUUID().toString(), id));
		var claim = new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 100);
		assertEquals(returns.claim(seller, claim), returns.claim(seller, claim));
		assertEquals(38, queries.detail(seller, id).item().quantity());
		returns.claim(seller, new MarketReturnCommandService.Claim(UUID.randomUUID().toString(), id, 38));
		transactions.run(() -> {
			for (int i = 0; i < 288; i++)
				inventory.grantStrict(seller, "BATCH:" + i, "LIFE_ORE_1", 1, false, false);
			assertEquals("RETURNED", listings.get(id).getState());
			assertTrue(listings.get(id).getItemAssets().isEmpty());
		});
		assertEquals(138,
				items.list(seller)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_1"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertEquals(150,
				items.list(buyer)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_1"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void batchRejectsDuplicatesDifferentMaterialsAndChangedLastSourceAtomically() {
		long seller = ready();
		transactions.run(() -> {
			inventory.grantStrict(seller, "BATCH_A", "LIFE_ORE_1", 2, false, false);
			inventory.grantStrict(seller, "BATCH_B", "LIFE_ORE_1", 2, false, false);
		});
		var ore = items.list(seller)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_1"))
			.map(item -> new Selection(Long.parseLong(item.id()), 2, item))
			.toList();
		assertThrows(BusinessException.class, () -> market.listBatch(seller,
				new BatchRequest(UUID.randomUUID().toString(), List.of(ore.getFirst(), ore.getFirst()), 2)));
		var other = items.list(seller)
			.stream()
			.filter(item -> item.code().equals("WIND_FEATHER"))
			.findFirst()
			.orElseThrow();
		assertThrows(BusinessException.class,
				() -> market.listBatch(seller, new BatchRequest(UUID.randomUUID().toString(),
						List.of(ore.getFirst(), new Selection(Long.parseLong(other.id()), 1, other)), 2)));
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET quantity=1 WHERE id=:id")
			.setParameter("id", ore.getLast().itemId())
			.executeUpdate());
		assertThrows(BusinessException.class,
				() -> market.listBatch(seller, new BatchRequest(UUID.randomUUID().toString(), ore, 2)));
		assertEquals(3,
				items.list(seller)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_1"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> assertEquals(0, listings.active(seller)));
	}

	@Test
	void fullBagRollsBackEarlierBatchesAndPaymentThenSameRequestCanRetry() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> {
			inventory.grantStrict(seller, "BATCH_A", "LIFE_ORE_1", 1, false, false);
			inventory.grantStrict(seller, "BATCH_B", "LIFE_ORE_1", 1, false, false);
			inventory.grantStrict(buyer, "NEAR_FULL", "LIFE_ORE_1", 98, false, false);
			for (int i = 0; i < 58; i++)
				inventory.grantStrict(buyer, "FULL:" + i, "STARTER_WEAPON", 1, true, true);
			characters.changeGold(buyer, 10);
		});
		var selections = items.list(seller)
			.stream()
			.filter(item -> item.code().equals("LIFE_ORE_1"))
			.map(item -> new Selection(Long.parseLong(item.id()), 1, item))
			.toList();
		long id = Long.parseLong(
				market.listBatch(seller, new BatchRequest(UUID.randomUUID().toString(), selections, 2)).listingId());
		var buy = new MarketPurchaseCommandService.Request(UUID.randomUUID().toString(), id, 2, 2);
		assertThrows(BusinessException.class, () -> purchases.buy(buyer, buy));
		assertEquals(10, characterQueries.find(buyer).gold());
		assertEquals(0, characterQueries.find(seller).gold());
		assertEquals(2, queries.detail(buyer, id).item().quantity());
		assertEquals(98,
				items.list(buyer)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_1"))
					.mapToInt(item -> item.quantity())
					.sum());
		transactions.run(() -> inventory.take(buyer, "LIFE_ORE_1", 1));
		purchases.buy(buyer, buy);
		assertEquals(6, characterQueries.find(buyer).gold());
		assertEquals(99,
				items.list(buyer)
					.stream()
					.filter(item -> item.code().equals("LIFE_ORE_1"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

}
