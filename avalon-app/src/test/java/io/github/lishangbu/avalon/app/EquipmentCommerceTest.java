package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.command.CharacterFactCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.EquipmentCommerceService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.EquipmentCommerceRequest;
import io.github.lishangbu.avalon.inventory.application.query.EquipmentShopQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库验证装备来源、金币守恒、空间检查、确认信息和并发防重。 */
class EquipmentCommerceTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactCommandService facts;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	EquipmentShopQueryService shops;

	@Autowired
	EquipmentCommerceService commerce;

	@Autowired
	EntityManager em;

	private long ready() {
		String name = "trade_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Commerce-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, 100);
			facts.record(owner, "QUEST:BQ004");
			em.createNativeQuery("UPDATE player_character SET location = 'WEAPON_SHOP' WHERE account_id = :owner")
				.setParameter("owner", owner)
				.executeUpdate();
		});
		return owner;
	}

	private EquipmentCommerceRequest buy(String code) {
		return new EquipmentCommerceRequest(UUID.randomUUID().toString(), "BUY", code, null);
	}

	@Test
	void purchasedEquipmentRetainsResaleAfterBindingButGiftsCannotBeSold() {
		long owner = ready();
		assertEquals(3, shops.view(owner).products().size());
		assertThrows(BusinessException.class, () -> commerce.execute(owner, buy("CAVE_WEAPON")));
		var request = buy("STARTER_BODY");
		commerce.execute(owner, request);
		commerce.execute(owner, request);
		assertEquals(80, characterQueries.find(owner).gold());
		var bought = items.list(owner).getFirst();
		assertEquals(4, bought.sellPrice());
		assertFalse(bought.bound());
		inventory.equip(owner, Long.parseLong(bought.id()));
		var equipped = items.list(owner).getFirst();
		assertTrue(equipped.bound());
		assertThrows(BusinessException.class, () -> commerce.execute(owner,
				new EquipmentCommerceRequest(UUID.randomUUID().toString(), "SELL", null, equipped)));
		inventory.unequip(owner, Long.parseLong(bought.id()));
		assertThrows(BusinessException.class,
				() -> commerce.execute(owner,
						new EquipmentCommerceRequest(UUID.randomUUID().toString(), "SELL", null, bought)),
				"绑定状态发生变化必须重新确认");
		var sell = new EquipmentCommerceRequest(UUID.randomUUID().toString(), "SELL", null,
				items.list(owner).getFirst());
		commerce.execute(owner, sell);
		commerce.execute(owner, sell);
		assertEquals(84, characterQueries.find(owner).gold());
		assertTrue(items.list(owner).isEmpty());
		transactions.run(() -> inventory.grant(owner, "GIFT", "STARTER_BODY", 1, true));
		var gift = items.list(owner).getFirst();
		assertEquals(0, gift.sellPrice());
		assertThrows(BusinessException.class, () -> commerce.execute(owner,
				new EquipmentCommerceRequest(UUID.randomUUID().toString(), "SELL", null, gift)));
		var destroy = new EquipmentCommerceRequest(UUID.randomUUID().toString(), "DESTROY", null, gift);
		commerce.execute(owner, destroy);
		commerce.execute(owner, destroy);
		transactions.run(() -> inventory.grant(owner, "GIFT", "STARTER_BODY", 1, true));
		assertTrue(items.list(owner).isEmpty());
		assertEquals(84, characterQueries.find(owner).gold());
	}

	@Test
	void capacityFundsOwnershipAndRollbackRejectWithoutConsumingResources() {
		long owner = ready();
		transactions.run(() -> {
			for (int i = 0; i < 60; i++)
				inventory.grant(owner, "FULL:" + i, "STARTER_BODY", 1, true);
		});
		assertThrows(BusinessException.class, () -> commerce.execute(owner, buy("STARTER_BODY")));
		assertEquals(100, characterQueries.find(owner).gold());
		assertTrue(items.pending(owner).isEmpty());
		long other = ready();
		var gift = items.list(owner).getFirst();
		var destroy = new EquipmentCommerceRequest(UUID.randomUUID().toString(), "DESTROY", null, gift);
		assertThrows(BusinessException.class, () -> commerce.execute(other, destroy));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commerce.execute(owner, destroy);
			throw new IllegalStateException("模拟外层提交失败");
		}));
		assertEquals(60, items.list(owner).size());
		transactions.run(() -> characters.changeGold(other, -100));
		assertThrows(BusinessException.class, () -> commerce.execute(other, buy("STARTER_BODY")));
		assertEquals(0, characterQueries.find(other).gold());
		assertTrue(items.list(other).isEmpty());
	}

	@Test
	void concurrentPurchaseCreditsOnceAndReusedRequestCannotChangeOperation() throws Exception {
		long owner = ready();
		var request = buy("STARTER_WEAPON");
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				start.await();
				commerce.execute(owner, request);
				return true;
			});
			var second = executor.submit(() -> {
				start.await();
				commerce.execute(owner, request);
				return true;
			});
			start.countDown();
			assertTrue(first.get(15, TimeUnit.SECONDS));
			assertTrue(second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(80, characterQueries.find(owner).gold());
		assertEquals(1, items.list(owner).size());
		assertThrows(BusinessException.class, () -> commerce.execute(owner,
				new EquipmentCommerceRequest(request.requestId(), "BUY", "STARTER_BODY", null)));
		transactions.run(() -> characters.setBattle(owner, 123L, false));
		assertThrows(BusinessException.class, () -> commerce.execute(owner, buy("STARTER_BODY")));
		assertFalse(shops.view(owner).available());
	}

}
