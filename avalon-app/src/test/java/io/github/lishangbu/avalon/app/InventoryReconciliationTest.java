package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryReconciliationService;
import io.github.lishangbu.avalon.inventory.persistence.entity.OwnedItem;
import io.github.lishangbu.avalon.inventory.persistence.repository.InventoryReconciliationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** 用旧档形态验证升级守恒；直接 SQL 仅用于构造正式发放接口不会产生的超容量库存。 */
class InventoryReconciliationTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	InventoryReconciliationService reconciliation;

	@Autowired
	InventoryReconciliationRepository queue;

	@Autowired
	EntityManager em;

	private long ready() {
		String name = "repair_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
		registrations.register(name, "Inventory-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	private List<OwnedItem> rows(long character) {
		return em.createQuery("from OwnedItem where characterId = :id order by id", OwnedItem.class)
			.setParameter("id", character)
			.getResultList();
	}

	private void enqueue(long character) {
		em.createNativeQuery("INSERT INTO inventory_reconciliation(character_id) VALUES (:id)")
			.setParameter("id", character)
			.executeUpdate();
	}

	@Test
	void equippedItemHasPriorityAndOverflowPreservesIdentityBindingAndPending() {
		long owner = ready();
		long character = transactions.call(() -> {
			long id = characters.lock(owner).id();
			for (int i = 0; i < 62; i++)
				inventory.grant(owner, "LEGACY:" + i, "STARTER_BODY", 1, true);
			inventory.grant(owner, "TASK", "PET_GUIDE", 1, false);
			em.flush();
			// 最晚获得的一件已穿戴，另一件同时拥有历史待领量。
			em.createNativeQuery("UPDATE owned_item SET quantity = 1, pending_quantity = 0 WHERE character_id = :id")
				.setParameter("id", id)
				.executeUpdate();
			em.createNativeQuery(
					"UPDATE owned_item SET equipped = true WHERE character_id = :id AND source = 'LEGACY:61'")
				.setParameter("id", id)
				.executeUpdate();
			em.createNativeQuery(
					"UPDATE owned_item SET pending_quantity = 2 WHERE character_id = :id AND source = 'LEGACY:60'")
				.setParameter("id", id)
				.executeUpdate();
			enqueue(id);
			return id;
		});
		var before = transactions.call(() -> rows(character).stream().map(OwnedItem::getId).toList());
		reconciliation.repair(owner);
		reconciliation.repair(owner);
		transactions.run(() -> {
			var rows = rows(character);
			assertEquals(before, rows.stream().map(OwnedItem::getId).toList());
			assertEquals(60,
					rows.stream().filter(item -> item.getSlot() != null).mapToInt(OwnedItem::getQuantity).sum());
			assertEquals(65, rows.stream().mapToInt(item -> item.getQuantity() + item.getPendingQuantity()).sum());
			assertTrue(rows.stream().allMatch(OwnedItem::isBound));
			assertEquals(1, rows.stream().filter(OwnedItem::isEquipped).findFirst().orElseThrow().getQuantity());
			assertEquals(1,
					rows.stream()
						.filter(item -> item.getSource().equals("TASK"))
						.findFirst()
						.orElseThrow()
						.getQuantity());
			assertEquals(3,
					rows.stream()
						.filter(item -> item.getSource().equals("LEGACY:60"))
						.findFirst()
						.orElseThrow()
						.getPendingQuantity());
			assertFalse(queue.pending(character));
		});
	}

	@Test
	void crossSourceStacksShareSpaceAndRollbackLeavesQueueRetryable() {
		long owner = ready();
		long character = transactions.call(() -> {
			long id = characters.lock(owner).id();
			for (int i = 0; i < 59; i++)
				inventory.grant(owner, "GEAR:" + i, "STARTER_BODY", 1, true);
			inventory.grant(owner, "POTION:A", "HEALTH_POTION", 80, false);
			inventory.grant(owner, "POTION:B", "HEALTH_POTION", 30, false);
			em.flush();
			em.createNativeQuery(
					"UPDATE owned_item SET quantity = 30, pending_quantity = 5 WHERE character_id = :id AND source = 'POTION:B'")
				.setParameter("id", id)
				.executeUpdate();
			enqueue(id);
			return id;
		});
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			reconciliation.repair(owner);
			throw new IllegalStateException("模拟升级提交失败");
		}));
		transactions.run(() -> {
			assertTrue(queue.pending(character));
			var second = rows(character).stream()
				.filter(item -> item.getSource().equals("POTION:B"))
				.findFirst()
				.orElseThrow();
			assertEquals(30, second.getQuantity());
			assertEquals(5, second.getPendingQuantity());
		});
		reconciliation.repair(owner);
		transactions.run(() -> {
			var potions = rows(character).stream().filter(item -> item.getCode().equals("HEALTH_POTION")).toList();
			assertEquals(99, potions.stream().mapToInt(OwnedItem::getQuantity).sum());
			assertEquals(16, potions.stream().mapToInt(OwnedItem::getPendingQuantity).sum());
			assertEquals(2, potions.size());
			assertFalse(queue.pending(character));
		});
	}

}
