package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.character.application.query.CharacterQueryService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.BookRecyclingCommandService;
import io.github.lishangbu.avalon.inventory.application.command.CraftingCommandService;
import io.github.lishangbu.avalon.inventory.application.command.EquipmentCommerceService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryClaimService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.inventory.application.model.InventoryItemView;
import io.github.lishangbu.avalon.inventory.application.query.BookRecyclingQueryService;
import io.github.lishangbu.avalon.inventory.application.query.InventoryQueryService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 接近堆叠上限的长期库存夹具只负责起点，不冒充七日自然游玩。 后续领取、腾位、合成和出售均调用正式应用服务，逐步核对可用与待领总量。
 */
class ResourceCirculationTest extends SpringIntegrationTest {

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
	InventoryQueryService items;

	@Autowired
	InventoryClaimService claims;

	@Autowired
	CraftingCommandService crafting;

	@Autowired
	EquipmentCommerceService equipment;

	@Autowired
	BookRecyclingQueryService recyclingQueries;

	@Autowired
	BookRecyclingCommandService recycling;

	@Autowired
	EntityManager em;

	@Test
	void fullBagRewardsCanCirculateWithoutCreatingOrLosingResources() {
		String name = "cycle_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Circulation-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			characters.changeGold(owner, 100);
			for (int index = 0; index < 58; index++)
				inventory.grant(owner, "OLD:" + index, "STARTER_BODY", 1, true);
			inventory.grantWithBinding(owner, "OLD:FRAGMENT", "FRAGMENT_WATER", 98, false, false);
			inventory.grantWithBinding(owner, "OLD:BOOK", "BOOK_WATER", 99, false, false);
		});
		location(owner, "SUPPLY_SHOP");
		reward(owner);
		reward(owner);
		assertEquals(99, available(owner, "FRAGMENT_WATER"));
		assertEquals(99, available(owner, "BOOK_WATER"));
		assertEquals(13, items.pending(owner).stream().mapToInt(item -> item.quantity()).sum());
		var fragment = items.pending(owner).stream().filter(item -> item.quantity() == 9).findFirst().orElseThrow();
		var book = items.pending(owner).stream().filter(item -> item.quantity() == 3).findFirst().orElseThrow();
		var gear = items.pending(owner).stream().filter(item -> item.quantity() == 1).findFirst().orElseThrow();
		var claimFragments = new InventoryClaimService.Request(UUID.randomUUID().toString(), 9);
		assertThrows(BusinessException.class, () -> claims.claim(owner, Long.parseLong(fragment.id()), claimFragments));
		assertEquals(100, characterQueries.find(owner).gold());

		destroyOld(owner);
		claims.claim(owner, Long.parseLong(fragment.id()), claimFragments);
		claims.claim(owner, Long.parseLong(fragment.id()), claimFragments);
		assertEquals(108, available(owner, "FRAGMENT_WATER"));
		var craft = new CraftingCommandService.Request(UUID.randomUUID().toString(), "WATER_BOOK",
				Map.of(fragment.id(), 5));
		var beforeItems = items.list(owner);
		assertThrows(BusinessException.class, () -> crafting.craft(owner, craft));
		assertEquals(beforeItems, items.list(owner), "碎片与书均跨过 99 堆叠边界，空间不足不能先消耗材料");
		assertEquals(100, characterQueries.find(owner).gold());

		destroyOld(owner);
		var crafted = crafting.craft(owner, craft);
		assertFalse(crafted.bound());
		assertEquals(crafted, crafting.craft(owner, craft));
		assertEquals(103, available(owner, "FRAGMENT_WATER"));
		assertEquals(100, available(owner, "BOOK_WATER"));
		assertEquals(90, characterQueries.find(owner).gold());
		var offer = recyclingQueries.view(owner)
			.offers()
			.stream()
			.filter(value -> value.code().equals("BOOK_WATER"))
			.findFirst()
			.orElseThrow();
		assertEquals(99, offer.maximum(), "三本待领不计入保留数量");
		var sellBooks = new BookRecyclingCommandService.Request(UUID.randomUUID().toString(), 99, offer);
		recycling.sell(owner, sellBooks);
		recycling.sell(owner, sellBooks);
		assertEquals(1, available(owner, "BOOK_WATER"));
		assertEquals(684, characterQueries.find(owner).gold());
		claim(owner, book.id(), 3);
		claim(owner, gear.id(), 1);
		assertTrue(items.pending(owner).isEmpty());
		var receivedGear = items.list(owner)
			.stream()
			.filter(item -> item.id().equals(gear.id()))
			.findFirst()
			.orElseThrow();
		assertTrue(receivedGear.bound());
		assertEquals(24, receivedGear.sellPrice(), "待领转入包不能丢失来源卖价");
		location(owner, "WEAPON_SHOP");
		var sellGear = new EquipmentCommerceService.Request(UUID.randomUUID().toString(), "SELL", null, receivedGear);
		equipment.execute(owner, sellGear);
		equipment.execute(owner, sellGear);
		// 已出售或合成的奖励来源不能靠重新发放再次获得。
		reward(owner);
		assertEquals(708, characterQueries.find(owner).gold());
		assertEquals(56, available(owner, "STARTER_BODY"));
		assertEquals(103, available(owner, "FRAGMENT_WATER"));
		assertEquals(4, available(owner, "BOOK_WATER"));
		assertEquals(0, available(owner, "CRYSTAL_SWORD"));
		assertTrue(items.pending(owner).isEmpty());
	}

	void reward(long owner) {
		transactions.run(() -> {
			inventory.grantWithBinding(owner, "REWARD:FRAGMENT", "FRAGMENT_WATER", 10, false, false);
			inventory.grantWithBinding(owner, "REWARD:BOOK", "BOOK_WATER", 3, false, false);
			inventory.grantRewardEquipment(owner, "REWARD:GEAR", "CRYSTAL_SWORD", 24);
		});
	}

	void destroyOld(long owner) {
		InventoryItemView old = items.list(owner)
			.stream()
			.filter(item -> item.code().equals("STARTER_BODY"))
			.findFirst()
			.orElseThrow();
		var request = new EquipmentCommerceService.Request(UUID.randomUUID().toString(), "DESTROY", null, old);
		equipment.execute(owner, request);
		equipment.execute(owner, request);
	}

	void claim(long owner, String id, int quantity) {
		var request = new InventoryClaimService.Request(UUID.randomUUID().toString(), quantity);
		claims.claim(owner, Long.parseLong(id), request);
		claims.claim(owner, Long.parseLong(id), request);
	}

	int available(long owner, String code) {
		return items.list(owner)
			.stream()
			.filter(item -> item.code().equals(code))
			.mapToInt(InventoryItemView::quantity)
			.sum();
	}

	void location(long owner, String location) {
		transactions
			.run(() -> em.createNativeQuery("UPDATE player_character SET location=:location WHERE account_id=:owner")
				.setParameter("location", location)
				.setParameter("owner", owner)
				.executeUpdate());
	}

}
