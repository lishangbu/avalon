package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.world.domain.GatheringRules;
import io.github.lishangbu.avalon.world.domain.GatheringRulesCost;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GatheringRulesTest {

	private final Instant start = Instant.parse("2026-09-22T00:00:00Z");

	@Test
	void disconnectAndBackgroundKeepOnlyConfirmedProgress() {
		var delayed = GatheringRules.heartbeat(4000, 10000, start, start.plusSeconds(600), true);
		assertEquals(4000, delayed.confirmedMillis());
		var resumed = GatheringRules.heartbeat(delayed.confirmedMillis(), 10000, delayed.lastSeen(),
				start.plusSeconds(601), true);
		assertEquals(5000, resumed.confirmedMillis());
		var background = GatheringRules.heartbeat(5000, 10000, resumed.lastSeen(), start.plusSeconds(602), false);
		assertNull(background.lastSeen());
		assertEquals(5000,
				GatheringRules
					.heartbeat(background.confirmedMillis(), 10000, background.lastSeen(), start.plusSeconds(900), true)
					.confirmedMillis());
	}

	@Test
	void rapidOrDelayedHeartbeatsCannotAccelerateOrCompleteMultipleRounds() {
		assertEquals(0, GatheringRules.heartbeat(0, 6000, start, start, true).confirmedMillis());
		assertEquals(1000, GatheringRules.heartbeat(0, 6000, start, start.plusMillis(2400), true).confirmedMillis());
		var completion = GatheringRules.heartbeat(5900, 6000, start, start.plusSeconds(1), true);
		assertTrue(completion.complete());
		assertEquals(6000, completion.confirmedMillis());
		assertEquals(start, GatheringRules.heartbeat(3000, 6000, start, start.minusSeconds(1), true).lastSeen());
	}

	@Test
	void speedIsBoundedAndCostsDependOnlyOnResourceTier() {
		assertEquals(10000, GatheringRules.roundMillisForSkill(1));
		assertEquals(8000, GatheringRules.roundMillisForSkill(5));
		assertEquals(6000, GatheringRules.roundMillisForSkill(10));
		assertEquals(new GatheringRulesCost(25, 10), GatheringRules.cost(4));
		assertEquals(new GatheringRulesCost(5, 2), GatheringRules.cost(0));
		assertThrows(IllegalArgumentException.class, () -> GatheringRules.cost(5));
		assertThrows(IllegalArgumentException.class, () -> GatheringRules.roundMillisForSkill(0));
		assertThrows(IllegalArgumentException.class, () -> GatheringRules.roundMillisForSkill(11));
	}

}
