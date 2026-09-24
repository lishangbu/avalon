package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 用数据库转手夹具隔离检验原发放防重；不把此夹具称为已实现的市场成交。 */
class InventoryProvenanceTest extends SpringIntegrationTest {

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
	EntityManager em;

	@Test
	void academySuppliesCannotBeGrantedToPermanentInventory() {
		long owner = ready();
		for (String code : new String[] { "HEALTH_POTION_II", "MANA_POTION_II" })
			assertThrows(IllegalArgumentException.class,
					() -> transactions.run(() -> inventory.grant(owner, "ACADEMY:" + code, code, 1, false)));
		assertTrue(items.list(owner).isEmpty());
	}

	private long ready() {
		String name = "origin_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Inventory-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	@Test
	void transferringInstanceDoesNotResetOriginalRewardOrConsumeBuyersRewardEligibility() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> inventory.grantStrict(seller, "ORIGINAL_REWARD", "STARTER_WEAPON", 1, true, false));
		String id = items.list(seller).getFirst().id();
		transactions.run(() -> {
			long target = characters.lock(buyer).id();
			// 市场取得来源与原奖励来源分离，买方自己的同名任务奖励不能被交易物品占用。
			em.createNativeQuery("UPDATE owned_item SET character_id=:target, source='MARKET:FIXTURE' WHERE id=:id")
				.setParameter("target", target)
				.setParameter("id", Long.parseLong(id))
				.executeUpdate();
		});
		transactions.run(() -> {
			inventory.grantStrict(seller, "ORIGINAL_REWARD", "STARTER_WEAPON", 1, true, false);
			inventory.grantStrict(buyer, "ORIGINAL_REWARD", "STARTER_WEAPON", 1, true, false);
		});
		assertTrue(items.list(seller).isEmpty());
		assertEquals(2, items.list(buyer).size());
		assertTrue(items.list(buyer).stream().anyMatch(item -> item.id().equals(id)));
		transactions.run(() -> {
			var origin = (Object[]) em.createNativeQuery("""
					SELECT g.character_id, g.source FROM owned_item i
					JOIN inventory_grant g ON i.origin_grant_id = g.id WHERE i.id = :id
					""").setParameter("id", Long.parseLong(id)).getSingleResult();
			assertEquals(characters.lock(seller).id(), ((Number) origin[0]).longValue());
			assertEquals("ORIGINAL_REWARD", origin[1]);
		});
	}

	@Test
	void failedGrantDoesNotLeaveReceiptAndConsumedInventoryStillCannotBeRegranted() {
		long owner = ready();
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			inventory.grant(owner, "ROLLBACK", "WIND_FEATHER", 1, false);
			throw new IllegalStateException("模拟发放失败");
		}));
		assertTrue(items.list(owner).isEmpty());
		transactions.run(() -> inventory.grant(owner, "ROLLBACK", "WIND_FEATHER", 1, false));
		assertEquals(1, items.list(owner).getFirst().quantity());
		transactions.run(() -> inventory.take(owner, "WIND_FEATHER", 1));
		transactions.run(() -> inventory.grant(owner, "ROLLBACK", "WIND_FEATHER", 1, false));
		assertTrue(items.list(owner).isEmpty());
	}

}
