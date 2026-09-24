package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.CraftingCommandService;
import io.github.lishangbu.avalon.inventory.application.command.CraftingCommandService.Request;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.inventory.application.query.CraftingQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import io.github.lishangbu.avalon.inventory.application.model.CraftingResult;

import static org.junit.jupiter.api.Assertions.*;

/** 真实数据库验证绑定选择、净空间及不足资源时不产生部分消费。 */
class CraftingFlowTest extends SpringIntegrationTest {

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
	InventoryQueryService inventoryQueries;

	@Autowired
	CraftingCommandService commands;

	@Autowired
	CraftingQueryService queries;

	private long ready(int fragments, int equipment, int gold) {
		String name = "craft_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
		registrations.register(name, "Craft-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, gold);
			inventory.grantWithBinding(owner, "FRAGMENTS", "FRAGMENT_WATER", fragments, false, false);
			for (int i = 0; i < equipment; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_WEAPON", 1, true);
		});
		return owner;
	}

	private String fragment(long owner, boolean bound) {
		return inventoryQueries.list(owner)
			.stream()
			.filter(i -> i.code().equals("FRAGMENT_WATER") && i.bound() == bound)
			.findFirst()
			.orElseThrow()
			.id();
	}

	@Test
	void consumedLastStackFreesSlotAndRetryCannotDuplicateBook() {
		long owner = ready(5, 59, 20);
		assertEquals(60, queries.preview(owner).usedSlots());
		var request = new Request(UUID.randomUUID().toString(), "WATER_BOOK", Map.of(fragment(owner, false), 5));
		var result = commands.craft(owner, request);
		assertFalse(result.bound());
		assertEquals(result, commands.craft(owner, request));
		assertEquals(10, characterQueries.find(owner).gold());
		assertEquals(60, queries.preview(owner).usedSlots());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(i -> i.code().equals("BOOK_WATER"))
					.mapToInt(i -> i.quantity())
					.sum());
		assertTrue(queries.preview(owner).recipes().isEmpty());
	}

	@Test
	void fullBagAndInsufficientGoldLeaveAllInputsIntact() {
		long owner = ready(6, 59, 20);
		var request = new Request(UUID.randomUUID().toString(), "WATER_BOOK", Map.of(fragment(owner, false), 5));
		assertThrows(BusinessException.class, () -> commands.craft(owner, request));
		assertEquals(20, characterQueries.find(owner).gold());
		assertEquals(6,
				inventoryQueries.list(owner)
					.stream()
					.filter(i -> i.code().equals("FRAGMENT_WATER"))
					.findFirst()
					.orElseThrow()
					.quantity());
		long poor = ready(5, 0, 9);
		var poorRequest = new Request(UUID.randomUUID().toString(), "WATER_BOOK", Map.of(fragment(poor, false), 5));
		assertThrows(BusinessException.class, () -> commands.craft(poor, poorRequest));
		assertEquals(9, characterQueries.find(poor).gold());
		assertEquals(5, queries.preview(poor).recipes().getFirst().fragments().getFirst().quantity());
		assertThrows(BusinessException.class, () -> commands.craft(poor, request));
	}

	@Test
	void concurrentRequestsCommitOnceAndTransactionFailureRestoresInputs() throws Exception {
		long owner = ready(5, 0, 20);
		var request = new Request(UUID.randomUUID().toString(), "WATER_BOOK", Map.of(fragment(owner, false), 5));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			commands.craft(owner, request);
			throw new IllegalStateException("模拟提交前失败");
		}));
		assertEquals(20, characterQueries.find(owner).gold());
		assertEquals(5, queries.preview(owner).recipes().getFirst().fragments().getFirst().quantity());
		var start = new CountDownLatch(1);
		Callable<CraftingResult> action = () -> {
			assertTrue(start.await(10, TimeUnit.SECONDS));
			return commands.craft(owner, request);
		};
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(action);
			var second = executor.submit(action);
			start.countDown();
			assertEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(10, characterQueries.find(owner).gold());
		assertEquals(1,
				inventoryQueries.list(owner)
					.stream()
					.filter(i -> i.code().equals("BOOK_WATER"))
					.mapToInt(i -> i.quantity())
					.sum());
	}

	@Test
	void explicitlyMixedBindingProducesBoundBookAndRejectsChangedPayload() {
		long owner = ready(5, 0, 30);
		transactions.run(() -> inventory.grant(owner, "BOUND", "FRAGMENT_WATER", 3, false));
		String loose = fragment(owner, false);
		String bound = fragment(owner, true);
		var request = new Request(UUID.randomUUID().toString(), "WATER_BOOK", Map.of(loose, 2, bound, 3));
		assertTrue(commands.craft(owner, request).bound());
		assertEquals(3, queries.preview(owner).recipes().getFirst().fragments().getFirst().quantity());
		assertThrows(BusinessException.class,
				() -> commands.craft(owner, new Request(request.requestId(), "WATER_BOOK", Map.of(loose, 5))));
		assertEquals(20, characterQueries.find(owner).gold());
	}

}
