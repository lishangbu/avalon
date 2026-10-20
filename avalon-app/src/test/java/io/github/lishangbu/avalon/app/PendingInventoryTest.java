package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryClaimService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryClaimRequest;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

/** 满包固定奖励仍保存，主动消费则失败；领取和消耗都只能使用真实可用数量。 */
class PendingInventoryTest extends SpringIntegrationTest {

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
	InventoryQueryService queries;

	@Autowired
	InventoryClaimService claims;

	private long ready() {
		String name = "bag_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		registrations.register(name, "Pending-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, 100);
			for (int i = 0; i < 59; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_WEAPON", 1, true);
			inventory.grant(owner, "POTIONS", "HEALTH_POTION", 98, false);
		});
		return owner;
	}

	@Test
	void rewardsOverflowWithoutLossAndPendingCannotBeUsed() {
		long owner = ready();
		transactions.run(() -> {
			inventory.grant(owner, "REWARD", "HEALTH_POTION", 10, false);
			inventory.grant(owner, "REWARD", "HEALTH_POTION", 10, false);
			inventory.grant(owner, "QUEST", "OPENING_MINE_REPORT", 1, false);
			inventory.grant(owner, "WEAPON", "CAVE_WEAPON", 1, true);
		});
		assertEquals(99,
				queries.list(owner)
					.stream()
					.filter(item -> item.code().equals("HEALTH_POTION"))
					.mapToInt(item -> item.quantity())
					.sum());
		assertTrue(queries.list(owner).stream().anyMatch(item -> item.code().equals("OPENING_MINE_REPORT")));
		var potion = queries.pending(owner)
			.stream()
			.filter(item -> item.name().equals("生命药"))
			.findFirst()
			.orElseThrow();
		assertEquals(9, potion.quantity());
		var weapon = queries.pending(owner)
			.stream()
			.filter(item -> item.name().equals("精铁剑"))
			.findFirst()
			.orElseThrow();
		assertThrows(BusinessException.class, () -> inventory.equip(owner, Long.parseLong(weapon.id())));
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> inventory.take(owner, "HEALTH_POTION", 100)));
		assertEquals(9,
				queries.pending(owner)
					.stream()
					.filter(item -> item.id().equals(potion.id()))
					.findFirst()
					.orElseThrow()
					.quantity());
		assertThrows(BusinessException.class, () -> transactions.run(() -> {
			characters.changeGold(owner, -12);
			inventory.grantStrict(owner, "BUY", "HEALTH_POTION", 1, false, true);
		}));
		assertEquals(100, characterQueries.find(owner).gold());
		assertEquals(2, queries.pending(owner).size());
	}

	@Test
	void partialClaimIsAtomicIdempotentAndSerialized() throws Exception {
		long owner = ready();
		transactions.run(() -> inventory.grant(owner, "REWARD", "HEALTH_POTION", 10, false));
		var pending = queries.pending(owner).getFirst();
		long item = Long.parseLong(pending.id());
		var request = new InventoryClaimRequest(UUID.randomUUID().toString(), 3);
		assertThrows(BusinessException.class, () -> claims.claim(owner, item, request));
		transactions.run(() -> inventory.take(owner, "HEALTH_POTION", 3));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			claims.claim(owner, item, request);
			throw new IllegalStateException("模拟领取后事务失败");
		}));
		assertEquals(9, queries.pending(owner).getFirst().quantity());
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				start.await();
				claims.claim(owner, item, request);
				return true;
			});
			var second = executor.submit(() -> {
				start.await();
				claims.claim(owner, item, request);
				return true;
			});
			start.countDown();
			assertTrue(first.get(15, TimeUnit.SECONDS));
			assertTrue(second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(6, queries.pending(owner).getFirst().quantity());
		assertThrows(BusinessException.class,
				() -> claims.claim(owner, item, new InventoryClaimRequest(request.requestId(), 1)));
		long outsider = ready();
		assertThrows(BusinessException.class,
				() -> claims.claim(outsider, item, new InventoryClaimRequest(UUID.randomUUID().toString(), 1)));
		transactions.run(() -> {
			inventory.take(owner, "HEALTH_POTION", 1);
			characters.setBattle(owner, 123L, false);
		});
		assertThrows(BusinessException.class,
				() -> claims.claim(owner, item, new InventoryClaimRequest(UUID.randomUUID().toString(), 1)));
		assertEquals(6, queries.pending(owner).getFirst().quantity());
	}

}
