package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.character.application.command.CharacterCommandService;
import io.github.lishangbu.avalon.identity.application.command.RegistrationService;
import io.github.lishangbu.avalon.identity.application.query.AccountQueryService;
import io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService;
import io.github.lishangbu.avalon.inventory.application.command.InventoryCommandService;
import io.github.lishangbu.avalon.platform.BusinessException;

import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static io.github.lishangbu.avalon.inventory.application.command.CultivationMaterialService.Mode.*;

/** 用既有道具隔离验证材料选择与真实库存事务，不把该夹具视为正式进化配方。 */
class CultivationMaterialTest extends SpringIntegrationTest {

	@Autowired
	RegistrationService registrations;

	@Autowired
	AccountQueryService accounts;

	@Autowired
	CharacterCommandService characters;

	@Autowired
	InventoryCommandService inventory;

	@Autowired
	CultivationMaterialService materials;

	private long ready() {
		String name = "mat_" + UUID.randomUUID().toString().substring(0, 8);
		registrations.register(name, "Material-test-1234", "测试");
		long owner = accounts.accountId(name);
		characters.create(owner, name, "WARRIOR");
		transactions.run(() -> {
			inventory.grantStrict(owner, "BOUND_MATERIAL", "BOOK_FIRE", 2, false, true);
			inventory.grantStrict(owner, "FREE_MATERIAL", "BOOK_FIRE", 1, false, false);
		});
		return owner;
	}

	@Test
	void unboundSelectionCannotSilentlyConsumeBoundStock() {
		long owner = ready();
		var one = Map.of("BOOK_FIRE", 1);
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> materials.preview(owner, Map.of("BOOK_FIRE", 2), UNBOUND_ONLY)));
		var unbound = transactions.call(() -> materials.preview(owner, one, UNBOUND_ONLY));
		assertFalse(unbound.bindsTarget());
		var bound = transactions.call(() -> materials.preview(owner, one, BOUND_FIRST));
		assertTrue(bound.bindsTarget());
		assertNotEquals(unbound.quantities(), bound.quantities());
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> materials.consume(owner, one, BOUND_FIRST, unbound)));
		assertFalse(transactions.call(() -> materials.consume(owner, one, UNBOUND_ONLY, unbound)));
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> materials.preview(owner, one, UNBOUND_ONLY)));
		assertTrue(transactions.call(() -> materials.consume(owner, one, BOUND_FIRST, bound)));
	}

	@Test
	void foreignPlanRejectedAndOuterFailureRestoresExactMaterials() {
		long owner = ready();
		long other = ready();
		var recipe = Map.of("BOOK_FIRE", 3);
		var plan = transactions.call(() -> materials.preview(owner, recipe, BOUND_FIRST));
		assertTrue(plan.bindsTarget());
		assertEquals(2, plan.quantities().size());
		assertThrows(BusinessException.class,
				() -> transactions.run(() -> materials.consume(other, recipe, BOUND_FIRST, plan)));
		assertThrows(IllegalStateException.class, () -> transactions.run(() -> {
			materials.consume(owner, recipe, BOUND_FIRST, plan);
			throw new IllegalStateException("模拟培养结果保存失败");
		}));
		assertEquals(plan, transactions.call(() -> materials.preview(owner, recipe, BOUND_FIRST)));
	}

}
