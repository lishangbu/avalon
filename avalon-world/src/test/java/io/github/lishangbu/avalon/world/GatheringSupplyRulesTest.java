package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.GatheringSupplyRules;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatheringSupplyRulesTest {

	@Test
	void insufficientSecondResourceDoesNotPartiallyConsumeFirstPotion() {
		var plan = GatheringSupplyRules.plan(1, 0, 200, 40, 5, 2,
				List.of(new GatheringSupplyRules.Supply(1, 3, 120, 0)));
		assertFalse(plan.sufficient());
		assertTrue(plan.consumed().isEmpty());
		assertEquals(0, plan.health());
	}

	@Test
	void needsOnlyMinimumPotionsAndNeverUsesThemWhenResourcesAlreadySuffice() {
		var supplies = List.of(new GatheringSupplyRules.Supply(1, 3, 120, 0),
				new GatheringSupplyRules.Supply(2, 3, 0, 24));
		var plan = GatheringSupplyRules.plan(1, 0, 200, 40, 25, 10, supplies);
		assertTrue(plan.sufficient());
		assertEquals(1, plan.consumed().get(1L));
		assertEquals(1, plan.consumed().get(2L));
		assertTrue(GatheringSupplyRules.plan(26, 10, 200, 40, 25, 10, supplies).consumed().isEmpty());
		assertFalse(GatheringSupplyRules.plan(1, 0, 20, 40, 25, 10, supplies).sufficient());
	}

}
