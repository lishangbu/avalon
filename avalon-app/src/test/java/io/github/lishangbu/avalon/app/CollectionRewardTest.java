package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.content.creature.CreatureCatalog;
import io.github.lishangbu.avalon.content.creature.CollectionCatalog;
import io.github.lishangbu.avalon.creature.application.command.CollectionCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** 一次性收集奖励与真实获得同事务；并发请求与满包待领均不能重复制造收益。 */
class CollectionRewardTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CharacterQueryService characterQueries;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService creatureQueries;

	@Autowired
	CollectionCommandService collection;

	@Autowired
	CreatureCatalog catalog;

	@Autowired
	CollectionCatalog rewards;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryQueryService items;

	@Autowired
	EntityManager em;

	@Autowired
	io.github.lishangbu.avalon.creature.persistence.repository.CreatureRepository creatureRecords;

	@Autowired
	io.github.lishangbu.avalon.creature.persistence.repository.CreatureDiscoveryRepository discoveries;

	private long ready() {
		String name = "collect_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Collection-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private void capture(long owner, String source, String species, String slot) {
		var pet = catalog.pet(species);
		transactions
			.run(() -> creatures.capture(owner, source, species, pet.level(), slot, pet.ability(), pet.skills()));
	}

	private int quantity(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(item -> item.quantity())
			.sum();
	}

	@Test
	void speciesMilestonesAndHiddenProgressSettleOnceForEachAccount() {
		long owner = ready();
		for (var species : rewards.definition().milestoneSpecies())
			capture(owner, species, species, "A");
		assertEquals(150, characterQueries.find(owner).gold());
		assertEquals(10, quantity(owner, "SEAL_I"));
		assertEquals(2, quantity(owner, "SEAL_II"));
		capture(owner, "DUPLICATE", "STONE_GOLEM", "H");
		capture(owner, "DUPLICATE_2", "STONE_GOLEM", "H");
		collection.claim(owner);
		collection.claim(owner);
		assertEquals(150, characterQueries.find(owner).gold());
		assertEquals(10, quantity(owner, "SEAL_I"));
		assertTrue(creatureQueries.encyclopedia(owner)
			.stream()
			.filter(entry -> rewards.definition().milestoneSpecies().contains(entry.species()))
			.allMatch(entry -> entry.rewarded()));
		long hidden = transactions.call(() -> ((Number) em
			.createNativeQuery(
					"SELECT count(*) FROM collection_receipt WHERE account_id = :owner AND code LIKE 'HIDDEN:%'")
			.setParameter("owner", owner)
			.getSingleResult()).longValue());
		assertEquals(1, hidden);
		long other = ready();
		capture(other, "FIRST", "STONE_GOLEM", "A");
		assertEquals(10, characterQueries.find(other).gold());
	}

	@Test
	void evolvedDiscoveryIsPermanentAndCannotReplaceOpeningSpecies() {
		long owner = ready();
		capture(owner, "BIRD", "GALE_SPARROW", "H");
		long character = Long.parseLong(characterQueries.find(owner).id());
		// 只模拟形态已变更这一图鉴输入，不将直接 SQL 夹具当作实际进化命令或等级资格验证。
		transactions.run(() -> {
			characters.lock(owner);
			em.createNativeQuery("UPDATE owned_creature SET species = 'GALE_FALCON' WHERE character_id = :id")
				.setParameter("id", character)
				.executeUpdate();
			discoveries.remember(creatureRecords.history(character).getFirst());
			collection.settle(owner);
		});
		collection.claim(owner);
		assertEquals(20, characterQueries.find(owner).gold());
		assertEquals(1, quantity(owner, "SEAL_I"));
		assertEquals(1, quantity(owner, "SEAL_II"));
		assertTrue(items.list(owner)
			.stream()
			.filter(item -> item.code().equals("SEAL_II"))
			.allMatch(item -> item.bound()));
		var encyclopedia = creatureQueries.encyclopedia(owner);
		assertTrue(encyclopedia.stream().anyMatch(entry -> entry.species().equals("GALE_SPARROW") && entry.rewarded()));
		assertTrue(encyclopedia.stream().anyMatch(entry -> entry.species().equals("GALE_FALCON") && entry.rewarded()));
		capture(owner, "CAT", "CLAW_CAT", "A");
		capture(owner, "STONE", "STONE_GOLEM", "A");
		assertEquals(40, characterQueries.find(owner).gold(), "三种原形态加进化形态不能领取开篇四种奖励");
		assertThrows(BusinessException.class, () -> capture(owner, "ILLEGAL", "GALE_FALCON", "A"));
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> creatures.grant(owner, "ILLEGAL_GIFT", "GALE_FALCON")));
	}

	@Test
	void fullBagKeepsRewardPendingAndCaptureRollbackKeepsNoDiscoveryOrMoney() {
		long owner = ready();
		transactions.run(() -> {
			for (int i = 0; i < 60; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_WEAPON", 1, true);
		});
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			creatures.grant(owner, "ROLLBACK", "SPROUT");
			throw new IllegalStateException("模拟外层任务提交失败");
		}));
		assertEquals(0, characterQueries.find(owner).gold());
		assertTrue(creatureQueries.list(owner).isEmpty());
		assertTrue(items.pending(owner).isEmpty());
		transactions.run(() -> creatures.grant(owner, "ROLLBACK", "SPROUT"));
		assertEquals(10, characterQueries.find(owner).gold());
		assertEquals(0, quantity(owner, "SEAL_I"));
		assertEquals(1, items.pending(owner).getFirst().quantity());
		collection.claim(owner);
		assertEquals(1, items.pending(owner).getFirst().quantity());
		assertEquals(10, characterQueries.find(owner).gold());
	}

	@Test
	void collectionClaimsAreIdempotentConcurrentlyAndBlockedDuringBattle() throws Exception {
		long owner = ready();
		transactions.run(() -> creatures.grant(owner, "COLLECTION_TEST", "SPROUT"));
		transactions.run(() -> characters.setBattle(owner, 123L, false));
		assertThrows(BusinessException.class, () -> collection.claim(owner));
		transactions.run(() -> characters.setBattle(owner, null, false));
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				start.await();
				collection.claim(owner);
				return true;
			});
			var second = executor.submit(() -> {
				start.await();
				collection.claim(owner);
				return true;
			});
			start.countDown();
			assertTrue(first.get(15, TimeUnit.SECONDS));
			assertTrue(second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(10, characterQueries.find(owner).gold());
		assertEquals(1, quantity(owner, "SEAL_I"));
	}

}
