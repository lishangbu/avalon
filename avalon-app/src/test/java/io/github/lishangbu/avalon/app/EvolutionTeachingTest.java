package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterFactQueryService;
import io.github.lishangbu.avalon.content.battle.GrowthRules;
import io.github.lishangbu.avalon.creature.application.command.EvolutionTeachingCommandService;
import io.github.lishangbu.avalon.creature.application.command.ResearchExchangeCommandService;
import io.github.lishangbu.avalon.creature.application.model.EvolutionTeachingRequest;
import io.github.lishangbu.avalon.creature.application.model.ResearchExchangeRequest;
import io.github.lishangbu.avalon.creature.application.query.EvolutionResearchQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
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

/** 使用正式经验曲线验证11级研究教学；夹具只准备经验及地点，不替换成长规则。 */
class EvolutionTeachingTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterFactQueryService facts;

	@Autowired
	EvolutionTeachingCommandService teaching;

	@Autowired
	EvolutionResearchQueryService research;

	@Autowired
	InventoryQueryService inventory;

	@Autowired
	EntityManager em;

	@Autowired
	ResearchExchangeCommandService exchange;

	@Autowired
	InventoryCommandService inventoryCommands;

	@Autowired
	GrowthRules growth;

	private long ready() {
		String name = "teach_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Teaching-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(11))));
		assertEquals(11, transactions.call(() -> characters.lock(owner).level()));
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location = 'CITY_PETS' WHERE id = :id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		return owner;
	}

	@Test
	void safeRecoveryPlaceAllowsEvolutionButNotTeachingOrTokenExchange() {
		long owner = ready();
		teaching.learn(owner, new EvolutionTeachingRequest("CLAW_CAT", true));
		transactions.run(() -> inventoryCommands.grantStrict(owner, "TOKENS", "RESEARCH_TOKEN", 12, false, true));
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='PET_CENTER' WHERE id=:id")
			.setParameter("id", characters.lock(owner).id())
			.executeUpdate());
		var view = research.view(owner);
		assertTrue(view.available());
		assertTrue(view.taught());
		assertTrue(view.offers().isEmpty());
		assertFalse(view.routes().isEmpty());
		assertThrows(BusinessException.class, () -> exchange.exchange(owner,
				new ResearchExchangeRequest(UUID.randomUUID().toString(), "WATER_PEARL", 1, 1, true)));
		assertEquals(12, research.view(owner).tokens());
		long untaught = ready();
		transactions.run(() -> em.createNativeQuery("UPDATE player_character SET location='PET_CENTER' WHERE id=:id")
			.setParameter("id", characters.lock(untaught).id())
			.executeUpdate());
		assertFalse(research.view(untaught).available());
		assertThrows(BusinessException.class,
				() -> teaching.learn(untaught, new EvolutionTeachingRequest("CLAW_CAT", true)));
	}

	@Test
	void teachingOpensAtElevenAndRejectsTenWithoutGrantingMaterials() {
		long owner = ready();
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET experience = :experience WHERE id = :id")
				.setParameter("experience", growth.threshold(10))
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate());
		var request = new EvolutionTeachingRequest("GALE_SPARROW", true);
		assertFalse(research.view(owner).available());
		assertThrows(BusinessException.class, () -> teaching.learn(owner, request));
		assertTrue(inventory.list(owner).isEmpty());
		assertFalse(facts.codes(owner).contains("EVOLUTION:TUTORIAL"));
		transactions
			.run(() -> characters.grantExperience(owner, Math.toIntExact(growth.threshold(11) - growth.threshold(10))));
		assertTrue(research.view(owner).available());
		teaching.learn(owner, request);
		assertTrue(facts.codes(owner).contains("EVOLUTION:TUTORIAL"));
	}

	@Test
	void exchangeIsBoundAndConcurrentRetryChargesTokensOnce() throws Exception {
		long owner = ready();
		teaching.learn(owner, new EvolutionTeachingRequest("GALE_SPARROW", true));
		transactions.run(() -> inventoryCommands.grantStrict(owner, "TOKENS", "RESEARCH_TOKEN", 24, false, true));
		var request = new ResearchExchangeRequest(UUID.randomUUID().toString(), "BOOK_FIRESTORM", 1, 12, true);
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var a = executor.submit(() -> {
				start.await();
				exchange.exchange(owner, request);
				return true;
			});
			var b = executor.submit(() -> {
				start.await();
				exchange.exchange(owner, request);
				return true;
			});
			start.countDown();
			assertTrue(a.get(15, TimeUnit.SECONDS));
			assertTrue(b.get(15, TimeUnit.SECONDS));
		}
		assertEquals(12, research.view(owner).tokens());
		var book = inventory.list(owner)
			.stream()
			.filter(item -> item.code().equals("BOOK_FIRESTORM"))
			.findFirst()
			.orElseThrow();
		assertEquals(1, book.quantity());
		assertTrue(book.bound());
		assertThrows(BusinessException.class, () -> exchange.exchange(owner,
				new ResearchExchangeRequest(request.requestId(), "BOOK_GALE_BLADE", 1, 12, true)));
		assertThrows(BusinessException.class, () -> exchange.exchange(owner,
				new ResearchExchangeRequest(UUID.randomUUID().toString(), "BOOK_GALE_BLADE", 1, 1, true)));
		assertThrows(BusinessException.class,
				() -> exchange.exchange(owner, new ResearchExchangeRequest(UUID.randomUUID().toString(),
						"BOOK_GALE_BLADE", Integer.MAX_VALUE, 12, true)));
		assertEquals(12, research.view(owner).tokens());
	}

	@Test
	void exchangeCapacityFailureAndOuterRollbackRestoreAllTokens() {
		long owner = ready();
		teaching.learn(owner, new EvolutionTeachingRequest("GALE_SPARROW", true));
		transactions.run(() -> inventoryCommands.grantStrict(owner, "TOKENS", "RESEARCH_TOKEN", 99, false, true));
		var request = new ResearchExchangeRequest(UUID.randomUUID().toString(), "BOOK_GALE_BLADE", 1, 12, true);
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			exchange.exchange(owner, request);
			throw new IllegalStateException("模拟兑换最后一步失败");
		}));
		assertEquals(99, research.view(owner).tokens());
		transactions.run(() -> {
			for (int i = 0; i < 57; i++)
				inventoryCommands.grantStrict(owner, "FULL:" + i, "STARTER_WEAPON", 1, true, true);
		});
		assertThrows(BusinessException.class, () -> exchange.exchange(owner, request));
		assertEquals(99, research.view(owner).tokens());
		assertFalse(inventory.list(owner).stream().anyMatch(item -> item.code().equals("BOOK_GALE_BLADE")));
		assertTrue(inventory.pending(owner).isEmpty());
	}

	@Test
	void selectedRouteGrantsOneBoundSetAndCannotBeChangedAfterReceipt() {
		long owner = ready();
		assertTrue(research.view(owner).available());
		// 当前运行目录包含新增低级家族，共二十条相邻进化路线。
		assertEquals(20, research.view(owner).routes().size());
		var request = new EvolutionTeachingRequest("GALE_SPARROW", true);
		teaching.learn(owner, request);
		teaching.learn(owner, request);
		var items = inventory.list(owner);
		assertEquals(2, items.size());
		assertTrue(items.stream().allMatch(item -> item.bound() && item.quantity() == 1));
		assertTrue(items.stream().anyMatch(item -> item.code().equals("WIND_FEATHER")));
		assertTrue(items.stream().anyMatch(item -> item.code().equals("GALE_PLUME")));
		assertTrue(facts.codes(owner).contains("EVOLUTION:TUTORIAL"));
		assertEquals("GALE_SPARROW", research.view(owner).selected());
		assertThrows(BusinessException.class,
				() -> teaching.learn(owner, new EvolutionTeachingRequest("FLAME_RABBIT", true)));
		assertEquals(2, inventory.list(owner).size());
	}

	@Test
	void acknowledgementAndLocationAreRequiredAndOuterFailureRollsBackWholeReward() {
		long owner = ready();
		var request = new EvolutionTeachingRequest("FLAME_RABBIT", true);
		assertThrows(BusinessException.class,
				() -> teaching.learn(owner, new EvolutionTeachingRequest("FLAME_RABBIT", false)));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			teaching.learn(owner, request);
			throw new IllegalStateException("模拟教学结束前失败");
		}));
		assertTrue(inventory.list(owner).isEmpty());
		assertFalse(facts.codes(owner).contains("EVOLUTION:TUTORIAL"));
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location = 'GNOME_CITY' WHERE id = :id")
				.setParameter("id", characters.lock(owner).id())
				.executeUpdate());
		assertFalse(research.view(owner).available());
		assertThrows(BusinessException.class, () -> teaching.learn(owner, request));
		assertTrue(inventory.list(owner).isEmpty());
	}

}
