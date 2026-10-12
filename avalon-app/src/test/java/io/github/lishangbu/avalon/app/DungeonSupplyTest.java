package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.DungeonSupplyCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryClaimService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryClaimRequest;
import io.github.lishangbu.avalon.inventory.application.query.DungeonSupplyQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 真实 PostgreSQL 验证物资登记、来源隔离、待领、并发和外层回滚。尚不代表正式开本门禁。 */
class DungeonSupplyTest extends SpringIntegrationTest {

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
	InventoryClaimService claims;

	@Autowired
	DungeonSupplyCommandService supplies;

	@Autowired
	DungeonSupplyQueryService queries;

	private long owner() {
		String name = "supply_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Supply-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private void tx(Runnable work) {
		transactions.run(work);
	}

	private int count(long owner) {
		return queries.available(100, owner).getOrDefault("HEALTH_POTION", 0);
	}

	@Test
	void entryIsIdempotentAndNeverIncludesLaterExternalSourcesOrOtherPlayers() {
		long owner = owner();
		tx(() -> {
			inventory.grant(owner, "INITIAL", "HEALTH_POTION", 2, false);
			supplies.enter(100, owner);
			inventory.grant(owner, "EXTERNAL", "HEALTH_POTION", 5, false);
			supplies.enter(100, owner);
		});
		assertEquals(2, count(owner));
		long stranger = owner();
		assertEquals(0, count(stranger));
		assertThrows(BusinessException.class, () -> tx(() -> supplies.take(100, stranger, "HEALTH_POTION", 1)));
		assertThrows(BusinessException.class, () -> tx(() -> supplies.take(100, owner, "HEALTH_POTION", 3)));
		assertEquals(2, count(owner));
		tx(() -> supplies.take(100, owner, "HEALTH_POTION", 2));
		assertEquals(0, count(owner));
		assertEquals(5, items.list(owner).getFirst().quantity());
		tx(() -> supplies.enter(100, owner));
		assertEquals(0, count(owner));
		assertThrows(BusinessException.class, () -> tx(() -> supplies.enter(101, owner)));
		tx(() -> supplies.close(100, owner));
		assertThrows(BusinessException.class, () -> tx(() -> supplies.enter(100, owner)));
		tx(() -> supplies.enter(101, owner));
		assertEquals(5, queries.available(101, owner).get("HEALTH_POTION"));
	}

	@Test
	void pendingExternalPotionsStayExcludedWhileClaimedDungeonRewardsBecomeAvailable() {
		long owner = owner();
		tx(() -> {
			for (int i = 0; i < 59; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_BODY", 1, true);
			inventory.grant(owner, "INITIAL", "HEALTH_POTION", 98, false);
			inventory.grant(owner, "EXTERNAL", "HEALTH_POTION", 3, false);
			supplies.enter(100, owner);
			supplies.reward(100, owner, "STAGE1:0", "HEALTH_POTION", 4);
		});
		assertEquals(99, count(owner));
		var pending = items.pending(owner);
		var external = pending.stream().filter(item -> item.quantity() == 2).findFirst().orElseThrow();
		var internal = pending.stream().filter(item -> item.quantity() == 4).findFirst().orElseThrow();
		assertThrows(BusinessException.class, () -> tx(() -> supplies.take(100, owner, "HEALTH_POTION", 100)));
		tx(() -> supplies.take(100, owner, "HEALTH_POTION", 99));
		claims.claim(owner, Long.parseLong(external.id()), new InventoryClaimRequest(UUID.randomUUID().toString(), 2));
		assertEquals(0, count(owner));
		var claim = new InventoryClaimRequest(UUID.randomUUID().toString(), 3);
		claims.claim(owner, Long.parseLong(internal.id()), claim);
		claims.claim(owner, Long.parseLong(internal.id()), claim);
		assertEquals(3, count(owner));
		tx(() -> {
			supplies.take(100, owner, "HEALTH_POTION", 3);
			supplies.reward(100, owner, "STAGE1:0", "HEALTH_POTION", 4);
		});
		assertEquals(0, count(owner));
		claims.claim(owner, Long.parseLong(internal.id()), new InventoryClaimRequest(UUID.randomUUID().toString(), 1));
		assertEquals(1, count(owner));
		tx(() -> supplies.take(100, owner, "HEALTH_POTION", 1));
		assertEquals(2,
				items.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
	}

	@Test
	void registrationRewardAndConsumptionRollbackTogetherWithOuterUseCase() {
		long owner = owner();
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			supplies.enter(100, owner);
			supplies.reward(100, owner, "STAGE1:0", "HEALTH_POTION", 2);
			throw new IllegalStateException("模拟开本事务回滚");
		}));
		assertEquals(0, count(owner));
		assertTrue(items.list(owner).isEmpty());
		tx(() -> {
			supplies.enter(100, owner);
			supplies.reward(100, owner, "STAGE1:0", "HEALTH_POTION", 2);
		});
		assertThrows(IllegalStateException.class, () -> tx(() -> {
			supplies.take(100, owner, "HEALTH_POTION", 1);
			supplies.close(100, owner);
			throw new IllegalStateException("模拟回合事务回滚");
		}));
		assertEquals(2, count(owner));
		assertEquals(2, items.list(owner).getFirst().quantity());
	}

	@Test
	void concurrentConsumptionCannotSpendOnePotionTwiceAndEmptyEntryStaysEmpty() throws Exception {
		long owner = owner();
		tx(() -> supplies.enter(100, owner));
		tx(() -> inventory.grant(owner, "EXTERNAL", "HEALTH_POTION", 2, false));
		tx(() -> supplies.enter(100, owner));
		assertEquals(0, count(owner));
		tx(() -> supplies.reward(100, owner, "STAGE1:0", "HEALTH_POTION", 1));
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Callable<Boolean> task = () -> {
				start.await();
				try {
					tx(() -> supplies.take(100, owner, "HEALTH_POTION", 1));
					return true;
				}
				catch (BusinessException error) {
					return false;
				}
			};
			var first = executor.submit(task);
			var second = executor.submit(task);
			start.countDown();
			assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(0, count(owner));
		assertEquals(2, items.list(owner).getFirst().quantity());
	}

}
