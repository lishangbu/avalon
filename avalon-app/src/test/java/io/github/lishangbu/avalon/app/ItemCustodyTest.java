package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.command.ItemCustodyService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import io.github.lishangbu.avalon.inventory.application.query.ItemCustodyQueryService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** 真实库存托管和接收测试，不代替市场金币结算及请求重试验收。 */
class ItemCustodyTest extends SpringIntegrationTest {

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
	ItemCustodyService custody;

	@Autowired
	ItemCustodyQueryService custodyQueries;

	@Autowired
	EntityManager em;

	private long ready() {
		String name = "cust_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Custody-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		return owner;
	}

	@Test
	void splitCustodyIsUnavailableAndPartialReceiptsConserveQuantity() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> inventory.grantStrict(seller, "DROP", "WIND_FEATHER", 10, false, false));
		var original = items.list(seller).getFirst();
		long asset = transactions.call(() -> custody.deposit(seller, Long.parseLong(original.id()), 7, original));
		assertEquals(3, items.list(seller).getFirst().quantity());
		assertTrue(items.pending(seller).isEmpty());
		assertThrows(BusinessException.class, () -> transactions.run(() -> inventory.take(seller, "WIND_FEATHER", 4)));
		transactions.run(() -> custody.receive(seller, buyer, asset, 2, "MARKET:SALE:1"));
		assertEquals(2, items.list(buyer).getFirst().quantity());
		assertFalse(items.list(buyer).getFirst().bound());
		transactions.run(() -> custody.receive(seller, seller, asset, 5, "MARKET:RETURN:1"));
		assertEquals(8, items.list(seller).stream().mapToInt(item -> item.quantity()).sum());
		transactions.run(() -> inventory.grantStrict(seller, "DROP", "WIND_FEATHER", 10, false, false));
		assertEquals(8, items.list(seller).stream().mapToInt(item -> item.quantity()).sum());
	}

	@Test
	void equipmentKeepsItsIdAndFullBuyerBagRollsBackDelivery() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> {
			inventory.grantStrict(seller, "GEAR", "STARTER_WEAPON", 1, true, false);
			for (int i = 0; i < 60; i++)
				inventory.grantStrict(buyer, "FULL:" + i, "STARTER_WEAPON", 1, true, true);
		});
		var original = items.list(seller).getFirst();
		long asset = transactions.call(() -> custody.deposit(seller, Long.parseLong(original.id()), 1, original));
		assertTrue(items.list(seller).isEmpty());
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> custody.receive(seller, buyer, asset, 1, "MARKET:SALE:2")));
		assertTrue(items.pending(buyer).isEmpty());
		String returned = transactions.call(() -> custody.receive(seller, seller, asset, 1, "MARKET:RETURN:2"));
		assertEquals(original.id(), returned);
		assertEquals(original.stats(), items.list(seller).getFirst().stats());
	}

	/** SQL仅建立强化存档夹具，验证各投影和交易保存字段；不冒充真实强化命令验收。 */
	@Test
	void enhancedEquipmentKeepsOriginalStatsThroughCustodyTransferAndLevelReduction() {
		long seller = ready(), buyer = ready();
		transactions.run(() -> inventory.grantStrict(seller, "ENHANCED", "STARTER_WEAPON", 1, true, false));
		var original = items.list(seller).getFirst();
		long id = Long.parseLong(original.id());
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET enhancement_level=7 WHERE id=:id")
			.setParameter("id", id)
			.executeUpdate());
		var enhanced = items.list(seller).getFirst();
		assertEquals(7, enhanced.enhancementLevel());
		original.stats().forEach((key, value) -> assertEquals(value * 135 / 100, enhanced.stats().get(key)));
		assertEquals(original.sellPrice(), enhanced.sellPrice());
		transactions.run(() -> custody.deposit(seller, id, 1, enhanced));
		assertEquals(7, custodyQueries.detail(id).enhancementLevel());
		assertEquals(enhanced.stats(), custodyQueries.detail(id).stats());
		transactions.run(() -> {
			characters.lock(Math.min(seller, buyer));
			characters.lock(Math.max(seller, buyer));
			custody.receive(seller, buyer, id, 1, "MARKET:ENHANCED");
		});
		assertEquals(enhanced.stats(), items.list(buyer).getFirst().stats());
		assertEquals(7, items.list(buyer).getFirst().enhancementLevel());
		inventory.equip(buyer, id);
		assertEquals(enhanced.stats(), items.loadout(buyer).stats());
		inventory.unequip(buyer, id);
		transactions.run(() -> em.createNativeQuery("UPDATE owned_item SET enhancement_level=0 WHERE id=:id")
			.setParameter("id", id)
			.executeUpdate());
		assertEquals(original.stats(), items.list(buyer).getFirst().stats());
		assertEquals(original.id(), items.list(buyer).getFirst().id());
	}

}
