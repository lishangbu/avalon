package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.creature.application.command.CreatureCommandService;
import io.github.lishangbu.avalon.creature.application.query.CreatureQueryService;
import io.github.lishangbu.avalon.creature.application.port.CreatureResourceCommand;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsStore;
import io.github.lishangbu.avalon.battle.persistence.repository.CombatVitalsRepository;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import static org.junit.jupiter.api.Assertions.*;

/** 生产与战斗读取同一资源存档，检验临界生命、跨角色隔离及外层事务回滚。 */
class ProductionCreatureResourceTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	CreatureCommandService creatures;

	@Autowired
	CreatureQueryService pets;

	@Autowired
	CreatureResourceCommand production;

	@Autowired
	CombatVitalsStore store;

	@Autowired
	CombatVitalsRepository vitals;

	private long owner() {
		String name = "gather_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Gather-test-12345", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> creatures.grant(owner, "GATHER_PET", "SPROUT"));
		return owner;
	}

	@Test
	void fixedCostsPreserveOneHealthAndRefuseFurtherDrain() {
		long owner = owner();
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		transactions.run(() -> store.saveVitals(characters.lock(owner).id(), pet, 6, 2));
		assertTrue(transactions.call(() -> production.spendForProduction(owner, pet, 5, 2)));
		assertEquals(1, vitals.get(pet).getHp());
		assertEquals(0, vitals.get(pet).getMp());
		assertFalse(transactions.call(() -> production.spendForProduction(owner, pet, 5, 2)));
		assertEquals(1, vitals.get(pet).getHp());
		assertEquals(0, vitals.get(pet).getMp());
	}

	@Test
	void insufficientManaAndForeignPetNeverConsumeHealth() {
		long owner = owner(), other = owner();
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		transactions.run(() -> store.saveVitals(characters.lock(owner).id(), pet, 20, 1));
		assertFalse(transactions.call(() -> production.spendForProduction(owner, pet, 5, 2)));
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> production.spendForProduction(other, pet, 5, 0)));
		assertEquals(20, vitals.get(pet).getHp());
		assertEquals(1, vitals.get(pet).getMp());
	}

	@Test
	void concurrentRoundsCannotBothSpendTheLastAvailableResources() throws Exception {
		long owner = owner();
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		transactions.run(() -> store.saveVitals(characters.lock(owner).id(), pet, 6, 2));
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> {
				assertTrue(start.await(10, TimeUnit.SECONDS));
				return transactions.call(() -> production.spendForProduction(owner, pet, 5, 2));
			});
			var second = executor.submit(() -> {
				assertTrue(start.await(10, TimeUnit.SECONDS));
				return transactions.call(() -> production.spendForProduction(owner, pet, 5, 2));
			});
			start.countDown();
			assertNotEquals(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
		}
		assertEquals(1, vitals.get(pet).getHp());
		assertEquals(0, vitals.get(pet).getMp());
	}

	@Test
	void failedProductionRollsBackVitalsAndRequiresAnOuterTransaction() {
		long owner = owner();
		long pet = Long.parseLong(pets.list(owner).getFirst().id());
		transactions.run(() -> store.saveVitals(characters.lock(owner).id(), pet, 20, 10));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			assertTrue(production.spendForProduction(owner, pet, 5, 2));
			throw new IllegalStateException("模拟产出失败");
		}));
		assertEquals(20, vitals.get(pet).getHp());
		assertEquals(10, vitals.get(pet).getMp());
		assertThrows(IllegalTransactionStateException.class, () -> production.spendForProduction(owner, pet, 5, 2));
	}

}
